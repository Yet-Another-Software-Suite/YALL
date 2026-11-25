package yall;

import static org.wpilib.units.Units.Seconds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.wpilib.math.estimator.PoseEstimator;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Translation3d;
import org.wpilib.math.linalg.VecBuilder;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.StructPublisher;
import org.wpilib.units.measure.AngularVelocity;
import org.wpilib.units.measure.Time;
import yall.results.VisionPoseEstimate;
import yall.sim.VisionSim;

/**
 * Several {@link VisionCamera}s working together, from any mix of vendors.
 *
 * <pre>{@code
 * VisionSystem vision = new VisionSystem(frontLimelight, backPhoton, sideSynapse);
 *
 * public void periodic() {
 *   vision.setRobotOrientation(gyro.getRotation3d(), gyro.getYawRate());
 *   vision.update();
 *   vision.addVisionMeasurements(poseEstimator);
 * }
 *
 * public void simulationPeriodic() {
 *   vision.updateSimulation(drivetrainSim.getPose());
 * }
 * }</pre>
 *
 * <p>{@link #addVisionMeasurements(PoseEstimator)} is the most accurate way to use several cameras:
 * the pose estimator weighs each estimate at the moment its image was taken. {@link #getFusedPose()}
 * combines the cameras' newest estimates into one pose, for code that wants a single answer.
 */
public class VisionSystem implements AutoCloseable {

  private final List<VisionCamera> cameras;
  private double maxFusionTimeSkewSeconds = 0.1;
  private List<VisionPoseEstimate> estimates = List.of();
  private final StructPublisher<Pose3d> fusedPublisher;

  /**
   * Creates a vision system.
   *
   * @param cameras The cameras.
   */
  public VisionSystem(VisionCamera... cameras) {
    this.cameras = List.of(cameras);
    fusedPublisher =
        NetworkTableInstance.getDefault()
            .getTable("YALL")
            .getStructTopic("fusedPose", Pose3d.struct)
            .publish();
  }

  /**
   * Sets how far apart in time the cameras' newest estimates can be and still be fused. Older ones
   * are left out of {@link #getFusedPose()}. Defaults to 0.1 s.
   *
   * @param maxSkew Largest time between the newest and oldest fused estimate.
   * @return This system, for chaining.
   */
  public VisionSystem withMaxFusionTimeSkew(Time maxSkew) {
    maxFusionTimeSkewSeconds = maxSkew.in(Seconds);
    return this;
  }

  /** Updates every camera. Call once per loop. */
  public void update() {
    var all = new ArrayList<VisionPoseEstimate>();
    for (VisionCamera camera : cameras) {
      camera.update();
      all.addAll(camera.getEstimatedPoses());
    }
    all.sort(Comparator.comparingDouble(VisionPoseEstimate::timestampSeconds));
    estimates = List.copyOf(all);
    getFusedPose().ifPresent(e -> fusedPublisher.set(e.pose()));
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
   * Gives every camera the robot's orientation from the gyro.
   *
   * @param orientation Robot orientation on the field.
   * @param yawRate Robot yaw rate.
   * @see VisionCamera#setRobotOrientation(Rotation3d, AngularVelocity)
   */
  public void setRobotOrientation(Rotation3d orientation, AngularVelocity yawRate) {
    cameras.forEach(c -> c.setRobotOrientation(orientation, yawRate));
  }

  /**
   * Gives every camera the robot's heading from the gyro.
   *
   * @param heading Robot heading on the field.
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
   * Every accepted estimate from every camera in the last {@link #update()}.
   *
   * @return The estimates, oldest first.
   */
  public List<VisionPoseEstimate> getEstimatedPoses() {
    return estimates;
  }

  /**
   * Passes every accepted estimate from the last {@link #update()} to a consumer, oldest first. Use
   * it for drivetrains with their own vision measurement method.
   *
   * @param consumer Receives each estimate.
   */
  public void forEachEstimatedPose(Consumer<VisionPoseEstimate> consumer) {
    estimates.forEach(consumer);
  }

  /**
   * Adds every accepted estimate from the last {@link #update()} to a WPILib pose estimator, oldest
   * first, with its timestamp and standard deviations.
   *
   * @param poseEstimator The drivetrain's pose estimator.
   */
  public void addVisionMeasurements(PoseEstimator<?> poseEstimator) {
    forEachEstimatedPose(
        e -> poseEstimator.addVisionMeasurement(e.pose2d(), e.timestampSeconds(), e.stdDevs()));
  }

  /**
   * Combines each camera's newest accepted estimate from the last {@link #update()} into one pose.
   * Estimates more than the {@link #withMaxFusionTimeSkew(Time) time skew} older than the newest are
   * left out. Each axis is a weighted average, weighted by the inverse square of the estimate's
   * standard deviation, so more trusted cameras count for more.
   *
   * @return The fused estimate, named "fused", or empty if no camera has one.
   */
  public Optional<VisionPoseEstimate> getFusedPose() {
    var newest = new ArrayList<VisionPoseEstimate>();
    for (VisionCamera camera : cameras) {
      camera.getEstimatedPose().ifPresent(newest::add);
    }
    if (newest.isEmpty()) {
      return Optional.empty();
    }
    double latest =
        newest.stream().mapToDouble(VisionPoseEstimate::timestampSeconds).max().getAsDouble();
    newest.removeIf(e -> latest - e.timestampSeconds() > maxFusionTimeSkewSeconds);
    return Optional.of(fuse(newest));
  }

  /**
   * Combines estimates into one, weighting each axis by the inverse square of its standard
   * deviation.
   *
   * @param estimates The estimates. Must not be empty.
   * @return The fused estimate.
   */
  public static VisionPoseEstimate fuse(List<VisionPoseEstimate> estimates) {
    if (estimates.size() == 1) {
      return estimates.get(0);
    }
    double wx = 0, wy = 0, wt = 0;
    double x = 0, y = 0, z = 0, sin = 0, cos = 0, roll = 0, pitch = 0, time = 0, distance = 0;
    var tagIds = new LinkedHashSet<Integer>();
    boolean headingAssisted = true;
    for (VisionPoseEstimate e : estimates) {
      double ix = 1 / square(e.stdDevs().get(0, 0));
      double iy = 1 / square(e.stdDevs().get(1, 0));
      double it = 1 / square(e.stdDevs().get(2, 0));
      double heading = e.pose().getRotation().getZ();
      x += ix * e.pose().getX();
      y += iy * e.pose().getY();
      sin += it * Math.sin(heading);
      cos += it * Math.cos(heading);
      z += e.pose().getZ() / estimates.size();
      roll += e.pose().getRotation().getX() / estimates.size();
      pitch += e.pose().getRotation().getY() / estimates.size();
      time += ix * e.timestampSeconds();
      distance += ix * e.averageTagDistanceMeters();
      wx += ix;
      wy += iy;
      wt += it;
      tagIds.addAll(e.tagIds());
      headingAssisted &= e.headingAssisted();
    }
    var pose =
        new Pose3d(
            new Translation3d(x / wx, y / wy, z),
            new Rotation3d(roll, pitch, Math.atan2(sin, cos)));
    return new VisionPoseEstimate(
        "fused",
        pose,
        time / wx,
        List.copyOf(tagIds),
        distance / wx,
        0,
        headingAssisted,
        VecBuilder.fill(1 / Math.sqrt(wx), 1 / Math.sqrt(wy), 1 / Math.sqrt(wt)));
  }

  private static double square(double value) {
    return value * value;
  }

  /** Closes every camera and the fused pose telemetry. */
  @Override
  public void close() {
    cameras.forEach(VisionCamera::close);
    fusedPublisher.close();
  }
}
