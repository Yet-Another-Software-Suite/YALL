package frc.robot.mechanisms;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Inches;
import static org.wpilib.units.Units.RadiansPerSecond;

import java.io.File;
import java.util.List;
import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.driverstation.RobotState;
import org.wpilib.math.controller.PIDController;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform2d;
import org.wpilib.math.kinematics.ChassisVelocities;
import org.wpilib.smartdashboard.Field2d;
import org.wpilib.system.Filesystem;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.AngularVelocity;
import org.wpilib.units.measure.Distance;
import com.limelightvision.Limelight;
import org.photonvision.PhotonCamera;
import yall.PoseEstimationMode;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.VisionSystem;
import yall.cameras.LimelightWrapper;
import yall.cameras.PhotonVisionWrapper;
import yall.sim.CameraModel;
import yall.telemetry.VisionTelemetryConfig;
import swervelib.commands3.SwerveParser;
import yams.commands3.config.SwerveDriveConfig;
import yams.commands3.swerve.SwerveDrive;
import yams.core.telemetry.SwerveDriveTelemetryConfig;
import yams.core.telemetry.enums.TelemetryVerbosity;

/**
 * Swerve drive built by YAGSL from the JSON configuration in {@code deploy/swerve/base}, as a Commands v3
 * {@link Mechanism}.
 */
public class SwerveDriveMechanism implements Mechanism
{

  /**
   * How close {@link #driveToPose(Pose2d)} has to get to its target, in translation, before it ends.
   */
  private static final Distance POSE_TRANSLATION_TOLERANCE = Inches.of(2);
  /**
   * How close {@link #driveToPose(Pose2d)} has to get to its target, in heading, before it ends.
   */
  private static final Angle    POSE_ROTATION_TOLERANCE    = Degrees.of(5);

  /**
   * Frames a Limelight 4 skips between processed frames while the robot is disabled. Limelight recommends 100 to 200
   * while disabled, and 0 while enabled, to keep the Limelight 4 from heating up in the queue and before a match.
   */
  private static final int    LIMELIGHT_DISABLED_THROTTLE        = 150;
  /**
   * How old the Limelight's newest frame can be before it counts as disconnected, while throttled. At 150 skipped
   * frames it processes one every few seconds.
   */
  private static final double LIMELIGHT_THROTTLED_STALE_SECONDS = 5;

  private final SwerveDrive  drive;
  private final Limelight    backLimelight = new Limelight("limelight-back");
  private final VisionSystem vision;
  /** Whether the back Limelight was last throttled, or null before the first loop. */
  private       Boolean      limelightThrottled;

  /**
   * Parse the YAGSL configuration and create the swerve drive.
   */
  public SwerveDriveMechanism()
  {
    var cfg = new SwerveDriveConfig()
        .withStartingPose(new Pose2d(3, 3, Rotation2d.ZERO))
        .withMechanism(this)
        .withTranslationController(new PIDController(4, 0, 0))
        // YAMS stops any module asked for less than 0.1 m/s (its default module velocity deadband), so a weak
        // rotation gain leaves a heading error it can never close. At kP = 3 that dead zone is under 4.5 degrees,
        // inside driveToPose's 5 degree tolerance.
        .withRotationController(new PIDController(3, 0, 0))
        .withTelemetry("swerve", new SwerveDriveTelemetryConfig(TelemetryVerbosity.HIGH).withDataLogName("swerve"));

    SwerveParser.parse(new File(Filesystem.getDeployDirectory(), "swerve/base"));
    drive = SwerveParser.createSwerveDrive(cfg);
    vision = createCameras();
    // Use SwerveParser.createSwerveDriveDevices(cfg) instead to also get the raw vendor devices (motor controllers,
    // encoders, and the gyro) YAGSL created.
  }

  /**
   * Get the underlying YAMS {@link SwerveDrive}.
   *
   * @return The swerve drive.
   */
  public SwerveDrive getDrive()
  {
    return drive;
  }

  /**
   * Drive the robot with the given field-relative {@link ChassisVelocities}.
   *
   * @param velocity Field-relative {@link ChassisVelocities} to apply.
   */
  public void drive(ChassisVelocities velocity)
  {
    drive.setFieldRelativeChassisSpeeds(velocity);
  }

  /**
   * Lock the swerve drive wheels in an X pattern so the robot is difficult to push.
   *
   * @return {@link Command} that locks the wheels in place until canceled.
   */
  public Command lockPose()
  {
    return run(coroutine -> {
      while (true)
      {
        drive.lockPose();
        coroutine.yield();
      }
    }).named("Lock Pose");
  }

  /**
   * Lock the swerve drive wheels in an X pattern so the robot is difficult to push.
   *
   * @return {@link Command} that locks the wheels in place until canceled.
   */
  public Command lock()
  {
    return lockPose();
  }

  /**
   * Drive to a series of poses relative to wherever the robot is when the command starts, one after another, with
   * {@link #driveToPose(Pose2d)}, then hold the last one. Because the poses are relative, the routine works from any
   * starting position and on either alliance.
   *
   * @param name      Name of the command.
   * @param waypoints Poses to drive to, each relative to the robot's pose when the command starts.
   * @return {@link Command} that drives to each pose in turn and holds the last one until it is canceled.
   */
  public Command driveRelativeWaypoints(String name, List<Transform2d> waypoints)
  {
    return run(coroutine -> {
      Pose2d start = getPose();
      for (int i = 0; i < waypoints.size() - 1; i++)
      {
        coroutine.await(driveToPose(start.transformBy(waypoints.get(i))));
      }
      // driveToPose ends as soon as the robot is within tolerance, even if it is still moving fast, so the last
      // waypoint is held instead. Otherwise the robot would coast past it once the routine ended.
      coroutine.await(drive.driveToPose(start.transformBy(waypoints.get(waypoints.size() - 1))));
    }).named(name);
  }

  /**
   * Drive straight to the given pose with YAMS' {@link SwerveDrive#driveToPose(Pose2d, Distance, Angle)}, using the
   * translation and rotation PID controllers from the {@link SwerveDriveConfig}. There is no path planning (Commands
   * v3 has no PathPlanner support), so nothing steers it around obstacles.
   *
   * @param pose {@link Pose2d} to drive to. Field relative, blue-origin where 0deg is facing towards RED alliance.
   * @return {@link Command} that drives to the pose, ending once it is within 2in and 5deg of it.
   */
  public Command driveToPose(Pose2d pose)
  {
    return drive.driveToPose(pose, POSE_TRANSLATION_TOLERANCE, POSE_ROTATION_TOLERANCE);
  }

  /**
   * Zero the gyro, resetting the robot's heading to face away from the driver station (0deg, red alliance side).
   *
   * @return {@link Command} that zeroes the gyro.
   */
  public Command zeroGyro()
  {
    return run(coroutine -> drive.zeroGyro()).named("Zero Gyro");
  }

  /**
   * Gets the measured pose (position and rotation) of the robot, as reported by odometry.
   *
   * @return The robot's pose.
   */
  public Pose2d getPose()
  {
    return drive.getPose();
  }

  /**
   * Get the {@link Field2d} used to display the robot's pose.
   *
   * @return {@link Field2d} of the drive.
   */
  public Field2d getField2d()
  {
    return drive.getField2d();
  }

  /**
   * Get the robot's heading on the field, as reported by odometry. Vision measurements correct it, field relative
   * driving uses it, and the cameras' MegaTag2 uses it.
   *
   * @return {@link Rotation2d} of the robot's heading.
   */
  public Rotation2d getHeading()
  {
    return drive.getRotation();
  }

  /**
   * Get how fast the robot is turning, counterclockwise positive. The cameras' pose filter rejects images taken while
   * the robot spins too fast to see the tags sharply.
   *
   * @return The robot's yaw rate.
   */
  public AngularVelocity getYawRate()
  {
    return RadiansPerSecond.of(drive.getRobotRelativeSpeed().omega);
  }


  /**
   * Get the simulated robot's true pose, for the simulated cameras.
   *
   * @return The simulated robot's pose.
   */
  public Pose2d getSimPose()
  {
    return drive.getSimPose();
  }

  /**
   * Get the gyro's 3D orientation.
   *
   * @return {@link Rotation3d} of the gyro.
   */
  public Rotation3d getGyroRotation3d()
  {
    return drive.getGyroRotation3d();
  }

  /**
   * Update odometry and telemetry, then correct the pose with the cameras. Call every loop; Commands v3 mechanisms
   * have no periodic() hook of their own.
   */
  public void periodic()
  {
    drive.updateTelemetry();
    updateVision();
  }

  /**
   * Step the swerve drive simulation and move the simulated cameras with it. Call every loop in simulation.
   */
  public void simulationPeriodic()
  {
    drive.simIterate();
    vision.updateSimulation(drive.getSimPose());
  }
  /**
   * Create the cameras that correct the drive's pose with AprilTags. The front camera runs PhotonVision. The back
   * camera is a Limelight 4 using MegaTag2, which also uses the robot's heading. In simulation both are simulated with
   * PhotonVision's simulator, so the example runs without a camera.
   *
   * @return The cameras, in a {@link VisionSystem}.
   */
  private VisionSystem createCameras()
  {
    // Each camera publishes to NetworkTables for dashboards, and logs the same values to the DataLog under
    // YALL/<camera name>.
    VisionTelemetryConfig telemetry = new VisionTelemetryConfig().withDataLog();

    // A PhotonVision camera 12 in forward of the robot's center, 10 in up, tilted 15 degrees up.
    VisionCamera front = new PhotonVisionWrapper(
        new PhotonCamera("front"),
        new VisionCameraConfig()
            .withCameraModel(CameraModel.ARDUCAM_OV9281)
            .withTelemetry(telemetry)
            .withRobotToCamera(Inches.of(12), Inches.zero(), Inches.of(10),
                               Degrees.zero(), Degrees.of(-15), Degrees.zero()));

    // A Limelight 4 12 in behind the robot's center, 10 in up, facing backward and tilted 15 degrees up. MegaTag2
    // (HEADING_ASSISTED) uses the heading given in updateVision().
    VisionCamera back = new LimelightWrapper(
        backLimelight,
        new VisionCameraConfig()
            .withCameraModel(CameraModel.LIMELIGHT_4)
            .withTelemetry(telemetry)
            .withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED)
            .withRobotToCamera(Inches.of(-12), Inches.zero(), Inches.of(10),
                               Degrees.zero(), Degrees.of(-15), Degrees.of(180)));

    return new VisionSystem(front, back);
  }

  /**
   * Read the cameras and add their pose estimates to the drive's pose estimator. Runs every loop, right after the
   * drive updates its odometry.
   */
  private void updateVision()
  {
    // Keep the Limelight 4 cool: skip most frames while disabled, process every frame while enabled. Only sent when
    // the robot is enabled or disabled, not every loop.
    boolean disabled = RobotState.isDisabled();
    if (limelightThrottled == null || limelightThrottled != disabled)
    {
      backLimelight.setThrottle(disabled ? LIMELIGHT_DISABLED_THROTTLE : 0);
      // So the throttled Limelight isn't reported as disconnected between its processed frames.
      backLimelight.withStaleFrameThreshold(disabled ? LIMELIGHT_THROTTLED_STALE_SECONDS
                                                     : Limelight.STALE_FRAME_SECONDS);
      limelightThrottled = disabled;
    }

    // The heading is the drive's odometry heading, not the gyro's: resetting odometry (for an auto's starting pose,
    // or with the zero gyro button) turns the odometry heading without turning the gyro.
    vision.setRobotOrientation(getHeading(), getYawRate());
    vision.update();
    // Every estimate that passed the pose filter, oldest first, each with its own timestamp and standard deviations.
    vision.forEachEstimatedPose(
        estimate -> drive.addVisionMeasurement(estimate.pose2d(), estimate.timestampSeconds(), estimate.stdDevs()));
  }

  /**
   * The cameras, for tests and dashboards.
   *
   * @return The {@link VisionSystem}.
   */
  public VisionSystem getVisionSystem()
  {
    return vision;
  }

}
