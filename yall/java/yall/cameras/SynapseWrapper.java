package yall.cameras;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.photonvision.simulation.SimCameraProperties;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.PubSubOption;
import org.wpilib.networktables.RawSubscriber;
import org.wpilib.networktables.TimestampedRaw;
import synapse.SynapseCamera;
import synapse.SynapsePipelineType;
import synapse.pipelines.apriltag.ApriltagDetection;
import synapse.pipelines.apriltag.ApriltagResult;
import synapse.pipelines.color.ColorDetection;
import synapse.pipelines.color.ColorResult;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.results.VisionPoseEstimate;
import yall.results.VisionResult;
import yall.results.VisionTarget;

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
 * <p>No vendor configs are accepted; use {@link #getCamera()} for Synapse settings.
 */
public class SynapseWrapper extends VisionCamera {

  private static final double NT_TICKS_PER_SECOND = 1.0e9;

  private final SynapseCamera camera;
  private final RawSubscriber resultsSubscriber;

  /**
   * Wraps a Synapse camera.
   *
   * @param camera The Synapse camera.
   * @param config The camera's config.
   */
  public SynapseWrapper(SynapseCamera camera, VisionCameraConfig config) {
    super(camera.getCameraName(), config, null);
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
  protected List<Frame> readFrames() {
    var frames = new ArrayList<Frame>();
    boolean apriltag = SynapsePipelineType.kApriltag.typestring.equals(camera.getPipelineType());
    boolean color = SynapsePipelineType.kColor.typestring.equals(camera.getPipelineType());
    double latencySeconds = camera.getCaptureLatency() + camera.getProcessLatency();
    for (TimestampedRaw raw : resultsSubscriber.readQueue()) {
      if (raw.value.length == 0) {
        continue;
      }
      double timestamp = raw.timestamp / NT_TICKS_PER_SECOND - latencySeconds;
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
    return config.getCameraProperties().orElseGet(this::defaultCameraProperties);
  }

  private Frame toFrame(ApriltagResult result, double timestamp, double latencySeconds) {
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
      return new Frame(visionResult, Optional.empty());
    }
    var field = config.getField();
    var tagIds = Arrays.stream(tags).map(t -> t.tagID).toList();
    double distance =
        tagIds.stream()
            .map(field::getTagPose)
            .flatMap(Optional::stream)
            .mapToDouble(p -> p.getTranslation().getDistance(cameraPose.getTranslation()))
            .average()
            .orElse(Double.NaN);
    var estimate =
        VisionPoseEstimate.unfiltered(
            name,
            cameraPose.transformBy(getRobotToCamera().inverse()),
            timestamp,
            tagIds,
            distance,
            tagIds.size() == 1 ? Double.NaN : 0,
            false);
    return new Frame(visionResult, Optional.of(estimate));
  }

  private Frame toFrame(ColorResult result, double timestamp, double latencySeconds) {
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
              -1,
              0,
              Double.NaN,
              Optional.empty()));
    }
    return new Frame(
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

  /**
   * A 1280x720 USB camera with a 75° diagonal field of view. Set the real camera's properties in the
   * config: Synapse needs them to turn pixels into angles.
   *
   * @return Camera properties.
   */
  @Override
  protected SimCameraProperties defaultCameraProperties() {
    var properties = new SimCameraProperties();
    properties.setCalibration(1280, 720, Rotation2d.fromDegrees(75));
    properties.setCalibError(0.25, 0.08);
    properties.setFPS(30);
    properties.setAvgLatencyMs(35);
    properties.setLatencyStdDevMs(5);
    return properties;
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
}
