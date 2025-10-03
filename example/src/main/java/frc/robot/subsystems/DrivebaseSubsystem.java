package frc.robot.subsystems;

import static edu.wpi.first.units.Units.DegreesPerSecond;
import static edu.wpi.first.units.Units.Inches;
import static edu.wpi.first.units.Units.Meters;

import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.studica.frc.AHRS;
import com.studica.frc.AHRS.NavXComType;
import edu.wpi.first.math.estimator.DifferentialDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.math.kinematics.DifferentialDriveWheelPositions;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import limelight.Limelight;
import limelight.LimelightModel;
import limelight.networktables.AngularVelocity3d;
import limelight.networktables.LimelightPoseEstimator;
import limelight.networktables.LimelightResults;
import limelight.networktables.LimelightSettings.LEDMode;
import limelight.networktables.Orientation3d;
import limelight.networktables.PoseEstimate;
import limelight.networktables.target.pipeline.NeuralClassifier;
import limelight.sim.LimelightSim;
import limelight.sim.LimelightPipelineSim;

public class DrivebaseSubsystem extends SubsystemBase {

  SparkMax left, right;
  RelativeEncoder leftEncoder, rightEncoder;
  AHRS navx;

  double driveGearRatio = 1.0;
  double wheelDiameterMeters = 4.0;
  double trackWidth = Units.inchesToMeters(20);
  DifferentialDrive differentialDrive;
  DifferentialDrivePoseEstimator differentialDrivePoseEstimator;
  DifferentialDriveKinematics differentialDriveKinematics;
  Pose3d cameraOffset = new Pose3d(Inches.of(5).in(Meters),
      Inches.of(5).in(Meters),
      Inches.of(5).in(Meters),
      Rotation3d.kZero);
  Limelight limelight;
  LimelightSim limelightSim;
  LimelightPoseEstimator poseEstimator;

  public DrivebaseSubsystem() {
    left = new SparkMax(1, MotorType.kBrushless);
    right = new SparkMax(2, MotorType.kBrushless);
    navx = new AHRS(NavXComType.kMXP_SPI);
    leftEncoder = left.getEncoder();
    rightEncoder = right.getEncoder();

    differentialDrive = new DifferentialDrive(left, right);

    // Create the configuration object.
    SparkMaxConfig sparkMaxConfig = new SparkMaxConfig();
    // Set the position conversion factor.
    sparkMaxConfig.encoder.positionConversionFactor(driveGearRatio);
    // Configure the SparkMax's
    left.configure(sparkMaxConfig, ResetMode.kNoResetSafeParameters, PersistMode.kPersistParameters);
    right.configure(sparkMaxConfig, ResetMode.kNoResetSafeParameters, PersistMode.kPersistParameters);

    // Create the pose estimator
    differentialDriveKinematics = new DifferentialDriveKinematics(trackWidth);
    differentialDrivePoseEstimator = new DifferentialDrivePoseEstimator(differentialDriveKinematics,
        navx.getRotation2d(),
        0,
        0,
        Pose2d.kZero); // Starting at (0,0)

    limelight = new Limelight("limelight");
    limelight.getSettings()
        .withLimelightLEDMode(LEDMode.PipelineControl)
        .withCameraOffset(cameraOffset)
        .save();

    limelightSim = new LimelightSim(limelight, LimelightModel.Limelight3G)
        .withCameraToRobotOffset(cameraOffset)
        .withDefaultPipeline(1).withPipelines(
            new LimelightPipelineSim("pipe_color"));

    poseEstimator = limelight.getPoseEstimator(true);

  }

  /**
   * Get the wheel positions in Meters.
   *
   * @return {@link DifferentialDriveWheelPositions}
   */
  private DifferentialDriveWheelPositions getWheelPositions() {
    return new DifferentialDriveWheelPositions(leftEncoder.getPosition() * wheelDiameterMeters,
        rightEncoder.getPosition() * wheelDiameterMeters);
  }

  /**
   * Drive the robot.
   *
   * @param left  Left speed (-1,1)
   * @param right Right speed (-1, 1)
   * @return {@link Command} to drive the robot.
   */
  public Command drive(DoubleSupplier left, DoubleSupplier right) {
    return run(() -> {
      differentialDrive.tankDrive(left.getAsDouble() * 0.8, right.getAsDouble() * 0.8);
    });
  }

  @Override
  public void periodic() {
    differentialDrivePoseEstimator.update(navx.getRotation2d(), getWheelPositions());

    // Required for megatag2
    limelight.getSettings()
        .withRobotOrientation(new Orientation3d(navx.getRotation3d(),
            new AngularVelocity3d(DegreesPerSecond.of(0),
                DegreesPerSecond.of(0),
                DegreesPerSecond.of(0))))
        .save();

    if (RobotBase.isSimulation()) {
      limelightSim.update(differentialDrivePoseEstimator.getEstimatedPosition());
    }

    // Get the vision estimate.
    Optional<PoseEstimate> visionEstimate = poseEstimator.getPoseEstimate(); // BotPose.BLUE_MEGATAG2.get(limelight);
    visionEstimate.ifPresent((PoseEstimate poseEstimate) -> {
      // If the average tag distance is less than 4 meters,
      // there are more than 0 tags in view,
      // and the average ambiguity between tags is less than 30% then we update the
      // pose estimation.
      if (poseEstimate.avgTagDist < 4 && poseEstimate.tagCount > 0 && poseEstimate.getMinTagAmbiguity() < 0.3) {
        differentialDrivePoseEstimator.addVisionMeasurement(poseEstimate.pose.toPose2d(),
            poseEstimate.timestampSeconds);
      }
    });

    limelight.getLatestResults().ifPresent((LimelightResults result) -> {
      for (NeuralClassifier object : result.targets_Classifier) {
        // Classifier says its a note.
        if (object.className.equals("algae")) {
          if (object.ty > 2 && object.ty < 1) {
            // do stuff
          }
        }
      }
    });
  }
}
