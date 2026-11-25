package yall.cameras;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.photonvision.EstimatedRobotPose;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import yall.PoseEstimationMode;
import yall.VisionFrame;
import yall.results.VisionPoseEstimate;
import yall.results.VisionResult;
import yall.results.VisionTarget;

/**
 * Turns PhotonVision results into YALL frames. {@link PhotonVisionWrapper} uses it on real robots,
 * and {@link yall.sim.SimulatedCamera} uses it for every simulated camera.
 */
public final class PhotonVisionFrames {

  private PhotonVisionFrames() {}

  /**
   * Reads a PhotonVision camera's unread results and turns them into frames.
   *
   * @param cameraName Name to give the frames.
   * @param camera The PhotonVision camera.
   * @param poseEstimator PhotonVision's pose estimator for the camera.
   * @param mode How to estimate the robot pose.
   * @return The frames, oldest first.
   */
  public static List<VisionFrame> read(
      String cameraName,
      PhotonCamera camera,
      PhotonPoseEstimator poseEstimator,
      PoseEstimationMode mode) {
    var frames = new ArrayList<VisionFrame>();
    for (PhotonPipelineResult result : camera.getAllUnreadResults()) {
      frames.add(
          new VisionFrame(
              toVisionResult(cameraName, result),
              estimatePose(cameraName, result, poseEstimator, mode)));
    }
    return frames;
  }

  /**
   * Turns one PhotonVision result into a YALL result.
   *
   * @param cameraName Name to give the result.
   * @param result The PhotonVision result.
   * @return The YALL result.
   */
  public static VisionResult toVisionResult(String cameraName, PhotonPipelineResult result) {
    var targets = new ArrayList<VisionTarget>(result.getTargets().size());
    for (PhotonTrackedTarget target : result.getTargets()) {
      boolean isTag = target.getFiducialId() >= 0;
      boolean isObject = target.getDetectedObjectClassID() >= 0;
      targets.add(
          new VisionTarget(
              target.getYaw(),
              target.getPitch(),
              target.getArea(),
              target.getFiducialId(),
              isObject ? target.getDetectedObjectClassID() : -1,
              // PhotonVision reports -1 for targets that aren't detected objects.
              isObject ? target.getDetectedObjectConfidence() : 0,
              isTag && target.getPoseAmbiguity() >= 0 ? target.getPoseAmbiguity() : Double.NaN,
              isTag ? Optional.of(target.getBestCameraToTarget()) : Optional.empty()));
    }
    return new VisionResult(
        cameraName, result.getTimestampSeconds(), result.metadata.getLatencyMillis(), targets);
  }

  /**
   * Estimates the robot pose from one PhotonVision result: the trig solve for {@link
   * PoseEstimationMode#HEADING_ASSISTED}, then the coprocessor's multi-tag result, then the
   * lowest-ambiguity tag.
   *
   * @param cameraName Name to give the estimate.
   * @param result The PhotonVision result.
   * @param poseEstimator PhotonVision's pose estimator for the camera.
   * @param mode How to estimate the robot pose.
   * @return The estimate, or empty if the result has no tags.
   */
  public static Optional<VisionPoseEstimate> estimatePose(
      String cameraName,
      PhotonPipelineResult result,
      PhotonPoseEstimator poseEstimator,
      PoseEstimationMode mode) {
    if (!result.hasTargets()) {
      return Optional.empty();
    }
    boolean headingAssisted = mode == PoseEstimationMode.HEADING_ASSISTED;
    Optional<EstimatedRobotPose> estimate = Optional.empty();
    if (headingAssisted) {
      estimate = poseEstimator.estimatePnpDistanceTrigSolvePose(result);
    }
    if (estimate.isEmpty()) {
      headingAssisted = false;
      estimate = poseEstimator.estimateCoprocMultiTagPose(result);
    }
    if (estimate.isEmpty()) {
      estimate = poseEstimator.estimateLowestAmbiguityPose(result);
    }
    if (estimate.isEmpty()) {
      return Optional.empty();
    }
    var used = estimate.get().targetsUsed.stream().filter(t -> t.getFiducialId() >= 0).toList();
    var tagIds = used.stream().map(PhotonTrackedTarget::getFiducialId).toList();
    double distance =
        used.stream()
            .mapToDouble(t -> t.getBestCameraToTarget().getTranslation().getNorm())
            .average()
            .orElse(Double.NaN);
    double ambiguity =
        used.size() == 1 && used.get(0).getPoseAmbiguity() >= 0
            ? used.get(0).getPoseAmbiguity()
            : used.size() == 1 ? Double.NaN : 0;
    return Optional.of(
        VisionPoseEstimate.unfiltered(
            cameraName,
            estimate.get().estimatedPose,
            estimate.get().timestampSeconds,
            tagIds,
            distance,
            ambiguity,
            headingAssisted));
  }
}
