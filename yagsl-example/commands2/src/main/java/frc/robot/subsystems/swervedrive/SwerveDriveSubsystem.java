package frc.robot.subsystems.swervedrive;


import static org.wpilib.units.Units.DegreesPerSecond;
import static org.wpilib.units.Units.DegreesPerSecondPerSecond;
import static org.wpilib.units.Units.MetersPerSecond;
import static org.wpilib.units.Units.MetersPerSecondPerSecond;
import static org.wpilib.units.Units.RadiansPerSecond;
import static org.wpilib.units.Units.Second;
import static org.wpilib.units.Units.Seconds;
import static org.wpilib.units.Units.Volts;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import com.pathplanner.lib.path.PathConstraints;
import java.io.File;
import java.io.IOException;
import java.util.function.DoubleSupplier;
import org.json.simple.parser.ParseException;
import org.wpilib.command2.Command;
import org.wpilib.command2.Commands;
import org.wpilib.command2.SubsystemBase;
import org.wpilib.command2.sysid.SysIdRoutine;
import org.wpilib.driverstation.Alliance;
import org.wpilib.driverstation.MatchState;
import org.wpilib.math.controller.PIDController;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import org.wpilib.smartdashboard.Field2d;
import org.wpilib.system.Filesystem;
import org.wpilib.units.measure.AngularVelocity;
import swervelib.commands2.SwerveParser;
import yams.commands2.config.SwerveDriveConfig;
import yams.commands2.swerve.SwerveDrive;
import yams.commands2.swerve.SwerveInputStream;
import yams.core.mechanisms.swerve.SwerveModule;
import yams.core.motorcontrollers.SmartMotorController;
import yams.core.telemetry.SwerveDriveTelemetryConfig;
import yams.core.telemetry.enums.TelemetryVerbosity;

public class SwerveDriveSubsystem extends SubsystemBase
{

  /**
   * Constraints used when pathfinding to a point with PathPlanner, matching the defaults configured in the
   * PathPlanner GUI settings.
   */
  private static final PathConstraints PATHFINDING_CONSTRAINTS = new PathConstraints(
      MetersPerSecond.of(3.0), MetersPerSecondPerSecond.of(3.0),
      DegreesPerSecond.of(540), DegreesPerSecondPerSecond.of(720));

  private final SwerveDrive drive;


  public SwerveDriveSubsystem()
  {
    var cfg = new SwerveDriveConfig()
        .withStartingPose(new Pose2d(3, 3, Rotation2d.ZERO))
        .withSubsystem(this)
        .withTranslationController(new PIDController(4, 0, 0))
        // YAMS stops any module asked for less than 0.1 m/s (its default module velocity deadband), so a weak
        // rotation gain leaves heading control and driveToPoseYAMS short of their target. At kP = 3 that dead zone
        // is under 4.5 degrees.
        .withRotationController(new PIDController(3, 0, 0))
        .withTelemetry("swerve", new SwerveDriveTelemetryConfig(TelemetryVerbosity.HIGH));

    SwerveParser.parse(new File(Filesystem.getDeployDirectory(), "swerve/base"));
    drive = SwerveParser.createSwerveDrive(cfg);

    configurePathPlanner();
  }

  /**
   * Configure {@link AutoBuilder} to {@link SwerveDrive} so that {@link Command}s built from PathPlanner paths and
   * autos can drive this subsystem.
   */
  private void configurePathPlanner()
  {
    RobotConfig config;
    try
    {
      config = RobotConfig.fromGUISettings();
    } catch (IOException | ParseException e)
    {
      throw new RuntimeException("Failed to load PathPlanner GUI settings", e);
    }

    AutoBuilder.configure(
        drive::getPose,
        drive::resetOdometry,
        drive::getRobotRelativeSpeed,
        (speeds, feedforwards) -> drive.setRobotRelativeChassisSpeeds(speeds, feedforwards.linearForces()),
        new PPHolonomicDriveController(new PIDConstants(5.0, 0.0, 0.0), new PIDConstants(5.0, 0.0, 0.0)),
        config,
        () -> MatchState.getAlliance().orElse(Alliance.BLUE) == Alliance.RED,
        this
    );
  }

  /**
   * Drive to the given pose with PathPlanner's on-the-fly pathfinding, which plans a path around the field's obstacles
   * (from {@code deploy/pathplanner/navgrid.json}) and follows it with the {@link PPHolonomicDriveController}
   * configured in {@link #configurePathPlanner()}. The robot comes to a stop at the pose.
   *
   * @param pose {@link Pose2d} to drive to. Field relative, blue-origin where 0deg is facing towards RED alliance.
   * @return {@link Command} that pathfinds to the pose, ending when it arrives.
   */
  public Command driveToPose(Pose2d pose)
  {
    return AutoBuilder.pathfindToPose(pose, PATHFINDING_CONSTRAINTS, MetersPerSecond.of(0));
  }

  /**
   * Drive straight to the given pose with YAMS' {@link SwerveDrive#driveToPose(Pose2d)}, using the translation and
   * rotation PID controllers from the {@link SwerveDriveConfig}. No path planning, so nothing steers it around
   * obstacles; it runs until canceled.
   *
   * @param pose {@link Pose2d} to drive to. Field relative, blue-origin where 0deg is facing towards RED alliance.
   * @return {@link Command} that drives to the pose.
   */
  public Command driveToPoseYAMS(Pose2d pose)
  {
    return drive.driveToPose(pose);
  }

  /**
   * Zero the gyro, resetting the robot's heading to face away from the driver station (0deg, red alliance side).
   *
   * @return {@link Command} that zeroes the gyro.
   */
  public Command zeroGyro()
  {
    return Commands.runOnce(drive::zeroGyro, this).withName("Zero Gyro");
  }

  public SwerveInputStream getAngularVelocityStream(DoubleSupplier x, DoubleSupplier y, DoubleSupplier rot)
  {
    return SwerveInputStream.of(drive, x, y, rot);
  }

  /**
   * Get the robot's heading on the field, as reported by odometry. Vision measurements correct it, and the cameras'
   * MegaTag2 uses it.
   *
   * @return {@link Rotation2d} of the robot's heading.
   */
  public Rotation2d getHeading()
  {
    return drive.getPose().getRotation();
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
   * Gets the measured pose (position and rotation) of the robot, as reported by odometry.
   *
   * @return The robot's pose.
   */
  public Pose2d getPose()
  {
    return drive.getPose();
  }

  /**
   * Reset the pose estimator (and, in simulation, the ground truth pose) to the given pose.
   *
   * @param pose Pose to reset to. Field relative, blue-origin.
   */
  public void resetOdometry(Pose2d pose)
  {
    drive.resetOdometry(pose);
  }

  /**
   * Get the {@link Field2d} used to display the robot's pose, so callers (e.g. vision subsystems) can publish
   * additional {@link org.wpilib.smartdashboard.FieldObject2d}s onto the same field widget instead of
   * creating their own.
   *
   * @return {@link Field2d} of the drive.
   */
  public Field2d getField2d()
  {
    return drive.getField2d();
  }

  /**
   * Add a camera's pose estimate, from {@link frc.robot.subsystems.vision.Vision}, to the drive's pose estimator.
   *
   * @param visionPose       Vision-measured {@link Pose2d}, field relative, blue-origin.
   * @param timestampSeconds When the camera's image was taken.
   * @param stdDevs          Standard deviations of x (meters), y (meters) and heading (radians): how much to trust it.
   */
  public void addVisionMeasurement(Pose2d visionPose, double timestampSeconds, Matrix<N3, N1> stdDevs)
  {
    drive.addVisionMeasurement(visionPose, timestampSeconds, stdDevs);
  }

  /**
   * Drive with a field relative {@link SwerveInputStream}. YAMS turns the field relative velocities into module
   * states.
   *
   * @param stream Field relative {@link SwerveInputStream}.
   * @return {@link Command} that drives with the stream.
   */
  public Command driveFieldRelative(SwerveInputStream stream)
  {
    return run(() -> drive.setFieldRelativeChassisSpeeds(stream.get())).withName("Drive Field Relative");
  }

  /**
   * Create a {@link Command} that runs a full SysId characterization routine (quasistatic and dynamic, forward and
   * reverse) on a single swerve module's drive motor. The module's azimuth is held pointed straight ahead for the
   * duration of the test so only the drive motor is characterized.
   *
   * @param moduleName Name of the module to test, e.g. "frontleft", "frontright", "backleft", or "backright".
   * @return {@link Command} that runs the full SysId routine on the given module.
   */
  public Command sysIdModule(String moduleName)
  {

    SwerveModule         module       = drive.getModule(moduleName).orElseThrow();
    SmartMotorController driveMotor   = module.getDriveMotorController();
    SmartMotorController azimuthMotor = module.getAzimuthMotorController();

    SysIdRoutine routine = new SysIdRoutine(
        new SysIdRoutine.Config(Volts.of(1).per(Second), Volts.of(7), Seconds.of(10)),
        new SysIdRoutine.Mechanism(
            azimuthMotor::setVoltage,
            log -> log.motor(moduleName + "-azimuth")
                      .voltage(azimuthMotor.getVoltage())
                      .angularPosition(azimuthMotor.getMechanismPosition())
                      .angularVelocity(azimuthMotor.getMechanismVelocity()),
            this,
            moduleName + "-azimuth"
        )
    );

    return Commands.runOnce(() -> azimuthMotor.setPosition(Rotation2d.ZERO.getMeasure()))
                   .andThen(routine.quasistatic(SysIdRoutine.Direction.FORWARD))
                   .andThen(Commands.waitSeconds(1))
                   .andThen(routine.quasistatic(SysIdRoutine.Direction.REVERSE))
                   .andThen(Commands.waitSeconds(1))
                   .andThen(routine.dynamic(SysIdRoutine.Direction.FORWARD))
                   .andThen(Commands.waitSeconds(1))
                   .andThen(routine.dynamic(SysIdRoutine.Direction.REVERSE))
                   .withName("SysId " + moduleName + " Azimuth");
  }

  @Override
  public void periodic()
  {
    drive.updateTelemetry();
  }

  @Override
  public void simulationPeriodic()
  {
    drive.simIterate();
  }

  public Pose2d getSimPose()
  {
    return drive.getSimPose();
  }
}
