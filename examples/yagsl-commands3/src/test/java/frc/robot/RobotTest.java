package frc.robot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import frc.robot.opmodes.auto.DriveToPoseSquareAuto;
import frc.robot.opmodes.auto.LeaveStartingAreaAuto;
import frc.robot.opmodes.auto.OutAndBackAuto;
import frc.robot.opmodes.teleop.AngularVelocityTeleop;
import frc.robot.opmodes.teleop.DemoModeTeleop;
import frc.robot.opmodes.teleop.HeadingTeleop;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.simulation.SimHooks;
import yall.results.VisionPoseEstimate;

/**
 * Smoke test: builds the whole robot (YAGSL JSON parsing, the YAMS swerve drive, the YALL cameras and every opmode)
 * in simulation and runs the robot loop for two seconds, so a broken JSON config, vendordep, or API change fails the
 * build instead of the robot. The simulated cameras must see AprilTags and agree with where the simulated robot is.
 *
 * <p>Only one robot is built: a second one would claim the same simulated CAN IDs.
 */
class RobotTest
{

  @BeforeAll
  static void initializeHal()
  {
    HAL.initialize();
  }

  @Test
  void robotBuildsRunsAndSeesTags()
  {
    Robot robot = new Robot();
    assertNotNull(new AngularVelocityTeleop(robot));
    assertNotNull(new HeadingTeleop(robot));
    assertNotNull(new DemoModeTeleop(robot));
    assertNotNull(new LeaveStartingAreaAuto(robot));
    assertNotNull(new OutAndBackAuto(robot));
    assertNotNull(new DriveToPoseSquareAuto(robot));
    List<VisionPoseEstimate> estimates = new ArrayList<>();
    assertDoesNotThrow(() -> {
      for (int i = 0; i < 100; i++)
      {
        robot.robotPeriodic();
        robot.simulationPeriodic();
        estimates.addAll(robot.swerve.getVisionSystem().getEstimatedPoses());
        SimHooks.stepTiming(0.02);
      }
    });
    assertFalse(estimates.isEmpty(), "no camera saw a tag from the starting pose");
    // The robot is disabled here, so the Limelight 4 skips frames to stay cool.
    assertEquals(150, NetworkTableInstance.getDefault().getTable("limelight-back").getEntry("throttle_set").getDouble(-1));
    var truth = robot.swerve.getSimPose();
    for (VisionPoseEstimate estimate : estimates)
    {
      double error = estimate.pose2d().getTranslation().getDistance(truth.getTranslation());
      assertTrue(error < 0.3, estimate.cameraName() + " is " + error + " m from the simulated robot");
    }
    double drift = robot.swerve.getPose().getTranslation().getDistance(truth.getTranslation());
    assertTrue(drift < 0.3, "the pose estimate is " + drift + " m from the simulated robot");
  }
}
