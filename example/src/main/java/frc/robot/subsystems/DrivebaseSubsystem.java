package frc.robot.subsystems;


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
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import java.util.Optional;
import limelight.Limelight;
import limelight.estimator.LimelightPoseEstimator;
import limelight.estimator.PoseEstimate;
import limelight.structures.LimelightSettings.LEDMode;

public class DrivebaseSubsystem extends SubsystemBase
{

  SparkMax left, right;
  RelativeEncoder leftEncoder, rightEncoder;
  AHRS navx;

  double                         driveGearRatio      = 1.0;
  double                         wheelDiameterMeters = 4.0;
  double                         trackWidth          = Units.inchesToMeters(20);
  DifferentialDrive              differentialDrive;
  DifferentialDrivePoseEstimator differentialDrivePoseEstimator;
  DifferentialDriveKinematics    differentialDriveKinematics;
  Pose3d                         cameraOffset        = new Pose3d(Inches.of(5).in(Meters),
                                                                  Inches.of(5).in(Meters),
                                                                  Inches.of(5).in(Meters),
                                                                  Rotation3d.kZero);
  Limelight                      limelight;
  LimelightPoseEstimator         poseEstimator;


  public DrivebaseSubsystem()
  {
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
             .withCameraOffset(cameraOffset);
    poseEstimator = limelight.getPoseEstimator(true);

  }

  /**
   * Get the wheel positions in Meters.
   *
   * @return {@link DifferentialDriveWheelPositions}
   */
  private DifferentialDriveWheelPositions getWheelPositions()
  {
    return new DifferentialDriveWheelPositions(leftEncoder.getPosition() * wheelDiameterMeters,
                                               rightEncoder.getPosition() * wheelDiameterMeters);
  }

  @Override
  public void periodic()
  {
    differentialDrivePoseEstimator.update(navx.getRotation2d(), getWheelPositions());
    // Get the vision estimate.
    Optional<PoseEstimate> visionEstimate = poseEstimator.getPoseEstimate(); // BotPose.BLUE_MEGATAG2.get(limelight);
    visionEstimate.ifPresent((PoseEstimate poseEstimate) -> {
      differentialDrivePoseEstimator.addVisionMeasurement(poseEstimate.pose.toPose2d(), poseEstimate.timestampSeconds);
    });
  }
}

