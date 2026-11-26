package first.robot;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.simulation.SimHooks;

/** Vision keeps the pose estimate on the true pose while odometry drifts. */
class RobotTest {

  private static double error(Pose2d estimate, Pose2d truth) {
    return estimate.getTranslation().getDistance(truth.getTranslation());
  }

  @Test
  void visionCorrectsOdometryDrift() {
    HAL.initialize();
    SimHooks.pauseTiming();
    try {
      var robot = new Robot();
      // Drive a slow arc for 8 seconds.
      for (int i = 0; i < 400; i++) {
        robot.drive(0.4, 0.15);
        SimHooks.stepTiming(0.02);
        robot.simulationPeriodic();
        robot.robotPeriodic();
      }
      double odometryError = error(robot.getOdometryPose(), robot.getTruePose());
      double estimateError = error(robot.getEstimatedPose(), robot.getTruePose());
      System.out.printf(
          "odometry error %.3f m, odometry + vision error %.3f m%n", odometryError, estimateError);
      assertTrue(odometryError > 0.05, "odometry should drift, but is off by " + odometryError);
      assertTrue(
          estimateError < odometryError / 2,
          "vision should halve the drift, but the estimate is off by " + estimateError);
    } finally {
      SimHooks.resumeTiming();
    }
  }
}
