package yall;

import java.util.Optional;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import yall.results.VisionPoseEstimate;
import yall.results.VisionResult;

/**
 * One frame read from a camera, before the pose filter. Camera wrappers make these; robot code
 * reads {@link VisionResult}s and {@link VisionPoseEstimate}s instead.
 *
 * @param result The frame's targets.
 * @param estimate The robot pose estimated from the frame, if any.
 * @param vendorRejection Why the vendor's own filter rejected the estimate, if it did.
 * @param vendorStdDevs Standard deviations from the vendor, used instead of the filter's.
 */
public record VisionFrame(
    VisionResult result,
    Optional<VisionPoseEstimate> estimate,
    Optional<String> vendorRejection,
    Optional<Matrix<N3, N1>> vendorStdDevs) {

  /**
   * A frame with no vendor filtering.
   *
   * @param result The frame's targets.
   * @param estimate The robot pose estimated from the frame, if any.
   */
  public VisionFrame(VisionResult result, Optional<VisionPoseEstimate> estimate) {
    this(result, estimate, Optional.empty(), Optional.empty());
  }
}
