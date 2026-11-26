package first.robot;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.DegreesPerSecond;
import static org.wpilib.units.Units.Inches;
import static org.wpilib.units.Units.Meters;

import com.limelightvision.Limelight;
import org.photonvision.PhotonCamera;
import org.wpilib.driverstation.Gamepad;
import org.wpilib.framework.TimedRobot;
import org.wpilib.math.estimator.DifferentialDrivePoseEstimator;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.util.MathUtil;
import org.wpilib.simulation.DifferentialDrivetrainSim;
import org.wpilib.simulation.DifferentialDrivetrainSim.KitbotGearing;
import org.wpilib.simulation.DifferentialDrivetrainSim.KitbotMotor;
import org.wpilib.simulation.DifferentialDrivetrainSim.KitbotWheelSize;
import org.wpilib.smartdashboard.Field2d;
import org.wpilib.telemetry.Telemetry;
import synapse.SynapseCamera;
import yall.PoseEstimationMode;
import yall.VisionCameraConfig;
import yall.VisionSystem;
import yall.cameras.LimelightWrapper;
import yall.cameras.PhotonVisionWrapper;
import yall.cameras.SynapseWrapper;
import yall.filter.VisionPoseFilter;
import yall.sim.CameraModel;

/**
 * Drives a simulated kitbot around the field with three cameras, one from each vendor, correcting
 * its odometry.
 *
 * <p>The simulated wheels slip, so odometry alone drifts away from the true pose. Run the
 * simulation, drive with a gamepad, and compare the "Truth", "Odometry" and "Robot" (odometry plus
 * vision) poses on the Field widget.
 */
public class Robot extends TimedRobot {

  /** How much further the odometry thinks the wheels travelled than they did. */
  private static final double WHEEL_SLIP = 1.05;

  private final Gamepad gamepad = new Gamepad(0);

  private final DifferentialDrivetrainSim drivetrainSim =
      DifferentialDrivetrainSim.createKitbotSim(
          KitbotMotor.DUAL_CIM_PER_SIDE, KitbotGearing.RATIO_10P71, KitbotWheelSize.SIX_INCH, null);

  private final Pose2d startPose = new Pose2d(3, 4, Rotation2d.ZERO);

  private final DifferentialDrivePoseEstimator poseEstimator =
      new DifferentialDrivePoseEstimator(Rotation2d.ZERO, 0, 0, startPose);

  private final DifferentialDrivePoseEstimator odometryOnly =
      new DifferentialDrivePoseEstimator(Rotation2d.ZERO, 0, 0, startPose);

  /**
   * The Limelight 4 on the back. Limelight recommends skipping 100 to 200 frames between processed frames while
   * disabled, and none while enabled, to keep the Limelight 4 from heating up in the queue and before a match.
   */
  private final Limelight backLimelight = new Limelight("limelight-back");

  private final VisionSystem vision;

  private final Field2d field = new Field2d();

  /** Creates the cameras. */
  public Robot() {
    drivetrainSim.setPose(startPose);

    // Every camera shares the pose filter's defaults, with its own tweaks.
    var front =
        new PhotonVisionWrapper(
            new PhotonCamera("front"),
            new VisionCameraConfig()
                .withCameraModel(CameraModel.LUMA_P1)
                // 12" forward, centered, 12" up, tilted 10° up.
                .withRobotToCamera(
                    Inches.of(12),
                    Inches.zero(),
                    Inches.of(12),
                    Degrees.zero(),
                    Degrees.of(-10),
                    Degrees.zero()));
    var back =
        new LimelightWrapper(
            backLimelight,
            new VisionCameraConfig()
                .withCameraModel(CameraModel.LIMELIGHT_4)
                // 12" back, facing backward.
                .withRobotToCamera(
                    Inches.of(-12),
                    Inches.zero(),
                    Inches.of(12),
                    Degrees.zero(),
                    Degrees.of(-10),
                    Degrees.of(180))
                .withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED));
    var side =
        new SynapseWrapper(
            new SynapseCamera("side"),
            new VisionCameraConfig()
                .withCameraModel(CameraModel.ARDUCAM_OV9281)
                // 12" left, facing left.
                .withRobotToCamera(
                    Inches.zero(),
                    Inches.of(12),
                    Inches.of(12),
                    Degrees.zero(),
                    Degrees.zero(),
                    Degrees.of(90))
                .withPoseFilter(
                    new VisionPoseFilter().withMaxTagDistance(Meters.of(3), Meters.of(6))));
    vision = new VisionSystem(front, back, side);
  }

  @Override
  public void robotPeriodic() {
    var heading = drivetrainSim.getHeading();
    double left = drivetrainSim.getLeftPosition() * WHEEL_SLIP;
    double right = drivetrainSim.getRightPosition() * WHEEL_SLIP;
    poseEstimator.update(heading, left, right);
    odometryOnly.update(heading, left, right);

    // Heading-assisted cameras (the Limelight's MegaTag2) need the robot's heading on the field,
    // the pose estimator's, every loop.
    vision.setRobotOrientation(
        poseEstimator.getEstimatedPosition().getRotation(),
        DegreesPerSecond.of(Math.toDegrees(gyroRateRadiansPerSecond())));
    vision.update();
    vision.addVisionMeasurements(poseEstimator);

    field.setRobotPose(poseEstimator.getEstimatedPosition());
    field.getObject("Odometry").setPose(odometryOnly.getEstimatedPosition());
    field.getObject("Truth").setPose(drivetrainSim.getPose());
    Telemetry.log("Field", field);
  }

  private double gyroRateRadiansPerSecond() {
    return (drivetrainSim.getRightVelocity() - drivetrainSim.getLeftVelocity()) / 0.6;
  }

  @Override
  public void teleopPeriodic() {
    drive(
        -MathUtil.applyDeadband(gamepad.getLeftY(), 0.1),
        -MathUtil.applyDeadband(gamepad.getRightX(), 0.1));
  }

  /**
   * Drives the simulated robot.
   *
   * @param forward Forward speed, -1 to 1.
   * @param turn Counterclockwise turn speed, -1 to 1.
   */
  void drive(double forward, double turn) {
    drivetrainSim.setInputs((forward - turn) * 12 * 0.6, (forward + turn) * 12 * 0.6);
  }

  /**
   * The pose from odometry and vision.
   *
   * @return Estimated pose.
   */
  Pose2d getEstimatedPose() {
    return poseEstimator.getEstimatedPosition();
  }

  /**
   * The pose from odometry alone.
   *
   * @return Odometry pose.
   */
  Pose2d getOdometryPose() {
    return odometryOnly.getEstimatedPosition();
  }

  /**
   * The simulated robot's true pose.
   *
   * @return True pose.
   */
  Pose2d getTruePose() {
    return drivetrainSim.getPose();
  }

  @Override
  public void disabledInit() {
    drivetrainSim.setInputs(0, 0);
    // Keep the Limelight 4 cool while disabled. It then processes a frame every few seconds, so allow older frames
    // before it counts as disconnected.
    backLimelight.setThrottle(150);
    backLimelight.withStaleFrameThreshold(5);
  }

  @Override
  public void disabledExit() {
    // Process every frame while enabled.
    backLimelight.setThrottle(0);
    backLimelight.withStaleFrameThreshold(Limelight.STALE_FRAME_SECONDS);
  }

  @Override
  public void simulationPeriodic() {
    drivetrainSim.update(0.02);
    // The cameras see the field from the true pose.
    vision.updateSimulation(drivetrainSim.getPose());
  }
}
