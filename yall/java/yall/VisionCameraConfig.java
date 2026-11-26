package yall;

import static org.wpilib.units.Units.Hertz;
import static org.wpilib.units.Units.Radians;

import java.util.Optional;
import org.photonvision.simulation.SimCameraProperties;
import org.wpilib.fields.Field;
import org.wpilib.fields.Fields;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.geometry.Translation3d;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.Distance;
import org.wpilib.units.measure.Frequency;
import yall.filter.VisionPoseFilter;
import yall.sim.CameraModel;
import yall.sim.VisionSim;
import yall.telemetry.VisionTelemetryConfig;

/**
 * Settings shared by every {@link VisionCamera}, whatever the vendor.
 *
 * <pre>{@code
 * VisionCameraConfig config = new VisionCameraConfig()
 *     .withRobotToCamera(Inches.of(12), Inches.of(0), Inches.of(10),
 *                        Degrees.of(0), Degrees.of(-15), Degrees.of(0))
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
  private boolean simWireframe = false;
  private Optional<Object> vendorConfig = Optional.empty();
  private Optional<VisionTelemetryConfig> telemetry = Optional.of(new VisionTelemetryConfig());

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
   * Sets where the camera is on the robot, measured from the robot's center on the floor. The
   * arguments follow the order of Limelight's own camera pose: forward, left, up, then roll, pitch,
   * yaw.
   *
   * <pre>{@code
   * config.withRobotToCamera(
   *     Inches.of(12), Inches.of(0), Inches.of(10), // 12" forward, centered, 10" up
   *     Degrees.of(0), Degrees.of(-15), Degrees.of(0)); // tilted 15° up
   * }</pre>
   *
   * @param forward Distance forward of the robot center. Negative is behind it.
   * @param left Distance left of the robot center. Negative is right of it.
   * @param up Height above the floor.
   * @param roll Rotation about the camera's forward axis, counterclockwise positive.
   * @param pitch Rotation about the left axis. Negative tilts the camera up.
   * @param yaw Rotation about the up axis, counterclockwise positive. 180° faces backward.
   * @return This config, for chaining.
   * @see #withRobotToCamera(Transform3d)
   */
  public VisionCameraConfig withRobotToCamera(
      Distance forward, Distance left, Distance up, Angle roll, Angle pitch, Angle yaw) {
    return withRobotToCamera(robotToCamera(forward, left, up, roll, pitch, yaw));
  }

  /**
   * Builds a robot-to-camera transform from measurements.
   *
   * @param forward Distance forward of the robot center.
   * @param left Distance left of the robot center.
   * @param up Height above the floor.
   * @param roll Rotation about the forward axis.
   * @param pitch Rotation about the left axis.
   * @param yaw Rotation about the up axis.
   * @return The transform.
   */
  static Transform3d robotToCamera(
      Distance forward, Distance left, Distance up, Angle roll, Angle pitch, Angle yaw) {
    return new Transform3d(new Translation3d(forward, left, up), new Rotation3d(roll, pitch, yaw));
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
   * and Synapse uses them to turn pixel positions into angles. Each vendor wrapper has a default;
   * {@link #withCameraModel(CameraModel)} sets them from a camera's published specs.
   *
   * @param properties PhotonVision camera properties.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withCameraProperties(SimCameraProperties properties) {
    cameraProperties = Optional.of(properties);
    return this;
  }

  /**
   * Sets the camera's resolution, field of view and frame rate from a camera's published specs. Use
   * the model that matches the real camera, so simulation sees what it would.
   *
   * @param model The camera model.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withCameraModel(CameraModel model) {
    return withCameraProperties(model.getSimCameraProperties());
  }

  /**
   * Sets the camera's resolution and diagonal field of view, keeping the rest of the default camera
   * properties.
   *
   * @param widthPixels Image width.
   * @param heightPixels Image height.
   * @param diagonalFov Diagonal field of view.
   * @param frameRate Frame rate.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withCameraProperties(
      int widthPixels, int heightPixels, Angle diagonalFov, Frequency frameRate) {
    var properties = new SimCameraProperties();
    properties.setCalibration(
        widthPixels, heightPixels, Rotation2d.fromRadians(diagonalFov.in(Radians)));
    properties.setFPS(frameRate.in(Hertz));
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
   * Sets whether the simulated camera streams what it would see: PhotonVision draws the AprilTags
   * (and, with {@link #withSimulatedWireframe(boolean)}, the field) from the camera's point of view.
   * The streams cost CPU, so they default to off.
   *
   * <p>The streams are served by WPILib's CameraServer as {@code YALL-<camera name>-raw} and {@code
   * YALL-<camera name>-processed} ({@code <camera name>-raw} and {@code -processed} for a
   * PhotonVision camera). Open them in a dashboard's camera stream widget, or in a browser at {@code
   * http://localhost:1181}, {@code 1182} and up, one port per stream in the order they were made.
   *
   * @param enabled true to stream raw and processed simulated video.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withSimulatedVideoStreams(boolean enabled) {
    simVideoStreams = enabled;
    return this;
  }

  /**
   * Sets whether the simulated video streams also draw the field's floor and walls, which helps to
   * see where the camera points. Drawing them costs more CPU than the AprilTags alone, so it
   * defaults to off. Has no effect unless {@link #withSimulatedVideoStreams(boolean)} is on.
   *
   * @param enabled true to draw the field.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withSimulatedWireframe(boolean enabled) {
    simWireframe = enabled;
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
   * Sets whether the camera publishes telemetry to NetworkTables, with the default {@link
   * VisionTelemetryConfig}: every {@link yall.telemetry.TelemetryVerbosity#MID} field under {@code
   * YALL/<camera name>}. Defaults to true.
   *
   * @param enabled true to publish.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withTelemetry(boolean enabled) {
    telemetry = enabled ? Optional.of(new VisionTelemetryConfig()) : Optional.empty();
    return this;
  }

  /**
   * Sets what the camera publishes to NetworkTables and logs to a DataLog, and where.
   *
   * @param telemetryConfig What to publish, or null to publish nothing.
   * @return This config, for chaining.
   */
  public VisionCameraConfig withTelemetry(VisionTelemetryConfig telemetryConfig) {
    telemetry = Optional.ofNullable(telemetryConfig);
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
   * Whether the simulated video streams draw the field.
   *
   * @return true to draw the field.
   */
  public boolean isSimulatedWireframeEnabled() {
    return simWireframe;
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
    return telemetry.isPresent();
  }

  /**
   * What the camera publishes to NetworkTables.
   *
   * @return The telemetry config, or empty to publish nothing.
   */
  public Optional<VisionTelemetryConfig> getTelemetryConfig() {
    return telemetry;
  }
}
