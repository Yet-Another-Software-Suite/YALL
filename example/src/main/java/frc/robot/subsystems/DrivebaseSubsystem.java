package frc.robot.subsystems;


import static edu.wpi.first.units.Units.DegreesPerSecond;
import static edu.wpi.first.units.Units.Inches;
import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.MetersPerSecond;

import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.studica.frc.AHRS;
import com.studica.frc.AHRS.NavXComType;
import edu.wpi.first.math.estimator.DifferentialDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.math.kinematics.DifferentialDriveWheelPositions;
import edu.wpi.first.math.kinematics.DifferentialDriveWheelSpeeds;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import limelight.Limelight;
import limelight.networktables.AngularVelocity3d;
import limelight.networktables.LimelightPoseEstimator;
import limelight.networktables.LimelightPoseEstimator.EstimationMode;
import limelight.networktables.LimelightResults;
import limelight.networktables.LimelightSettings.LEDMode;
import limelight.networktables.Orientation3d;
import limelight.networktables.PoseEstimate;
import limelight.networktables.target.pipeline.NeuralClassifier;
import limelight.sim.LimelightSim;
import yams.gearing.MechanismGearing;
import yams.motorcontrollers.SmartMotorController;
import yams.motorcontrollers.SmartMotorControllerConfig;
import yams.motorcontrollers.SmartMotorControllerConfig.ControlMode;
import yams.motorcontrollers.SmartMotorControllerConfig.MotorMode;
import yams.motorcontrollers.local.SparkWrapper;

public class DrivebaseSubsystem extends SubsystemBase
{

  private AHRS navx;

  private double                         driveGearRatio      = 1.0;
  private double                         wheelDiameterMeters = Inches.of(4).in(Meters);
  private double                         trackWidth          = Units.inchesToMeters(20);
  private DifferentialDrivePoseEstimator differentialDrivePoseEstimator;
  private DifferentialDriveKinematics    differentialDriveKinematics;
  private Pose3d                 cameraOffset = new Pose3d(Inches.of(5).in(Meters),
                                                           Inches.of(5).in(Meters),
                                                           Inches.of(5).in(Meters),
                                                           Rotation3d.kZero);
  private Limelight              limelight;
  private LimelightPoseEstimator poseEstimator;
  private LimelightSim           limelightSim;

  // Wrapping the SparkMax's with YAMS SmartMotorControllers gets simulated encoder feedback (fed by
  // simIterate() below) for free, so the odometry code beneath does not need to know it is running in
  // simulation at all.
  private SmartMotorController leftMotor;
  private SmartMotorController rightMotor;
  private DifferentialDrive    differentialDrive;

  // Ground truth pose, integrated directly from simulated wheel speeds every simulationPeriodic()
  // loop. Kept separate from the (gyro and encoder driven) odometry estimate above so the Field2d can
  // show how far vision corrected odometry drifts from the truth.
  private Pose2d  simulatedPose = Pose2d.kZero;
  private Field2d field2d       = new Field2d();
  private double  lastSimTimestampSeconds;

  public DrivebaseSubsystem()
  {
    navx = new AHRS(NavXComType.kMXP_SPI);

    MechanismGearing gearing = new MechanismGearing(driveGearRatio);
    SmartMotorControllerConfig leftConfig = new SmartMotorControllerConfig(this)
        .withControlMode(ControlMode.OPEN_LOOP)
        .withGearing(gearing)
        .withIdleMode(MotorMode.COAST)
        .withMotorInverted(false)
        .withWheelDiameter(Inches.of(4));
    leftMotor = new SparkWrapper(new SparkMax(1, MotorType.kBrushless), DCMotor.getNEO(1), leftConfig);

    SmartMotorControllerConfig rightConfig = new SmartMotorControllerConfig(this)
        .withControlMode(ControlMode.OPEN_LOOP)
        .withGearing(gearing)
        .withIdleMode(MotorMode.COAST)
        .withMotorInverted(true)
        .withWheelDiameter(Inches.of(4));
    rightMotor = new SparkWrapper(new SparkMax(2, MotorType.kBrushless), DCMotor.getNEO(1), rightConfig);

    // DifferentialDrive applies its own squaring/deadband to duty cycle inputs, so raw values from
    // drive() are safe to pass straight through.
    differentialDrive = new DifferentialDrive(leftMotor::setDutyCycle, rightMotor::setDutyCycle);

    // Create the pose estimator
    differentialDriveKinematics = new DifferentialDriveKinematics(trackWidth);
    differentialDrivePoseEstimator = new DifferentialDrivePoseEstimator(differentialDriveKinematics,
                                                                        getHeading(),
                                                                        0,
                                                                        0,
                                                                        Pose2d.kZero); // Starting at (0,0)

    limelight = new Limelight("limelight");
    limelight.getSettings()
             .withLimelightLEDMode(LEDMode.PipelineControl)
             .withCameraOffset(cameraOffset)
             .save();
    poseEstimator = limelight.createPoseEstimator(EstimationMode.MEGATAG2);

    limelightSim = new LimelightSim(limelight)
        .withRobotToCameraTransform(new Transform3d(cameraOffset.getTranslation(), cameraOffset.getRotation()));

    SmartDashboard.putData("Field", field2d);
    lastSimTimestampSeconds = Timer.getFPGATimestamp();
  }

  /**
   * Get the wheel positions in Meters.
   *
   * @return {@link DifferentialDriveWheelPositions}
   */
  private DifferentialDriveWheelPositions getWheelPositions()
  {
    return new DifferentialDriveWheelPositions(leftMotor.getMeasurementPosition().in(Meters),
                                               rightMotor.getMeasurementPosition().in(Meters));
  }

  /**
   * Get the robot's current heading. Reads the navX on real hardware, or the kinematically integrated {@link #simulatedPose}'s rotation in simulation, since the navX has no working desktop simulation
   * backend.
   *
   * @return {@link Rotation2d} heading.
   */
  private Rotation2d getHeading()
  {
    return RobotBase.isSimulation() ? simulatedPose.getRotation() : navx.getRotation2d();
  }

  /**
   * Get the robot's current 3d orientation. See {@link #getHeading()}.
   *
   * @return {@link Rotation3d} orientation.
   */
  private Rotation3d getHeading3d()
  {
    return RobotBase.isSimulation() ? new Rotation3d(simulatedPose.getRotation()) : navx.getRotation3d();
  }

  /**
   * Drive the robot.
   *
   * @param left  Left speed (-1,1)
   * @param right Right speed (-1, 1)
   * @return {@link Command} to drive the robot.
   */
  public Command drive(DoubleSupplier left, DoubleSupplier right)
  {
    return run(() -> {
      differentialDrive.tankDrive(left.getAsDouble() * 0.8, right.getAsDouble() * 0.8);
    });
  }

  @Override
  public void periodic()
  {
    differentialDrivePoseEstimator.update(getHeading(), getWheelPositions());

    // Required for megatag2
    limelight.getSettings()
             .withRobotOrientation(new Orientation3d(getHeading3d(),
                                                     new AngularVelocity3d(DegreesPerSecond.of(0),
                                                                           DegreesPerSecond.of(0),
                                                                           DegreesPerSecond.of(0))))
             .save();

    // Get the vision estimate.
    Optional<PoseEstimate> visionEstimate = poseEstimator.getPoseEstimate(); // BotPose.BLUE_MEGATAG2.get(limelight);
    visionEstimate.ifPresent((PoseEstimate poseEstimate) -> {
      // If the average tag distance is less than 4 meters,
      // there are more than 0 tags in view,
      // and the average ambiguity between tags is less than 30% then we update the pose estimation.
      if (poseEstimate.avgTagDist < 4 && poseEstimate.tagCount > 0 && poseEstimate.getMinTagAmbiguity() < 0.3)
      {
        differentialDrivePoseEstimator.addVisionMeasurement(poseEstimate.pose.toPose2d(),
                                                            poseEstimate.timestampSeconds);
      }
    });

    limelight.getLatestResults().ifPresent((LimelightResults result) -> {
      for (NeuralClassifier object : result.targets_Classifier)
      {
        // Classifier says its a note.
        if (object.className.equals("algae"))
        {
          if (object.ty > 2 && object.ty < 1)
          {
            // do stuff
          }
        }
      }
    });

    field2d.setRobotPose(differentialDrivePoseEstimator.getEstimatedPosition());
  }

  @Override
  public void simulationPeriodic()
  {
    double now = Timer.getFPGATimestamp();
    double dt  = now - lastSimTimestampSeconds;
    lastSimTimestampSeconds = now;

    leftMotor.simIterate();
    rightMotor.simIterate();

    // Integrate ground truth pose directly from the simulated wheel speeds, independent of the
    // gyro/encoder driven odometry above, so the Field2d shows real vision correction happening
    // instead of the two traces always sitting on top of each other.
    DifferentialDriveWheelSpeeds wheelSpeeds = new DifferentialDriveWheelSpeeds(
        leftMotor.getMeasurementVelocity().in(MetersPerSecond),
        rightMotor.getMeasurementVelocity().in(MetersPerSecond));
    var     chassisSpeeds = differentialDriveKinematics.toChassisSpeeds(wheelSpeeds);
    Twist2d twist         = new Twist2d(chassisSpeeds.vxMetersPerSecond * dt, 0, chassisSpeeds.omegaRadiansPerSecond * dt);
    simulatedPose = simulatedPose.exp(twist);

    limelightSim.update(simulatedPose);

    field2d.getObject("Simulation").setPose(simulatedPose);
  }
}
