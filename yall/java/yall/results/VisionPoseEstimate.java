package yall.results;

import java.util.List;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.linalg.VecBuilder;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;

/**
 * A robot pose estimated from AprilTags, ready for a WPILib pose estimator.
 *
 * @param cameraName Name of the camera that produced the estimate, or "fused" for a {@link
 *     yall.VisionSystem#getFusedPose() fused} one.
 * @param pose Estimated robot pose on the field, in WPILib blue-origin field coordinates.
 * @param timestampSeconds When the image was captured, on the {@link
 *     org.wpilib.system.Timer#getMonotonicTimestamp()} timebase.
 * @param tagIds IDs of the AprilTags used.
 * @param averageTagDistanceMeters Average distance from the camera to the tags used.
 * @param ambiguity Pose ambiguity of a single-tag estimate, 0 to 1. 0 for multi-tag estimates, and
 *     {@link Double#NaN} if the camera doesn't report it.
 * @param headingAssisted Whether the estimate used the robot heading from {@link
 *     yall.VisionCamera#setRobotOrientation}, so its rotation is the gyro's rather than the camera's.
 * @param stdDevs Standard deviations of x (meters), y (meters) and heading (radians) for {@link
 *     org.wpilib.math.estimator.PoseEstimator#addVisionMeasurement(Pose2d, double, Matrix)}. Filled
 *     in by the {@link yall.filter.VisionPoseFilter}.
 */
public record VisionPoseEstimate(
    String cameraName,
    Pose3d pose,
    double timestampSeconds,
    List<Integer> tagIds,
    double averageTagDistanceMeters,
    double ambiguity,
    boolean headingAssisted,
    Matrix<N3, N1> stdDevs) {

  /** Copies the tag list so the estimate can't change after it is made. */
  public VisionPoseEstimate {
    tagIds = List.copyOf(tagIds);
  }

  /**
   * Creates an estimate whose standard deviations the filter hasn't filled in yet.
   *
   * @param cameraName Name of the camera.
   * @param pose Estimated robot pose.
   * @param timestampSeconds Capture timestamp.
   * @param tagIds IDs of the AprilTags used.
   * @param averageTagDistanceMeters Average distance to the tags used.
   * @param ambiguity Single-tag ambiguity, 0 for multi-tag, NaN if unknown.
   * @param headingAssisted Whether the robot heading was used.
   * @return The estimate, with zero standard deviations.
   */
  public static VisionPoseEstimate unfiltered(
      String cameraName,
      Pose3d pose,
      double timestampSeconds,
      List<Integer> tagIds,
      double averageTagDistanceMeters,
      double ambiguity,
      boolean headingAssisted) {
    return new VisionPoseEstimate(
        cameraName,
        pose,
        timestampSeconds,
        tagIds,
        averageTagDistanceMeters,
        ambiguity,
        headingAssisted,
        VecBuilder.fill(0, 0, 0));
  }

  /**
   * Number of AprilTags used.
   *
   * @return Tag count.
   */
  public int tagCount() {
    return tagIds.size();
  }

  /**
   * The estimated pose on the floor.
   *
   * @return {@link #pose()} as a {@link Pose2d}.
   */
  public Pose2d pose2d() {
    return pose.toPose2d();
  }

  /**
   * A copy of this estimate with other standard deviations.
   *
   * @param stdDevs Standard deviations of x, y and heading.
   * @return The copy.
   */
  public VisionPoseEstimate withStdDevs(Matrix<N3, N1> stdDevs) {
    return new VisionPoseEstimate(
        cameraName,
        pose,
        timestampSeconds,
        tagIds,
        averageTagDistanceMeters,
        ambiguity,
        headingAssisted,
        stdDevs);
  }
}
