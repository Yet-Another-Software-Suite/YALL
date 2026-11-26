package yall.cameras;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.photonvision.simulation.SimCameraProperties;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.PubSubOption;
import org.wpilib.networktables.RawSubscriber;
import org.wpilib.networktables.TimestampedRaw;
import org.wpilib.system.Timer;
import synapse.SynapseCamera;
import synapse.SynapsePipelineType;
import synapse.pipelines.apriltag.ApriltagDetection;
import synapse.pipelines.apriltag.ApriltagResult;
import synapse.pipelines.color.ColorDetection;
import synapse.pipelines.color.ColorResult;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.VisionFrame;
import yall.results.VisionPoseEstimate;
import yall.results.VisionResult;
import yall.results.VisionTarget;
import yall.sim.CameraModel;
import yall.sim.VisionSim;

/**
 * A Synapse camera.
 *
 * <p>Synapse reports target positions in pixels; this wrapper turns them into angles with the
 * config's {@link VisionCameraConfig#withCameraProperties camera properties}, so set them to match
 * the camera. Synapse doesn't report target area or AprilTag ambiguity, so those are {@link
 * Double#NaN} and the pose filter's ambiguity check is skipped.
 *
 * <p>Pose estimates come from Synapse's field-space camera pose and the config's robot-to-camera
 * transform. Synapse has no heading-assisted mode, so {@link
 * yall.PoseEstimationMode#HEADING_ASSISTED} works like {@link yall.PoseEstimationMode#TAGS_ONLY}.
 *
 * <p>Without camera properties in the config, it is simulated, and turns pixels into angles, as a
 * {@link CameraModel#ARDUCAM_OV9281}.
 *
 * <p>No vendor configs are accepted; use {@link #getCamera()} for Synapse settings.
 */
public class SynapseWrapper extends VisionCamera {

  private static final double NT_NANOS_PER_SECOND = 1.0e9;
  private static final double NT_MICROS_PER_SECOND = 1.0e6;

  private final SynapseCamera camera;
  private final RawSubscriber resultsSubscriber;

  /**
   * Wraps a Synapse camera.
   *
   * @param camera The Synapse camera.
   * @param config The camera's config.
   */
  public SynapseWrapper(SynapseCamera camera, VisionCameraConfig config) {
    super(
        camera.getCameraName(),
        config,
        VisionSim.createCameraSim(
            camera.getCameraName(), config, CameraModel.ARDUCAM_OV9281, null));
    this.camera = camera;
    config
        .getVendorConfig()
        .ifPresent(
            vendorConfig -> {
              throw new IllegalArgumentException(
                  "SynapseWrapper takes no vendor config, got "
                      + vendorConfig.getClass().getName()
                      + ". Use getCamera() for Synapse settings.");
            });
    camera.setRobotToCameraOffset(getRobotToCamera());
    // Our own subscriber, so every frame is read once with its arrival time.
    resultsSubscriber =
        NetworkTableInstance.getDefault()
            .getTable(camera.synapseTableName)
            .getSubTable(camera.getCameraName())
            .getSubTable("data")
            .getRawTopic("results")
            .subscribe(
                "raw", new byte[0], PubSubOption.KEEP_DUPLICATES, PubSubOption.pollStorage(20));
    config.getPipelineIndex().ifPresent(this::setPipelineIndex);
  }

  @Override
  protected List<VisionFrame> readFrames() {
    var frames = new ArrayList<VisionFrame>();
    boolean apriltag = SynapsePipelineType.kApriltag.typestring.equals(camera.getPipelineType());
    boolean color = SynapsePipelineType.kColor.typestring.equals(camera.getPipelineType());
    double nowSeconds = Timer.getMonotonicTimestamp();
    double latencySeconds = Math.max(0.0, camera.getCaptureLatency() + camera.getProcessLatency());
    for (TimestampedRaw raw : resultsSubscriber.readQueue()) {
      if (raw.value.length == 0) {
        continue;
      }
      double timestamp = resolveTimestampSeconds(raw.timestamp, nowSeconds, latencySeconds);
      if (apriltag) {
        camera
            .deserializeResults(SynapsePipelineType.kApriltag.getTypeReference(), raw.value)
            .ifPresent(r -> frames.add(toFrame(r, timestamp, latencySeconds)));
      } else if (color) {
        camera
            .deserializeResults(SynapsePipelineType.kColor.getTypeReference(), raw.value)
            .ifPresent(r -> frames.add(toFrame(r, timestamp, latencySeconds)));
      }
    }
    return frames;
  }

  private SimCameraProperties cameraProperties() {
    return config
        .getCameraProperties()
        .orElseGet(CameraModel.ARDUCAM_OV9281::getSimCameraProperties);
  }

  private VisionFrame toFrame(ApriltagResult result, double timestamp, double latencySeconds) {
    var properties = cameraProperties();
    var tags = result.tags == null ? new ApriltagDetection[0] : result.tags;
    var targets = new ArrayList<VisionTarget>(tags.length);
    for (ApriltagDetection tag : tags) {
      targets.add(
          new VisionTarget(
              -properties.getPixelYaw(tag.tx()).getDegrees(),
              -properties.getPixelPitch(tag.ty()).getDegrees(),
              Double.NaN,
              tag.tagID,
              -1,
              0,
              Double.NaN,
              Optional.empty()));
    }
    var visionResult = new VisionResult(name, timestamp, latencySeconds * 1000, targets);
    Pose3d cameraPose = result.cameraEstimate_fieldSpace3d();
    if (cameraPose == null || tags.length == 0) {
      return new VisionFrame(visionResult, Optional.empty());
    }
    var field = config.getField();
    var tagIds = new ArrayList<Integer>(tags.length);
    double distSum = 0.0;
    int distCount = 0;
    for (int i = 0; i < tags.length; i++) {
      int id = tags[i].tagID;
      tagIds.add(id);
      var tagPose = field.getTagPose(id);
      if (tagPose.isPresent()) {
        distSum += tagPose.get().getTranslation().getDistance(cameraPose.getTranslation());
        distCount++;
      }
    }
    double distance = distCount > 0 ? distSum / distCount : Double.NaN;
    var estimate =
        VisionPoseEstimate.unfiltered(
            name,
            cameraPose.transformBy(getRobotToCamera().inverse()),
            timestamp,
            tagIds,
            distance,
            tagIds.size() == 1 ? Double.NaN : 0,
            false);
    return new VisionFrame(visionResult, Optional.of(estimate));
  }

  private VisionFrame toFrame(ColorResult result, double timestamp, double latencySeconds) {
    var properties = cameraProperties();
    var detections = result.detections == null ? new ColorDetection[0] : result.detections;
    var targets = new ArrayList<VisionTarget>(detections.length);
    for (ColorDetection detection : detections) {
      if (detection.center == null || detection.center.length < 2) {
        continue;
      }
      targets.add(
          new VisionTarget(
              -properties.getPixelYaw(detection.center[0]).getDegrees(),
              -properties.getPixelPitch(detection.center[1]).getDegrees(),
              Double.NaN,
              -1,
              0,
              1.0,
              Double.NaN,
              Optional.empty()));
    }
    return new VisionFrame(
        new VisionResult(name, timestamp, latencySeconds * 1000, targets), Optional.empty());
  }

  @Override
  protected void applyPipelineIndex(int index) {
    camera.setPipeline(index);
  }

  @Override
  protected int readPipelineIndex() {
    return (int) camera.getPipeline();
  }

  @Override
  protected boolean readConnected() {
    return resultsSubscriber.exists();
  }

  @Override
  protected void applyRobotToCamera(Transform3d robotToCamera) {
    camera.setRobotToCameraOffset(robotToCamera);
  }

  @Override
  public SynapseCamera getCamera() {
    return camera;
  }

  /** Closes the results subscriber, then the parts every camera shares. */
  @Override
  public void close() {
    resultsSubscriber.close();
    super.close();
  }

  /**
   * Converts a NetworkTables {@code TimestampedRaw.timestamp} tick value into monotonic capture
   * seconds, automatically handling nanosecond ({@code 1e9}) vs. microsecond ({@code 1e6}) NT4
   * tick scales relative to {@code nowSeconds}.
   */
  static double resolveTimestampSeconds(
      long rawTimestampTicks, double nowSeconds, double latencySeconds) {
    double safeLatency =
        (Double.isFinite(latencySeconds) && latencySeconds > 0.0) ? latencySeconds : 0.0;
    double asNanos = rawTimestampTicks / NT_NANOS_PER_SECOND;
    double asMicros = rawTimestampTicks / NT_MICROS_PER_SECOND;
    double arrivalSeconds =
        (Double.isFinite(nowSeconds)
                && Math.abs(asMicros - nowSeconds) < Math.abs(asNanos - nowSeconds))
            ? asMicros
            : asNanos;
    return arrivalSeconds - safeLatency;
  }
}
