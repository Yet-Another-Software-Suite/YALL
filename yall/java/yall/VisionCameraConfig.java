package yall;

import java.util.Optional;
import org.photonvision.simulation.SimCameraProperties;
import org.wpilib.fields.Field;
import org.wpilib.fields.Fields;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Transform3d;
import yall.filter.VisionPoseFilter;
import yall.sim.VisionSim;

/**
 * Settings shared by every {@link VisionCamera}, whatever the vendor.
 *
 * <pre>{@code
 * VisionCameraConfig config = new VisionCameraConfig()
 *     .withRobotToCamera(new Transform3d(0.3, 0, 0.25, new Rotation3d(0, Math.toRadians(-15), 0)))
 *     .withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED)
 *     .withPoseFilter(new VisionPoseFilter().withMaxAmbiguity(0.15));
 * }</pre>
 *
 * <p>Vendor settings that YALL doesn't cover go through {@link #withVendorConfig(Object)}, or
 * straight to the vendor object from {@link VisionCamera#getCamera()}.
 */
public class VisionCameraConfig {

  private Optional<Transform3d> robotToCamera = Optional.empty();
  private Field field;
  private PoseEstimationMode poseEstimationMode = PoseEstimationMode.TAGS_ONLY;
  private VisionPoseFilter poseFilter = new VisionPoseFilter();
  private Optional<Integer> pipelineIndex = Optional.empty();
  private Optional<SimCameraProperties> cameraProperties = Optional.empty();
  private boolean simulationEnabled = true;
  private Optional<VisionSim> visionSim = Optional.empty();
  private boolean simVideoStreams = false;
  private Optional<Object> vendorConfig = Optional.empty();
  private boolean telemetry = true;

  /** Creates a config with the defaults: no camera offset, the current season's field. */
  public VisionCameraConfig() {}

  /**
   * Sets where the camera is on the robot. Pose estimates, simulation and Synapse's robot pose all
   * need it. On a Limelight it overrides the camera pose set in the web interface.
   *
   * @param robotToCamera Transform from the robot origin to the camera lens.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withRobotToCamera(Transform3d robotToCamera) {
    this.robotToCamera = Optional.of(robotToCamera);
    return this;
  }

  /**
   * Sets the field, which holds the AprilTag layout. Defaults to WPILib's {@link
   * Fields#DEFAULT_FIELD}.
   *
   * @param field The field.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withField(Field field) {
    this.field = field;
    return this;
  }

  /**
   * Sets how AprilTags become a robot pose. Defaults to {@link PoseEstimationMode#TAGS_ONLY}.
   *
   * @param mode The pose estimation mode.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withPoseEstimationMode(PoseEstimationMode mode) {
    poseEstimationMode = mode;
    return this;
  }

  /**
   * Sets the filter that accepts or rejects pose estimates and weighs them. Defaults to a new {@link
   * VisionPoseFilter}, which has every standard check on.
   *
   * @param filter The filter.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withPoseFilter(VisionPoseFilter filter) {
    poseFilter = filter;
    return this;
  }

  /**
   * Sets the pipeline to switch to when the camera is created.
   *
   * @param index Pipeline index.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withPipelineIndex(int index) {
    pipelineIndex = Optional.of(index);
    return this;
  }

  /**
   * Sets the camera's resolution, field of view, frame rate, latency and noise. Simulation uses them,
   * and Synapse uses them to turn pixel positions into angles. Each vendor wrapper has a sensible
   * default.
   *
   * @param properties PhotonVision camera properties.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withCameraProperties(SimCameraProperties properties) {
    cameraProperties = Optional.of(properties);
    return this;
  }

  /**
   * Sets the camera's resolution and diagonal field of view, keeping the rest of the default camera
   * properties.
   *
   * @param widthPixels Image width.
   * @param heightPixels Image height.
   * @param diagonalFov Diagonal field of view.
   * @param fps Frames per second.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withCameraProperties(
      int widthPixels, int heightPixels, Rotation2d diagonalFov, double fps) {
    var properties = new SimCameraProperties();
    properties.setCalibration(widthPixels, heightPixels, diagonalFov);
    properties.setFPS(fps);
    properties.setAvgLatencyMs(25);
    properties.setLatencyStdDevMs(5);
    properties.setCalibError(0.25, 0.08);
    return withCameraProperties(properties);
  }

  /**
   * Sets whether the camera gives simulated results when the robot program runs in simulation.
   * Defaults to true.
   *
   * @param enabled true to simulate.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withSimulation(boolean enabled) {
    simulationEnabled = enabled;
    return this;
  }

  /**
   * Sets the simulated world the camera belongs to. Defaults to {@link VisionSim#getDefault()}.
   *
   * @param sim The simulated world.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withSimulation(VisionSim sim) {
    visionSim = Optional.of(sim);
    simulationEnabled = true;
    return this;
  }

  /**
   * Sets whether the simulated camera streams rendered video. The streams cost CPU, so they default
   * to off.
   *
   * @param enabled true to stream raw and processed simulated video.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withSimulatedVideoStreams(boolean enabled) {
    simVideoStreams = enabled;
    return this;
  }

  /**
   * Passes a vendor config object to the camera, as YAMS does for motor controllers. Each wrapper
   * documents the types it accepts and throws {@link IllegalArgumentException} for others.
   *
   * @param vendorConfig Vendor config object.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withVendorConfig(Object vendorConfig) {
    this.vendorConfig = Optional.ofNullable(vendorConfig);
    return this;
  }

  /**
   * Sets whether the camera publishes its targets and pose estimates to NetworkTables under {@code
   * YALL/<camera name>}. Defaults to true.
   *
   * @param enabled true to publish.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withTelemetry(boolean enabled) {
    telemetry = enabled;
    return this;
  }

  /**
   * Transform from the robot origin to the camera.
   *
   * @return The transform, if set.
   */
  public Optional<Transform3d> getRobotToCamera() {
    return robotToCamera;
  }

  /**
   * The field holding the AprilTag layout.
   *
   * @return The configured field, or {@link Fields#DEFAULT_FIELD}.
   */
  public Field getField() {
    if (field == null) {
      field = Field.loadField(Fields.DEFAULT_FIELD);
    }
    return field;
  }

  /**
   * How AprilTags become a robot pose.
   *
   * @return The pose estimation mode.
   */
  public PoseEstimationMode getPoseEstimationMode() {
    return poseEstimationMode;
  }

  /**
   * The pose estimate filter.
   *
   * @return The filter.
   */
  public VisionPoseFilter getPoseFilter() {
    return poseFilter;
  }

  /**
   * Pipeline to switch to on creation.
   *
   * @return The pipeline index, if set.
   */
  public Optional<Integer> getPipelineIndex() {
    return pipelineIndex;
  }

  /**
   * Camera properties.
   *
   * @return The properties, if set.
   */
  public Optional<SimCameraProperties> getCameraProperties() {
    return cameraProperties;
  }

  /**
   * Whether the camera is simulated in simulation.
   *
   * @return true to simulate.
   */
  public boolean isSimulationEnabled() {
    return simulationEnabled;
  }

  /**
   * The simulated world.
   *
   * @return The world, if set.
   */
  public Optional<VisionSim> getVisionSim() {
    return visionSim;
  }

  /**
   * Whether the simulated camera streams video.
   *
   * @return true to stream.
   */
  public boolean isSimulatedVideoStreamsEnabled() {
    return simVideoStreams;
  }

  /**
   * Vendor config object.
   *
   * @return The object, if set.
   */
  public Optional<Object> getVendorConfig() {
    return vendorConfig;
  }

  /**
   * Whether the camera publishes telemetry.
   *
   * @return true to publish.
   */
  public boolean isTelemetryEnabled() {
    return telemetry;
  }
}
