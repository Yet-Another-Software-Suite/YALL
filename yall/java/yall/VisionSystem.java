package yall;

import static org.wpilib.units.Units.Seconds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.wpilib.math.estimator.PoseEstimator;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.system.Timer;
import org.wpilib.units.measure.AngularVelocity;
import org.wpilib.units.measure.Time;
import org.wpilib.util.Alert;
import yall.results.VisionPoseEstimate;
import yall.sim.VisionSim;

/**
 * Several {@link VisionCamera}s working together, from any mix of vendors.
 *
 * <pre>{@code
 * VisionSystem vision =
 *     new VisionSystem(frontLimelight, backPhoton, sideSynapse)
 *         .withDisconnectDebounce(VisionSystem.DEFAULT_DISCONNECT_DEBOUNCE)
 *         .withDisconnectAlerts(true)
 *         .onCameraDisconnected(cam -> sideSynapse.setPipelineIndex(APRILTAG_PIPELINE));
 *
 * public void periodic() {
 *   vision.setRobotOrientation(poseEstimator.getEstimatedPosition().getRotation(), gyro.getYawRate());
 *   vision.update();
 *   vision.addVisionMeasurements(poseEstimator);
 * }
 *
 * public void simulationPeriodic() {
 *   vision.updateSimulation(drivetrainSim.getPose());
 * }
 * }</pre>
 *
 * <p>Add every estimate to your drivetrain's pose estimator with {@link
 * #addVisionMeasurements(PoseEstimator)}, or with {@link #forEachEstimatedPose(Consumer)} for a
 * drivetrain with its own {@code addVisionMeasurement} method. Each estimate goes in on its own, with
 * the time its image was taken and its own standard deviations, so the pose estimator applies it
 * against the odometry from that moment and trusts each camera as much as it should. Don't combine
 * the cameras' estimates first: that loses their timestamps and standard deviations.
 */
public class VisionSystem implements AutoCloseable {

  /** Default debounce duration before a disconnected camera triggers alerts or failover callbacks. */
  public static final Time DEFAULT_DISCONNECT_DEBOUNCE = Seconds.of(1.5);

  /** Alert group name published to SmartDashboard / NetworkTables when disconnect alerts are on. */
  private static final String ALERT_GROUP = "YALL";

  private final List<VisionCamera> cameras;
  private final boolean[] reportedDisconnected;
  private final double[] disconnectedSinceSeconds;
  private final Alert[] disconnectAlerts;
  private double disconnectDebounceSeconds = 0.0;
  private boolean disconnectAlertsEnabled = false;
  private Consumer<VisionCamera> onDisconnectedCallback = null;
  private Consumer<VisionCamera> onReconnectedCallback = null;
  private List<VisionPoseEstimate> estimates = List.of();

  /**
   * Creates a vision system.
   *
   * @param cameras The cameras.
   */
  public VisionSystem(VisionCamera... cameras) {
    this.cameras = List.of(cameras);
    int n = this.cameras.size();
    this.reportedDisconnected = new boolean[n];
    this.disconnectedSinceSeconds = new double[n];
    this.disconnectAlerts = new Alert[n];
    for (int i = 0; i < n; i++) {
      this.disconnectedSinceSeconds[i] = Double.NaN;
    }
  }

  /**
   * Sets how long a camera must remain disconnected before {@link #onCameraDisconnected(Consumer)}
   * and {@link #withDisconnectAlerts(boolean)} activate, preventing brief NetworkTables hiccups
   * from flapping failover logic.
   *
   * @param debounce Non-negative debounce duration (e.g. {@link #DEFAULT_DISCONNECT_DEBOUNCE}).
   *     {@code null} disables debouncing (immediate reporting).
   * @return This vision system, for chaining.
   */
  public VisionSystem withDisconnectDebounce(Time debounce) {
    if (debounce == null) {
      this.disconnectDebounceSeconds = 0.0;
      return this;
    }
    double seconds = debounce.in(Seconds);
    if (!Double.isFinite(seconds) || seconds < 0.0) {
      throw new IllegalArgumentException("debounce must be finite and non-negative");
    }
    this.disconnectDebounceSeconds = seconds;
    return this;
  }

  /**
   * Enables or disables automatic WPILib {@link Alert}s when a camera disconnects.
   *
   * @param enabled {@code true} to publish a high-priority {@link Alert} per disconnected camera.
   * @return This vision system, for chaining.
   */
  public VisionSystem withDisconnectAlerts(boolean enabled) {
    this.disconnectAlertsEnabled = enabled;
    if (enabled) {
      for (int i = 0; i < cameras.size(); i++) {
        if (disconnectAlerts[i] == null) {
          disconnectAlerts[i] =
              new Alert(
                  ALERT_GROUP,
                  "CameraDisconnected/" + cameras.get(i).getName(),
                  "Vision camera '" + cameras.get(i).getName() + "' disconnected",
                  Alert.Level.HIGH);
        }
        disconnectAlerts[i].set(reportedDisconnected[i]);
      }
    } else {
      for (Alert alert : disconnectAlerts) {
        if (alert != null) {
          alert.set(false);
        }
      }
    }
    return this;
  }

  /**
   * Registers an edge-triggered callback invoked once when a camera transitions into the
   * disconnected state (after any configured {@link #withDisconnectDebounce(Time)}). Useful for
   * active multi-camera failover, such as switching a surviving game-piece camera to an AprilTag
   * pipeline when the primary localization camera drops mid-match.
   *
   * @param callback Callback receiving the disconnected camera, or {@code null} to clear.
   * @return This vision system, for chaining.
   */
  public VisionSystem onCameraDisconnected(Consumer<VisionCamera> callback) {
    this.onDisconnectedCallback = callback;
    return this;
  }

  /**
   * Registers an edge-triggered callback invoked once when a previously disconnected camera
   * reconnects.
   *
   * @param callback Callback receiving the reconnected camera, or {@code null} to clear.
   * @return This vision system, for chaining.
   */
  public VisionSystem onCameraReconnected(Consumer<VisionCamera> callback) {
    this.onReconnectedCallback = callback;
    return this;
  }

  /** Updates every camera and evaluates connection health. Call once per loop. */
  public void update() {
    update(Timer.getMonotonicTimestamp());
  }

  /**
   * Updates every camera and evaluates connection health using an explicit monotonic timestamp
   * (useful for deterministic unit tests and log replay).
   *
   * @param nowSeconds Current monotonic timestamp in seconds.
   */
  public void update(double nowSeconds) {
    var all = new ArrayList<VisionPoseEstimate>();
    for (int i = 0; i < cameras.size(); i++) {
      VisionCamera camera = cameras.get(i);
      camera.update();
      updateCameraHealth(i, camera, nowSeconds);
      all.addAll(camera.getEstimatedPoses());
    }
    all.sort(Comparator.comparingDouble(VisionPoseEstimate::timestampSeconds));
    estimates = List.copyOf(all);
  }

  private void updateCameraHealth(int index, VisionCamera camera, double nowSeconds) {
    if (camera.isConnected()) {
      disconnectedSinceSeconds[index] = Double.NaN;
      if (reportedDisconnected[index]) {
        reportedDisconnected[index] = false;
        if (disconnectAlertsEnabled && disconnectAlerts[index] != null) {
          disconnectAlerts[index].set(false);
        }
        if (onReconnectedCallback != null) {
          onReconnectedCallback.accept(camera);
        }
      }
      return;
    }

    if (Double.isNaN(disconnectedSinceSeconds[index])) {
      disconnectedSinceSeconds[index] = nowSeconds;
    }
    double elapsed = nowSeconds - disconnectedSinceSeconds[index];
    if (!reportedDisconnected[index] && elapsed >= disconnectDebounceSeconds) {
      reportedDisconnected[index] = true;
      if (disconnectAlertsEnabled && disconnectAlerts[index] != null) {
        disconnectAlerts[index].set(true);
      }
      if (onDisconnectedCallback != null) {
        onDisconnectedCallback.accept(camera);
      }
    }
  }

  /**
   * Updates the simulated worlds the cameras are in. Call once per loop from {@code
   * simulationPeriodic}.
   *
   * @param robotPose The robot's true pose.
   */
  public void updateSimulation(Pose2d robotPose) {
    var sims = new LinkedHashSet<VisionSim>();
    cameras.forEach(c -> c.getVisionSim().ifPresent(sims::add));
    sims.forEach(sim -> sim.update(robotPose));
  }

  /**
   * Gives every camera the robot's orientation on the field: the yaw from your pose estimator, the
   * roll and pitch from the gyro.
   *
   * @param orientation Robot orientation on the field.
   * @param yawRate Robot yaw rate.
   * @see VisionCamera#setRobotOrientation(Rotation3d, AngularVelocity)
   */
  public void setRobotOrientation(Rotation3d orientation, AngularVelocity yawRate) {
    cameras.forEach(c -> c.setRobotOrientation(orientation, yawRate));
  }

  /**
   * Gives every camera the robot's heading on the field and its yaw rate.
   *
   * @param heading Robot heading on the field, your pose estimator's rotation.
   * @param yawRate Robot yaw rate.
   * @see VisionCamera#setRobotOrientation(Rotation2d, AngularVelocity)
   */
  public void setRobotOrientation(Rotation2d heading, AngularVelocity yawRate) {
    cameras.forEach(c -> c.setRobotOrientation(heading, yawRate));
  }

  /**
   * Gives every camera the robot's heading on the field, without a yaw rate.
   *
   * @param heading Robot heading on the field, your pose estimator's rotation.
   * @see VisionCamera#setRobotOrientation(Rotation2d)
   */
  public void setRobotOrientation(Rotation2d heading) {
    cameras.forEach(c -> c.setRobotOrientation(heading));
  }

  /**
   * The cameras in this system.
   *
   * @return The cameras.
   */
  public List<VisionCamera> getCameras() {
    return cameras;
  }

  /**
   * Looks up a camera in this system by its configured name.
   *
   * @param name Camera name.
   * @return Matching camera, or empty if not found.
   */
  public Optional<VisionCamera> getCamera(String name) {
    Objects.requireNonNull(name, "name must not be null");
    for (VisionCamera camera : cameras) {
      if (camera.getName().equals(name)) {
        return Optional.of(camera);
      }
    }
    return Optional.empty();
  }

  /**
   * Switches the pipeline on a named camera in this system.
   *
   * @param cameraName Name of the camera to switch.
   * @param pipelineIndex Target pipeline index.
   * @return {@code true} if the camera was found and switched, {@code false} otherwise.
   */
  public boolean setPipelineIndex(String cameraName, int pipelineIndex) {
    Optional<VisionCamera> match = getCamera(cameraName);
    match.ifPresent(cam -> cam.setPipelineIndex(pipelineIndex));
    return match.isPresent();
  }

  /**
   * Cameras that are currently connected.
   *
   * @return Connected cameras in registration order.
   */
  public List<VisionCamera> getConnectedCameras() {
    var connected = new ArrayList<VisionCamera>(cameras.size());
    for (VisionCamera camera : cameras) {
      if (camera.isConnected()) {
        connected.add(camera);
      }
    }
    return List.copyOf(connected);
  }

  /**
   * Cameras that are currently disconnected.
   *
   * @return Disconnected cameras in registration order.
   */
  public List<VisionCamera> getDisconnectedCameras() {
    var disconnected = new ArrayList<VisionCamera>(cameras.size());
    for (VisionCamera camera : cameras) {
      if (!camera.isConnected()) {
        disconnected.add(camera);
      }
    }
    return List.copyOf(disconnected);
  }

  /**
   * Number of cameras currently reporting connected.
   *
   * @return Count of connected cameras.
   */
  public int getConnectedCameraCount() {
    int count = 0;
    for (VisionCamera camera : cameras) {
      if (camera.isConnected()) {
        count++;
      }
    }
    return count;
  }

  /**
   * Whether at least one camera in this system is currently connected.
   *
   * @return {@code true} if one or more cameras are connected.
   */
  public boolean hasConnectedCamera() {
    return getConnectedCameraCount() > 0;
  }

  /**
   * Every accepted estimate from every camera in the last {@link #update()}.
   *
   * @return The estimates, oldest first.
   */
  public List<VisionPoseEstimate> getEstimatedPoses() {
    return estimates;
  }

  /**
   * Passes every accepted estimate from the last {@link #update()} to a consumer, oldest first. Use
   * it for drivetrains with their own vision measurement method, passing each estimate's {@link
   * VisionPoseEstimate#pose2d() pose}, {@link VisionPoseEstimate#timestampSeconds() timestamp} and
   * {@link VisionPoseEstimate#stdDevs() standard deviations}.
   *
   * @param consumer Receives each estimate.
   */
  public void forEachEstimatedPose(Consumer<VisionPoseEstimate> consumer) {
    estimates.forEach(consumer);
  }

  /**
   * Adds every accepted estimate from the last {@link #update()} to a WPILib pose estimator, oldest
   * first, with its timestamp and standard deviations. Call it once per loop, after {@link
   * #update()}.
   *
   * @param poseEstimator The drivetrain's pose estimator.
   */
  public void addVisionMeasurements(PoseEstimator<?> poseEstimator) {
    forEachEstimatedPose(
        e -> poseEstimator.addVisionMeasurement(e.pose2d(), e.timestampSeconds(), e.stdDevs()));
  }

  /** Closes every camera and any active disconnect alerts. */
  @Override
  public void close() {
    for (Alert alert : disconnectAlerts) {
      if (alert != null) {
        alert.close();
      }
    }
    cameras.forEach(VisionCamera::close);
  }
}
