package yall.cameras;

import com.limelightvision.DetectorTarget;
import com.limelightvision.FiducialTarget;
import com.limelightvision.Limelight;
import com.limelightvision.LimelightResults;
import com.limelightvision.LimelightTarget;
import com.limelightvision.PipelineConfiguration;
import com.limelightvision.PoseEstimate;
import com.limelightvision.PoseEstimateConfig;
import com.limelightvision.PoseEstimateType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import yall.PoseEstimationMode;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.VisionFrame;
import yall.results.VisionPoseEstimate;
import yall.results.VisionResult;
import yall.results.VisionTarget;
import yall.sim.CameraModel;
import yall.sim.VisionSim;

/**
 * A Limelight, through LimelightLib 2.
 *
 * <p>Pose estimates are MegaTag1 for {@link PoseEstimationMode#TAGS_ONLY} and MegaTag2 for {@link
 * PoseEstimationMode#HEADING_ASSISTED}, in WPILib blue-origin coordinates. The config's
 * robot-to-camera transform, if set, overrides the camera pose from the Limelight web interface.
 *
 * <p>Vendor configs accepted by {@link VisionCameraConfig#withVendorConfig(Object)}:
 *
 * <ul>
 *   <li>{@link PoseEstimateConfig}: Limelight's own pose filter and standard deviations replace
 *       YALL's {@link yall.filter.VisionPoseFilter} for this camera.
 *   <li>{@link PipelineConfiguration}: applied as a pipeline override.
 * </ul>
 *
 * <p>Without a camera model in the config, it is simulated as a {@link CameraModel#LIMELIGHT_4}.
 *
 * <p>This wrapper reads {@link Limelight#readResultsQueue()}, so don't read any of the Limelight's
 * queues elsewhere: each frame is handed out only once.
 */
public class LimelightWrapper extends VisionCamera {

  private final Limelight limelight;
  private boolean useLimelightFilter;

  /**
   * Wraps a Limelight.
   *
   * @param limelight The Limelight.
   * @param config The camera's config.
   */
  public LimelightWrapper(Limelight limelight, VisionCameraConfig config) {
    super(
        limelight.getName(),
        config,
        VisionSim.createCameraSim(limelight.getName(), config, CameraModel.LIMELIGHT_4, null));
    this.limelight = limelight;
    config.getVendorConfig().ifPresent(this::applyVendorConfig);
    if (!isSimulated()) {
      config.getRobotToCamera().ifPresent(this::applyRobotToCamera);
    }
    config.getPipelineIndex().ifPresent(this::setPipelineIndex);
  }

  private void applyVendorConfig(Object vendorConfig) {
    if (vendorConfig instanceof PoseEstimateConfig poseConfig) {
      limelight.withPoseEstimateConfig_MT1(poseConfig);
      limelight.withPoseEstimateConfig_MT2(poseConfig);
      useLimelightFilter = true;
    } else if (vendorConfig instanceof PipelineConfiguration pipelineConfig) {
      limelight.setPipelineConfigurationOverride(pipelineConfig);
    } else {
      throw new IllegalArgumentException(
          "LimelightWrapper takes a PoseEstimateConfig or PipelineConfiguration as vendor config,"
              + " got "
              + vendorConfig.getClass().getName());
    }
  }

  private PoseEstimateType poseEstimateType() {
    return config.getPoseEstimationMode() == PoseEstimationMode.HEADING_ASSISTED
        ? PoseEstimateType.MT2_WPIBLUE
        : PoseEstimateType.MT1_WPIBLUE;
  }

  @Override
  protected List<VisionFrame> readFrames() {
    var frames = new ArrayList<VisionFrame>();
    for (LimelightResults results : limelight.readResultsQueue()) {
      frames.add(toFrame(results));
    }
    return frames;
  }

  private VisionFrame toFrame(LimelightResults results) {
    double latencyMillis = results.captureLatencyMillis + results.targetingLatencyMillis;
    double timestamp = results.receiveTimestampSeconds - latencyMillis / 1000.0;
    var targets = new ArrayList<VisionTarget>();
    // A Limelight target's tx is positive right of the crosshair, as PhotonVision's yaw is, so it
    // passes through. Its ty is positive down, so it is negated to give a positive-up pitch.
    for (FiducialTarget tag : results.fiducialTargets) {
      Pose3d cameraToTag = tag.getTargetPose_CameraSpace();
      targets.add(
          new VisionTarget(
              tag.txDegrees,
              -tag.tyDegrees,
              tag.targetAreaPercent,
              tag.fiducialId,
              -1,
              0,
              tag.ambiguity,
              Optional.of(
                  new Transform3d(cameraToTag.getTranslation(), cameraToTag.getRotation()))));
    }
    for (DetectorTarget detection : results.detectorTargets) {
      targets.add(
          new VisionTarget(
              detection.txDegrees,
              -detection.tyDegrees,
              detection.targetAreaPercent,
              -1,
              detection.classId,
              detection.confidence,
              Double.NaN,
              Optional.empty()));
    }
    for (LimelightTarget retro : results.retroTargets) {
      targets.add(
          new VisionTarget(
              retro.txDegrees,
              -retro.tyDegrees,
              retro.targetAreaPercent,
              -1,
              -1,
              0,
              Double.NaN,
              Optional.empty()));
    }
    var result = new VisionResult(name, timestamp, latencyMillis, targets);

    var type = poseEstimateType();
    PoseEstimate poseEstimate = limelight.getPoseEstimate(results, type);
    Pose3d pose = results.getRobotPose(type);
    if (poseEstimate.rawFiducials.length == 0 || pose == null) {
      return new VisionFrame(result, Optional.empty());
    }
    var tagIds = Arrays.stream(poseEstimate.rawFiducials).map(t -> t.fiducialId).toList();
    double ambiguity = tagIds.size() == 1 ? poseEstimate.rawFiducials[0].ambiguity : 0;
    var estimate =
        VisionPoseEstimate.unfiltered(
            name,
            pose,
            poseEstimate.timestampSeconds,
            tagIds,
            poseEstimate.avgTagDistanceMeters,
            ambiguity,
            poseEstimate.isMT2());
    if (!useLimelightFilter) {
      return new VisionFrame(result, Optional.of(estimate));
    }
    return new VisionFrame(
        result,
        Optional.of(estimate),
        poseEstimate.isValid()
            ? Optional.empty()
            : Optional.of("Limelight rejected it (flags " + poseEstimate.rejectionFlags + ")"),
        Optional.of(poseEstimate.stdDevs));
  }

  @Override
  protected void applyPipelineIndex(int index) {
    limelight.setPipelineIndex(index);
  }

  @Override
  protected int readPipelineIndex() {
    return limelight.getCurrentPipelineIndex();
  }

  @Override
  protected boolean readConnected() {
    return limelight.isConnected();
  }

  @Override
  protected void applyRobotOrientation(
      double timestampSeconds, Rotation3d orientation, double yawRateDegreesPerSecond) {
    limelight.setRobotOrientation(
        Math.toDegrees(orientation.getZ()),
        Double.isNaN(yawRateDegreesPerSecond) ? 0 : yawRateDegreesPerSecond,
        Math.toDegrees(orientation.getY()),
        0,
        Math.toDegrees(orientation.getX()),
        0,
        true);
  }

  @Override
  protected void applyRobotToCamera(Transform3d robotToCamera) {
    limelight.setCameraPose_RobotSpaceOverride(
        new Pose3d(robotToCamera.getTranslation(), robotToCamera.getRotation()), true);
  }

  @Override
  public Limelight getCamera() {
    return limelight;
  }
}
