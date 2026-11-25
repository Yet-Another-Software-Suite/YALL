package yall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.linalg.VecBuilder;
import yall.results.VisionPoseEstimate;

class VisionSystemFuseTest {

  private static VisionPoseEstimate estimate(double x, double y, double heading, double stdDev) {
    return new VisionPoseEstimate(
        "cam",
        new Pose3d(x, y, 0, new Rotation3d(0, 0, heading)),
        10,
        List.of(1),
        2,
        0,
        false,
        VecBuilder.fill(stdDev, stdDev, stdDev));
  }

  @Test
  void oneEstimateIsReturnedAsIs() {
    var e = estimate(1, 2, 0, 1);
    assertSame(e, VisionSystem.fuse(List.of(e)));
  }

  @Test
  void equalWeightsAverage() {
    var fused = VisionSystem.fuse(List.of(estimate(1, 1, 0, 1), estimate(3, 3, 0, 1)));
    assertEquals(2, fused.pose().getX(), 1e-9);
    assertEquals(2, fused.pose().getY(), 1e-9);
    assertEquals(1 / Math.sqrt(2), fused.stdDevs().get(0, 0), 1e-9);
  }

  @Test
  void trustedEstimatesCountForMore() {
    // std dev 1 vs 2: weights 1 and 1/4.
    var fused = VisionSystem.fuse(List.of(estimate(0, 0, 0, 1), estimate(5, 5, 0, 2)));
    assertEquals(5 * 0.25 / 1.25, fused.pose().getX(), 1e-9);
  }

  @Test
  void headingsAverageAcrossTheWrap() {
    var fused =
        VisionSystem.fuse(
            List.of(
                estimate(0, 0, Math.toRadians(179), 1), estimate(0, 0, Math.toRadians(-179), 1)));
    assertEquals(180, Math.abs(Math.toDegrees(fused.pose().getRotation().getZ())), 1e-6);
  }
}
