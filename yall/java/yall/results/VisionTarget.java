package yall.results;

import java.util.Optional;
import org.wpilib.math.geometry.Transform3d;

/**
 * One target a camera sees, in the same form for every camera vendor.
 *
 * <p>Angles follow the convention PhotonVision and Limelight share: yaw is positive to the right of
 * the crosshair, pitch is positive above it.
 *
 * @param yawDegrees Horizontal angle from the crosshair to the target, positive right.
 * @param pitchDegrees Vertical angle from the crosshair to the target, positive up.
 * @param areaPercent Share of the image the target covers, 0 to 100. {@link Double#NaN} if the camera
 *     doesn't report it.
 * @param fiducialId AprilTag ID, or -1 if the target isn't an AprilTag.
 * @param classId Object detection class ID, or -1 if the target isn't a detected object.
 * @param confidence Object detection confidence, 0 to 1. 0 if the target isn't a detected object.
 * @param ambiguity AprilTag pose ambiguity, 0 to 1. {@link Double#NaN} if the camera doesn't report it.
 * @param cameraToTarget Transform from the camera to the target, if the camera solved it.
 */
public record VisionTarget(
    double yawDegrees,
    double pitchDegrees,
    double areaPercent,
    int fiducialId,
    int classId,
    double confidence,
    double ambiguity,
    Optional<Transform3d> cameraToTarget) {

  /**
   * Whether the target is an AprilTag.
   *
   * @return true if {@link #fiducialId()} is a tag ID.
   */
  public boolean isAprilTag() {
    return fiducialId >= 0;
  }

  /**
   * Whether the target came from object detection.
   *
   * @return true if {@link #classId()} is a class ID.
   */
  public boolean isDetectedObject() {
    return classId >= 0;
  }
}
