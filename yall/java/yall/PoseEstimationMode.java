package yall;

/** How a camera turns the AprilTags it sees into a robot pose. */
public enum PoseEstimationMode {
  /**
   * Solve the robot pose from the tags alone: Limelight MegaTag1, PhotonVision's multi-tag result
   * with a lowest-ambiguity fallback, and Synapse's field-space camera pose.
   */
  TAGS_ONLY,
  /**
   * Use the robot heading from {@link VisionCamera#setRobotOrientation} to steady the estimate:
   * Limelight MegaTag2 and PhotonVision's PnP distance trig solve. Synapse doesn't support this and
   * falls back to {@link #TAGS_ONLY}. Call {@link VisionCamera#setRobotOrientation} every loop.
   */
  HEADING_ASSISTED
}
