package yall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation3d;
import yall.filter.PoseFilterStats;
import yall.results.VisionPoseEstimate;

class PoseFilterStatsTest {

  private static VisionPoseEstimate at(double x, double y, double headingDegrees) {
    return VisionPoseEstimate.unfiltered(
        "cam",
        new Pose3d(x, y, 0, new Rotation3d(0, 0, Math.toRadians(headingDegrees))),
        1,
        List.of(1),
        3,
        0.05,
        false);
  }

  @Test
  void countsAcceptedAndRejectedByReason() {
    var stats = new PoseFilterStats();
    stats.recordAccepted(at(1, 1, 0));
    stats.recordRejected("ambiguous");
    stats.recordRejected("ambiguous");
    stats.recordRejected("off the field");
    assertEquals(1, stats.getAcceptedCount());
    assertEquals(3, stats.getRejectedCount());
    assertEquals(2, stats.getRejectedCount("ambiguous"));
    assertEquals(0.25, stats.getAcceptanceRate(), 1e-9);
    stats.reset();
    assertEquals(0, stats.getRejectedCount("ambiguous"));
    assertTrue(Double.isNaN(stats.getAcceptanceRate()));
  }

  @Test
  void measuresTheSpreadOfAcceptedPoses() {
    var stats = new PoseFilterStats();
    stats.recordAccepted(at(1, 2, 179));
    stats.recordAccepted(at(3, 2, -179));
    // Sample standard deviation of {1, 3} is sqrt(2).
    assertEquals(Math.sqrt(2), stats.getStdDevs().get(0, 0), 1e-9);
    assertEquals(0, stats.getStdDevs().get(1, 0), 1e-9);
    // 179° and -179° are 2° apart, not 358°.
    assertEquals(Math.toRadians(Math.sqrt(2)), stats.getStdDevs().get(2, 0), 1e-9);
    assertEquals(2, stats.getMeanPose().orElseThrow().getX(), 1e-9);
    assertEquals(180, Math.abs(stats.getMeanPose().orElseThrow().getRotation().getDegrees()), 1e-9);
    // Measured at 3 m: base = measured / (1 + 9 / 30).
    assertEquals(Math.sqrt(2) / 1.3, stats.getSuggestedBaseStdDevs(30).get(0, 0), 1e-9);
  }
}
