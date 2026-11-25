package yall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.wpilib.math.estimator.DifferentialDrivePoseEstimator;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.linalg.VecBuilder;
import yall.results.VisionPoseEstimate;

class VisionSystemFuseTest {

  private static VisionPoseEstimate estimate(
      double x, double y, double heading, double stdDev, double timestamp) {
    return new VisionPoseEstimate(
        "cam",
        new Pose3d(x, y, 0, new Rotation3d(0, 0, heading)),
        timestamp,
        List.of(1),
        2,
        0,
        false,
        VecBuilder.fill(stdDev, stdDev, stdDev));
  }

  @Test
  void oneEstimateIsReturnedAsIs() {
    var e = estimate(1, 2, 0, 1, 10);
    assertSame(e, VisionSystem.fuse(List.of(e)));
  }

  @Test
  void matchesAWpilibPoseEstimator() {
    var stateStdDevs = VecBuilder.fill(0.2, 0.3, 0.1);
    var first = estimate(2, 3, Math.toRadians(20), 0.5, 10);
    var second = estimate(2.4, 2.7, Math.toRadians(35), 0.8, 10.02);

    // A pose estimator whose robot hasn't moved, starting at the first estimate, then given the
    // second as a vision measurement.
    var heading = Rotation2d.fromRadians(Math.toRadians(20));
    var estimator =
        new DifferentialDrivePoseEstimator(
            heading, 0, 0, first.pose2d(), stateStdDevs, VecBuilder.fill(1, 1, 1));
    estimator.updateWithTime(10.0, heading, 0, 0);
    estimator.updateWithTime(10.02, heading, 0, 0);
    estimator.addVisionMeasurement(second.pose2d(), 10.02, second.stdDevs());
    var expected = estimator.getEstimatedPosition();

    var fused = VisionSystem.fuse(List.of(second, first), stateStdDevs).pose2d();
    assertEquals(expected.getX(), fused.getX(), 1e-9);
    assertEquals(expected.getY(), fused.getY(), 1e-9);
    assertEquals(expected.getRotation().getRadians(), fused.getRotation().getRadians(), 1e-9);
  }

  @Test
  void trustedEstimatesMoveTheFusedPoseFurther() {
    var sure = VisionSystem.fuse(List.of(estimate(0, 0, 0, 1, 10), estimate(5, 0, 0, 0.05, 11)));
    var unsure = VisionSystem.fuse(List.of(estimate(0, 0, 0, 1, 10), estimate(5, 0, 0, 2, 11)));
    assertTrue(sure.pose().getX() > unsure.pose().getX());
    assertTrue(sure.pose().getX() < 5);
  }

  @Test
  void stdDevsCombineLikeIndependentMeasurements() {
    var fused = VisionSystem.fuse(List.of(estimate(1, 1, 0, 1, 10), estimate(3, 3, 0, 1, 11)));
    assertEquals(1 / Math.sqrt(2), fused.stdDevs().get(0, 0), 1e-9);
    assertEquals(11, fused.timestampSeconds());
  }

  @Test
  void headingsFuseAcrossTheWrap() {
    var fused =
        VisionSystem.fuse(
            List.of(
                estimate(0, 0, Math.toRadians(179), 1, 10),
                estimate(0, 0, Math.toRadians(-179), 1, 11)),
            VecBuilder.fill(1, 1, 1));
    // Equal trust moves halfway: through 180°, not back through 0°.
    assertEquals(180, Math.abs(Math.toDegrees(fused.pose().getRotation().getZ())), 1e-6);
  }
}
