package first.robot;

import static org.wpilib.units.Units.DegreesPerSecond;
import static org.wpilib.units.Units.Meters;

import com.limelightvision.Limelight;
import org.photonvision.PhotonCamera;
import org.wpilib.driverstation.Gamepad;
import org.wpilib.framework.TimedRobot;
import org.wpilib.math.estimator.DifferentialDrivePoseEstimator;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
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
                .withRobotToCamera(
                    new Transform3d(0.3, 0, 0.3, new Rotation3d(0, Math.toRadians(-10), 0))));
    var back =
        new LimelightWrapper(
            new Limelight("limelight-back"),
            new VisionCameraConfig()
                .withRobotToCamera(
                    new Transform3d(-0.3, 0, 0.3, new Rotation3d(0, Math.toRadians(-10), Math.PI)))
                .withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED));
    var side =
        new SynapseWrapper(
            new SynapseCamera("side"),
            new VisionCameraConfig()
                .withRobotToCamera(new Transform3d(0, 0.3, 0.3, new Rotation3d(0, 0, Math.PI / 2)))
                .withPoseFilter(new VisionPoseFilter().withMaxTagDistance(Meters.of(3), Meters.of(6))));
    vision = new VisionSystem(front, back, side);
  }

  @Override
  public void robotPeriodic() {
    var heading = drivetrainSim.getHeading();
    double left = drivetrainSim.getLeftPosition() * WHEEL_SLIP;
    double right = drivetrainSim.getRightPosition() * WHEEL_SLIP;
    poseEstimator.update(heading, left, right);
    odometryOnly.update(heading, left, right);

    // Heading-assisted cameras (the Limelight's MegaTag2) need the gyro every loop.
    vision.setRobotOrientation(
        new Rotation3d(heading), DegreesPerSecond.of(Math.toDegrees(gyroRateRadiansPerSecond())));
    vision.update();
    vision.addVisionMeasurements(poseEstimator);

    field.setRobotPose(poseEstimator.getEstimatedPosition());
    field.getObject("Odometry").setPose(odometryOnly.getEstimatedPosition());
    field.getObject("Truth").setPose(drivetrainSim.getPose());
    vision.getFusedPose().ifPresent(fused -> field.getObject("Fused vision").setPose(fused.pose2d()));
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
  }

  @Override
  public void simulationPeriodic() {
    drivetrainSim.update(0.02);
    // The cameras see the field from the true pose.
    vision.updateSimulation(drivetrainSim.getPose());
  }
}
