package yall.sim;

import static org.wpilib.units.Units.Meters;
import static org.wpilib.units.Units.Seconds;

import java.util.Optional;
import java.util.SplittableRandom;
import org.photonvision.PhotonCamera;
import org.photonvision.simulation.PhotonCameraSim;
import org.photonvision.simulation.SimCameraProperties;
import org.photonvision.simulation.VisionSystemSim;
import org.photonvision.simulation.VisionTargetSim;
import org.wpilib.fields.Field;
import org.wpilib.fields.Fields;
import org.wpilib.framework.RobotBase;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.geometry.Translation3d;
import org.wpilib.smartdashboard.Field2d;
import org.wpilib.units.measure.Distance;
import org.wpilib.units.measure.Time;
import yall.VisionCameraConfig;

/**
 * The simulated world every simulated {@link yall.VisionCamera} sees, whatever its vendor. It is
 * PhotonVision's {@link VisionSystemSim}: each camera projects the field's AprilTags (and any extra
 * targets) through a simulated lens, with noise and latency.
 *
 * <p>Cameras join {@link #getDefault()} unless their config names another world. Update the world
 * once per loop with the robot's true pose:
 *
 * <pre>{@code
 * public void simulationPeriodic() {
 *   VisionSim.getDefault().update(drivetrainSim.getPose());
 * }
 * }</pre>
 */
public class VisionSim {

  private static VisionSim defaultSim;

  private final VisionSystemSim visionSystemSim;
  private final Field field;
  private double blackoutUntilSeconds = Double.NEGATIVE_INFINITY;
  private double dropoutProbability = 0.0;
  private double fieldTagJitterStdDevMeters = 0.0;
  private SplittableRandom faultRng = new SplittableRandom(2027L);

  /**
   * Creates a simulated world.
   *
   * @param name Name of the world, used for its NetworkTables debug field.
   * @param field Field whose AprilTags the cameras see.
   */
  public VisionSim(String name, Field field) {
    this.field = field;
    visionSystemSim = new VisionSystemSim(name);
    visionSystemSim.addAprilTags(field);
  }

  /**
   * The world cameras join when their config doesn't name one. It holds the AprilTags of WPILib's
   * {@link Fields#DEFAULT_FIELD}.
   *
   * @return The default world.
   */
  public static synchronized VisionSim getDefault() {
    if (defaultSim == null) {
      defaultSim = new VisionSim("YALL", Field.loadField(Fields.DEFAULT_FIELD));
    }
    return defaultSim;
  }

  /**
   * Replaces the default world, for example to use another field. Call it before creating cameras.
   *
   * @param sim The new default world.
   */
  public static synchronized void setDefault(VisionSim sim) {
    defaultSim = sim;
  }

  /**
   * Makes the PhotonVision simulation of a camera and adds it to the camera's world (the config's,
   * or {@link #getDefault()}), if the robot program runs in simulation and the config allows it.
   * Camera wrappers call this; robot code doesn't need to.
   *
   * @param name Name of the camera.
   * @param config The camera's config.
   * @param defaultModel Camera to simulate when the config sets no camera properties.
   * @param camera PhotonVision camera the simulation publishes to, or null to make a private one
   *     named {@code YALL-<name>}.
   * @return The simulated camera, or null when not simulating.
   */
  public static PhotonCameraSim createCameraSim(
      String name, VisionCameraConfig config, CameraModel defaultModel, PhotonCamera camera) {
    if (!RobotBase.isSimulation() || !config.isSimulationEnabled()) {
      return null;
    }
    var photonCamera = camera != null ? camera : new PhotonCamera(privateCameraName(name));
    SimCameraProperties properties =
        config.getCameraProperties().orElseGet(defaultModel::getSimCameraProperties);
    var cameraSim = new PhotonCameraSim(photonCamera, properties, config.getField());
    cameraSim.enableRawStream(config.isSimulatedVideoStreamsEnabled());
    cameraSim.enableProcessedStream(config.isSimulatedVideoStreamsEnabled());
    cameraSim.enableDrawWireframe(
        config.isSimulatedVideoStreamsEnabled() && config.isSimulatedWireframeEnabled());
    config
        .getVisionSim()
        .orElseGet(VisionSim::getDefault)
        .addCamera(cameraSim, config.getRobotToCamera().orElse(Transform3d.ZERO));
    return cameraSim;
  }

  /**
   * Name of the private PhotonVision camera made for a camera that isn't a PhotonVision camera.
   *
   * @param cameraName Name of the camera.
   * @return {@code YALL-<cameraName>}.
   */
  public static String privateCameraName(String cameraName) {
    return "YALL-" + cameraName;
  }

  /**
   * Adds a simulated camera. {@link yall.VisionCamera} calls this; teams don't need to.
   *
   * @param cameraSim The simulated camera.
   * @param robotToCamera Where the camera is on the robot.
   */
  public void addCamera(PhotonCameraSim cameraSim, Transform3d robotToCamera) {
    visionSystemSim.addCamera(cameraSim, robotToCamera);
  }

  /**
   * Removes a simulated camera.
   *
   * @param cameraSim The simulated camera.
   */
  public void removeCamera(PhotonCameraSim cameraSim) {
    visionSystemSim.removeCamera(cameraSim);
  }

  /**
   * Moves a simulated camera, for a camera on a turret or other mechanism.
   *
   * @param cameraSim The simulated camera.
   * @param robotToCamera Where the camera is on the robot now.
   */
  public void adjustCamera(PhotonCameraSim cameraSim, Transform3d robotToCamera) {
    visionSystemSim.adjustCamera(cameraSim, robotToCamera);
  }

  /**
   * Adds targets other than the field's AprilTags, such as game pieces for object detection: use
   * {@link VisionTargetSim#VisionTargetSim(Pose3d, org.photonvision.estimation.TargetModel, int,
   * float)} with a class ID.
   *
   * @param type Name of the group of targets, for the debug field.
   * @param targets The targets.
   */
  public void addVisionTargets(String type, VisionTargetSim... targets) {
    visionSystemSim.addVisionTargets(type, targets);
  }

  /**
   * Removes a group of targets added with {@link #addVisionTargets}.
   *
   * @param type Name of the group.
   */
  public void removeVisionTargets(String type) {
    visionSystemSim.removeVisionTargets(type);
  }

  /**
   * Moves the simulated robot and processes a frame on each camera that is due one.
   *
   * @param robotPose The robot's true pose.
   */
  public void update(Pose2d robotPose) {
    visionSystemSim.update(robotPose);
  }

  /**
   * Moves the simulated robot and processes a frame on each camera that is due one.
   *
   * @param robotPose The robot's true pose.
   */
  public void update(Pose3d robotPose) {
    visionSystemSim.update(robotPose);
  }

  /**
   * Moves the simulated robot without processing frames, for example when resetting the robot pose.
   *
   * @param robotPose The robot's new true pose.
   */
  public void resetRobotPose(Pose2d robotPose) {
    visionSystemSim.resetRobotPose(robotPose);
  }

  /**
   * A field drawing of the simulated robot, cameras and the targets they see.
   *
   * @return The debug field.
   */
  public Field2d getDebugField() {
    return visionSystemSim.getDebugField();
  }

  /**
   * The field whose AprilTags the cameras see.
   *
   * @return The field.
   */
  public Field getField() {
    return field;
  }

  /**
   * The PhotonVision simulation underneath, for settings YALL doesn't cover.
   *
   * @return PhotonVision's vision system simulation.
   */
  public VisionSystemSim getVisionSystemSim() {
    return visionSystemSim;
  }

  /**
   * Finds a simulated camera by its PhotonVision name.
   *
   * @param name The simulated camera's name.
   * @return The camera, if it is in this world.
   */
  public Optional<PhotonCameraSim> getCameraSim(String name) {
    return visionSystemSim.getCameraSim(name);
  }

  /**
   * Injects a deterministic camera blackout lasting {@code duration} starting at {@code
   * currentTimeSeconds}. Useful for testing vision-loss fallback and recovery in simulation.
   *
   * @param currentTimeSeconds Current monotonic simulation timestamp in seconds.
   * @param duration Blackout duration (must be non-negative).
   * @return This simulated world, for chaining.
   */
  public VisionSim injectBlackout(double currentTimeSeconds, Time duration) {
    double durationSec = duration.in(Seconds);
    if (!Double.isFinite(currentTimeSeconds)
        || !Double.isFinite(durationSec)
        || durationSec < 0.0) {
      throw new IllegalArgumentException(
          "Blackout timestamp and duration must be finite and non-negative");
    }
    if (durationSec > 0.0) {
      blackoutUntilSeconds = Math.max(blackoutUntilSeconds, currentTimeSeconds + durationSec);
    }
    return this;
  }

  /**
   * Whether this simulated world is currently in an injected blackout window at {@code nowSeconds}.
   *
   * @param nowSeconds Current monotonic timestamp in seconds.
   * @return true if frames should be suppressed at {@code nowSeconds}.
   */
  public boolean isBlackedOut(double nowSeconds) {
    return nowSeconds < blackoutUntilSeconds;
  }

  /**
   * Configures deterministic per-frame stochastic dropout probability for simulated cameras.
   *
   * @param probability Per-frame drop probability in {@code [0.0, 1.0]}.
   * @param seed Deterministic RNG seed for reproducible CI runs.
   * @return This simulated world, for chaining.
   */
  public VisionSim withDropoutProbability(double probability, long seed) {
    if (!Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("probability must be finite and in [0.0, 1.0]");
    }
    this.dropoutProbability = probability;
    this.faultRng = new SplittableRandom(seed);
    return this;
  }

  /**
   * Configures Gaussian X/Y field-tag placement jitter (for example, {@code Millimeters.of(13)} to
   * model Welded vs. AndyMark AprilTag field tolerances) applied to simulated pose estimates.
   *
   * @param stdDev Standard deviation of X and Y pose jitter (zero disables jitter).
   * @param seed Deterministic RNG seed for reproducible CI runs.
   * @return This simulated world, for chaining.
   */
  public VisionSim withFieldTagJitter(Distance stdDev, long seed) {
    double meters = stdDev.in(Meters);
    if (!Double.isFinite(meters) || meters < 0.0) {
      throw new IllegalArgumentException("stdDev must be finite and non-negative");
    }
    this.fieldTagJitterStdDevMeters = meters;
    this.faultRng = new SplittableRandom(seed ^ 0x9e3779b97f4a7c15L);
    return this;
  }

  /** Clears any active blackout, stochastic dropout probability, and field-tag jitter. */
  public void clearFaults() {
    blackoutUntilSeconds = Double.NEGATIVE_INFINITY;
    dropoutProbability = 0.0;
    fieldTagJitterStdDevMeters = 0.0;
  }

  /**
   * Evaluates whether a simulated frame arriving at {@code nowSeconds} should be dropped due to an
   * active blackout or stochastic dropout probability.
   *
   * @param nowSeconds Current monotonic timestamp in seconds.
   * @return true if the simulated frame should be dropped.
   */
  public boolean shouldDropFrame(double nowSeconds) {
    if (isBlackedOut(nowSeconds)) {
      return true;
    }
    return dropoutProbability > 0.0 && faultRng.nextDouble() < dropoutProbability;
  }

  /**
   * Applies configured Gaussian X/Y field-tag placement jitter to a simulated robot pose. Returns
   * {@code pose} unchanged when jitter is disabled.
   *
   * @param pose Simulated robot pose estimate.
   * @return Pose with optional deterministic X/Y jitter applied.
   */
  public Pose3d applyFieldTagJitter(Pose3d pose) {
    if (fieldTagJitterStdDevMeters <= 0.0 || pose == null) {
      return pose;
    }
    double dx = faultRng.nextGaussian() * fieldTagJitterStdDevMeters;
    double dy = faultRng.nextGaussian() * fieldTagJitterStdDevMeters;
    return new Pose3d(
        new Translation3d(pose.getX() + dx, pose.getY() + dy, pose.getZ()), pose.getRotation());
  }
}
