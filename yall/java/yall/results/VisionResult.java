package yall.results;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One processed camera frame, in the same form for every camera vendor.
 *
 * @param cameraName Name of the camera that produced the frame.
 * @param timestampSeconds When the image was captured, on the {@link
 *     org.wpilib.system.Timer#getMonotonicTimestamp()} timebase that WPILib's pose estimators use.
 * @param latencyMillis Time from capture to the result reaching the robot, in milliseconds.
 * @param targets Targets in the frame, best first.
 */
public record VisionResult(
    String cameraName, double timestampSeconds, double latencyMillis, List<VisionTarget> targets) {

  /** Copies the target list so the result can't change after it is made. */
  public VisionResult {
    targets = List.copyOf(targets);
  }

  /**
   * Whether the frame has any targets.
   *
   * @return true if {@link #targets()} isn't empty.
   */
  public boolean hasTargets() {
    return !targets.isEmpty();
  }

  /**
   * The best target in the frame, as ranked by the camera.
   *
   * @return The first target, or empty if there are none.
   */
  public Optional<VisionTarget> getBestTarget() {
    return targets.isEmpty() ? Optional.empty() : Optional.of(targets.get(0));
  }

  /**
   * The AprilTags in the frame.
   *
   * @return Targets with a fiducial ID.
   */
  public List<VisionTarget> getAprilTags() {
    if (targets.isEmpty()) {
      return List.of();
    }
    List<VisionTarget> matched = null;
    for (int i = 0; i < targets.size(); i++) {
      VisionTarget t = targets.get(i);
      if (t.isAprilTag()) {
        if (matched == null) {
          matched = new ArrayList<>(targets.size());
        }
        matched.add(t);
      }
    }
    return matched == null ? List.of() : List.copyOf(matched);
  }

  /**
   * The detected objects in the frame.
   *
   * @return Targets with an object detection class ID.
   */
  public List<VisionTarget> getDetectedObjects() {
    if (targets.isEmpty()) {
      return List.of();
    }
    List<VisionTarget> matched = null;
    for (int i = 0; i < targets.size(); i++) {
      VisionTarget t = targets.get(i);
      if (t.isDetectedObject()) {
        if (matched == null) {
          matched = new ArrayList<>(targets.size());
        }
        matched.add(t);
      }
    }
    return matched == null ? List.of() : List.copyOf(matched);
  }
}
