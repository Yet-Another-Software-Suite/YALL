package frc.robot.limelight.structures.target;

import static frc.robot.limelight.structures.LimelightUtils.toPose2D;
import static frc.robot.limelight.structures.LimelightUtils.toPose3D;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;

/**
 * Represents an AprilTag/Fiducial Target Result extracted from JSON Output
 */
public class AprilTagFiducial
{

  @JsonProperty("fID")
  public double fiducialID;

  @JsonProperty("fam")
  public String fiducialFamily;

  @JsonProperty("t6c_ts")
  private double[] cameraPose_TargetSpace;

  @JsonProperty("t6r_fs")
  private double[] robotPose_FieldSpace;

  @JsonProperty("t6r_ts")
  private double[] robotPose_TargetSpace;

  @JsonProperty("t6t_cs")
  private double[] targetPose_CameraSpace;

  @JsonProperty("t6t_rs")
  private double[] targetPose_RobotSpace;

  public Pose3d getCameraPose_TargetSpace()
  {
    return toPose3D(cameraPose_TargetSpace);
  }

  public Pose3d getRobotPose_FieldSpace()
  {
    return toPose3D(robotPose_FieldSpace);
  }

  public Pose3d getRobotPose_TargetSpace()
  {
    return toPose3D(robotPose_TargetSpace);
  }

  public Pose3d getTargetPose_CameraSpace()
  {
    return toPose3D(targetPose_CameraSpace);
  }

  public Pose3d getTargetPose_RobotSpace()
  {
    return toPose3D(targetPose_RobotSpace);
  }

  public Pose2d getCameraPose_TargetSpace2D()
  {
    return toPose2D(cameraPose_TargetSpace);
  }

  public Pose2d getRobotPose_FieldSpace2D()
  {
    return toPose2D(robotPose_FieldSpace);
  }

  public Pose2d getRobotPose_TargetSpace2D()
  {
    return toPose2D(robotPose_TargetSpace);
  }

  public Pose2d getTargetPose_CameraSpace2D()
  {
    return toPose2D(targetPose_CameraSpace);
  }

  public Pose2d getTargetPose_RobotSpace2D()
  {
    return toPose2D(targetPose_RobotSpace);
  }

  @JsonProperty("ta")
  public double ta;

  @JsonProperty("tx")
  public double tx;

  @JsonProperty("ty")
  public double ty;

  @JsonProperty("txp")
  public double tx_pixels;

  @JsonProperty("typ")
  public double ty_pixels;

  @JsonProperty("tx_nocross")
  public double tx_nocrosshair;

  @JsonProperty("ty_nocross")
  public double ty_nocrosshair;

  @JsonProperty("ts")
  public double ts;

  public AprilTagFiducial()
  {
    cameraPose_TargetSpace = new double[6];
    robotPose_FieldSpace = new double[6];
    robotPose_TargetSpace = new double[6];
    targetPose_CameraSpace = new double[6];
    targetPose_RobotSpace = new double[6];
  }
}
