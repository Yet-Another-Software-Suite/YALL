// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import limelight.Limelight;
import limelight.networktables.AngularVelocity3d;
import limelight.networktables.LimelightPoseEstimator;
import limelight.networktables.LimelightResults;
import limelight.networktables.Orientation3d;
import limelight.networktables.PoseEstimate;
import limelight.networktables.LimelightPoseEstimator.EstimationMode;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;


import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import swervelib.parser.SwerveParser;
import swervelib.SwerveDrive;
import swervelib.SwerveInputStream;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.util.struct.parser.ParseException;

import static edu.wpi.first.units.Units.Degrees;
import static edu.wpi.first.units.Units.DegreesPerSecond;
import static edu.wpi.first.units.Units.Meter;
import static edu.wpi.first.units.Units.RadiansPerSecond;
import static edu.wpi.first.units.Units.Second;

public class SwerveSubsystem extends SubsystemBase {
  /** Creates a new ExampleSubsystem. */

  
  File directory = new File(Filesystem.getDeployDirectory(),"swerve");
   static SwerveDrive             swerveDrive;
    Limelight               limelight;  
    LimelightPoseEstimator  limelightPoseEstimator;
    
    
  
    public SwerveSubsystem() {
  //error catching
   try
      {
        swerveDrive = new SwerveParser(directory).createSwerveDrive(4.5,//meters per second
                                                                    new Pose2d(new Translation2d(Meter.of(1),
                                                                                                  Meter.of(4)),
                                                                                        Rotation2d.fromDegrees(0)));
        // Alternative method if you don't want to supply the conversion factor via JSON files.
        // swerveDrive = new SwerveParser(directory).createSwerveDrive(maximumSpeed, angleConversionFactor, driveConversionFactor);
      } catch (Exception e)
      {
        throw new RuntimeException(e);
      }
      
      setupLimelight();
    }
  
    public void setupLimelight(){
      swerveDrive.stopOdometryThread();
      limelight.getSettings()
               .withPipelineIndex(0)
               .withCameraOffset(new Pose3d(Units.inchesToMeters(12),
                                            Units.inchesToMeters(12),
                                            Units.inchesToMeters(10.5),
                                            new Rotation3d(0, 0, Units.degreesToRadians(45))))
               .withAprilTagIdFilter(List.of(17, 18, 19, 20, 21, 22, 6, 7, 8, 9, 10, 11))
               .save();
      limelightPoseEstimator = limelight.createPoseEstimator(EstimationMode.MEGATAG2);
    }

  
    private int     outofAreaReading = 0;
    private boolean initialReading = false;
    @Override
    public void periodic() {
      // This method will be called once per scheduler run
      limelight.getSettings()
               .withRobotOrientation(new Orientation3d(new Rotation3d(swerveDrive.getOdometryHeading()
                                                                                 .rotateBy(Rotation2d.kZero)),
                                                       new AngularVelocity3d(DegreesPerSecond.of(0),
                                                                             DegreesPerSecond.of(0),
                                                                             DegreesPerSecond.of(0))))
               .save();
      Optional<PoseEstimate>     poseEstimates = limelightPoseEstimator.getPoseEstimate();
      Optional<LimelightResults> results       = limelight.getLatestResults();
      if (results.isPresent()/* && poseEstimates.isPresent()*/)
      {
        LimelightResults result       = results.get();
        PoseEstimate     poseEstimate = poseEstimates.get();
        SmartDashboard.putNumber("Avg Tag Ambiguity", poseEstimate.getAvgTagAmbiguity());
        SmartDashboard.putNumber("Min Tag Ambiguity", poseEstimate.getMinTagAmbiguity());
        SmartDashboard.putNumber("Max Tag Ambiguity", poseEstimate.getMaxTagAmbiguity());
        SmartDashboard.putNumber("Avg Distance", poseEstimate.avgTagDist);
        SmartDashboard.putNumber("Avg Tag Area", poseEstimate.avgTagArea);
        SmartDashboard.putNumber("Odom Pose/x", swerveDrive.getPose().getX());
        SmartDashboard.putNumber("Odom Pose/y", swerveDrive.getPose().getY());
        SmartDashboard.putNumber("Odom Pose/degrees", swerveDrive.getPose().getRotation().getDegrees());
        SmartDashboard.putNumber("Limelight Pose/x", poseEstimate.pose.getX());
        SmartDashboard.putNumber("Limelight Pose/y", poseEstimate.pose.getY());
        SmartDashboard.putNumber("Limelight Pose/degrees", poseEstimate.pose.toPose2d().getRotation().getDegrees());
        if (result.valid)
        {
          // Pose2d estimatorPose = poseEstimate.pose.toPose2d();
          Pose2d usefulPose     = result.getBotPose2d(Alliance.Blue);
          double distanceToPose = usefulPose.getTranslation().getDistance(swerveDrive.getPose().getTranslation());
          if (distanceToPose < 0.5 || (outofAreaReading > 10) || (outofAreaReading > 10 && !initialReading))
          {
            if (!initialReading)
            {
              initialReading = true;
            }
            outofAreaReading = 0;
            // System.out.println(usefulPose.toString());
            swerveDrive.setVisionMeasurementStdDevs(VecBuilder.fill(0.05, 0.05, 0.022));
            // System.out.println(result.timestamp_LIMELIGHT_publish);
            // System.out.println(result.timestamp_RIOFPGA_capture);
            swerveDrive.addVisionMeasurement(usefulPose, result.timestamp_RIOFPGA_capture);
          } else
          {
            outofAreaReading += 1;
          }
  //        swerveDrive.addVisionMeasurement(estimatorPose, poseEstimate.timestampSeconds);
        }
  
      }
  
    }
  
    public Rotation2d getOdometryHeading(){
      return swerveDrive.getOdometryHeading();
    }
  
   /**
     * Gets the current yaw angle of the robot, as reported by the swerve pose estimator in the underlying drivebase.
     * Note, this is not the raw gyro reading, this may be corrected from calls to resetOdometry().
     *
     * @return The yaw angle
     */
    public static Rotation2d getHeading()
    {
      return getPose().getRotation();
    }
  
    /**
     * Gets the current pose (position and rotation) of the robot, as reported by odometry.
     *
     * @return The robot's pose
     */
    public static Pose2d getPose()
    {
      return swerveDrive.getPose();
  }

  public Rotation2d getRotation()
  {
    return swerveDrive.getYaw();
  }

  @Override
  public void simulationPeriodic() {
    // This method will be called once per scheduler run during simulation
  }




  public SwerveDrive getSwerveDrive() {
    return swerveDrive;
  }


public void driveFieldOriented(ChassisSpeeds velocity){
  swerveDrive.driveFieldOriented(velocity);
}

public Command driveFieldOriented(Supplier<ChassisSpeeds> velocity){
  return run(() -> {
    swerveDrive.driveFieldOriented(velocity.get());
  });
}


  /**
   * Get the path follower with events.
   *
   * @param pathName PathPlanner path name.
   * @return {@link AutoBuilder#followPath(PathPlannerPath)} path command.
   */
  public Command getAutonomousCommand(String pathName)
  {
    // Create a path following command using AutoBuilder. This will also trigger event markers.
    return null;
  }

  
  public Command driveForwards()
  {
    return run(() -> {
      swerveDrive.drive(new Translation2d(1, 0), 0, false, false);
    }).finallyDo(() -> swerveDrive.drive(new Translation2d(0, 0), 0, false, false));
  }

  public Command driveBackwards()
  {
    return run(() -> {
      swerveDrive.drive(new Translation2d(-1, 0), 0, false, false);
    }).finallyDo(() -> swerveDrive.drive(new Translation2d(0, 0), 0, false, false));
  }

  public Command lockPos()
  {
    return run(swerveDrive::lockPose);
  }


  public Command drive(Supplier<ChassisSpeeds> driveAngularVelocity)
  {
    return run(() -> {
      swerveDrive.drive(driveAngularVelocity.get());
    });
  }


  public Command rotateToHeading(Rotation2d rotation2d)
  {
    return run(() -> swerveDrive.drive(new Translation2d(0, 0),
                                       swerveDrive.getSwerveController().headingCalculate(getHeading().getRadians(),
                                                                                          getHeading().getRadians() -
                                                                                          rotation2d.getRadians()),
                                       false, true));
  }

  public void zeroGyro()
  {
    swerveDrive.zeroGyro();
  }

}