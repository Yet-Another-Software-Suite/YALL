package limelight.networktables.target;

import static limelight.networktables.LimelightUtils.toPose2D;
import static limelight.networktables.LimelightUtils.toPose3D;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;

/**
 * Represents an AprilTag/Fiducial Target Result extracted from JSON Output
 */
public class AprilTagFiducial
{

  /**
   * Fiducial tag ID
   */
  @JsonProperty("fID")
  public double fiducialID;

  /**
   * Fiducial Family (16H5C, 25H9C, 36H11C, etc)
   */
  @JsonProperty("fam")
  public  String   fiducialFamily;
  /**
   * The size of the target as a percentage of the image (0-1)
   */
  @JsonProperty("ta")
  public  double   ta;
  /**
   * X-coordinate of the center of the target in degrees relative to crosshair. Positive-right, center-zero
   */
  @JsonProperty("tx")
  public  double   tx;
  /**
   * 	Y-coordinate of the center of the target in degrees relative to crosshair. Positive-down, center-zero
   */
  @JsonProperty("ty")
  public  double   ty;
  /**
   * X-coordinate of the center of the target in pixels relative to crosshair. Positive-right, center-zero
   */
  @JsonProperty("txp")
  public  double   tx_pixels;
  /**
   * Y-coordinate of the center of the target in pixels relative to crosshair. Positive-down, center-zero
   */
  @JsonProperty("typ")
  public  double   ty_pixels;
  /**
   * X-coordinate of the center of the target in degrees relative to principal piexel. Positive-right, center-zero
   */
  @JsonProperty("tx_nocross")
  public  double   tx_nocrosshair;
  /**
   * Y-coordinate of the center of the target in degrees relative to principal pixel. Positive-right, center-zero
   */
  @JsonProperty("ty_nocross")
  public  double   ty_nocrosshair;
  /**
   * Timestamp in milliseconds from boot.
   */
  @JsonProperty("ts")
  public  double   ts;
  /**
   * Camera Pose in target space as computed by solvepnp (x,y,z,rx,ry,rz)
   */
  @JsonProperty("t6c_ts")
  private double[] cameraPose_TargetSpace;
  /**
   * Robot Pose in field space as computed by solvepnp (x,y,z,rx,ry,rz)
   */
  @JsonProperty("t6r_fs")
  private double[] robotPose_FieldSpace;
  /**
   * Robot Pose in target space as computed by solvepnp (x,y,z,rx,ry,rz)
   */
  @JsonProperty("t6r_ts")
  private double[] robotPose_TargetSpace;
  /**
   * Target Pose in camera space as computed by solvepnp (x,y,z,rx,ry,rz)
   */
  @JsonProperty("t6t_cs")
  private double[] targetPose_CameraSpace;
  /**
   * Target Pose in robot space as computed by solvepnp (x,y,z,rx,ry,rz)
   */
  @JsonProperty("t6t_rs")
  private double[] targetPose_RobotSpace;

  /**
   * Create the AprilTagFiducial object
   */
  public AprilTagFiducial()
  {
    cameraPose_TargetSpace = new double[6];
    robotPose_FieldSpace = new double[6];
    robotPose_TargetSpace = new double[6];
    targetPose_CameraSpace = new double[6];
    targetPose_RobotSpace = new double[6];
  }

  /**
   * Get the AprilTag's 3D pose in target space
   *
   * @return {@link Pose3d} object representing the AprilTag's position and orientation relative to the camera
   */
  public Pose3d getCameraPose_TargetSpace()
  {
    return toPose3D(cameraPose_TargetSpace);
  }

  /**
   * Get the AprilTag's 3D pose in field space
   *
   * @return {@link Pose3d} object representing the AprilTag's position and orientation relative to the robot
   */
  public Pose3d getRobotPose_FieldSpace()
  {
    return toPose3D(robotPose_FieldSpace);
  }

  /**
   * Get the AprilTag's 3D pose in target space
   *
   * @return {@link Pose3d} object representing the AprilTag's position and orientation relative to the robot
   */
  public Pose3d getRobotPose_TargetSpace()
  {
    return toPose3D(robotPose_TargetSpace);
  }

  /**
   * Get the AprilTag's 3D pose in camera space
   *
   * @return {@link Pose3d} object representing the AprilTag's position and orientation relative to the camera
   */
  public Pose3d getTargetPose_CameraSpace()
  {
    return toPose3D(targetPose_CameraSpace);
  }

  /**
   * Get the AprilTag's 3D pose in robot space
   *
   * @return {@link Pose3d} object representing the AprilTag's position and orientation relative to the robot
   */
  public Pose3d getTargetPose_RobotSpace()
  {
    return toPose3D(targetPose_RobotSpace);
  }

  /**
   * Get the AprilTag's 2D pose in target space
   *
   * @return {@link Pose2d} object representing the AprilTag's position and orientation relative to the camera
   */
  public Pose2d getCameraPose_TargetSpace2D()
  {
    return toPose2D(cameraPose_TargetSpace);
  }

  /**
   * Get the AprilTag's 2D pose in field space
   *
   * @return {@link Pose2d} object representing the AprilTag's position and orientation relative to the robot
   */
  public Pose2d getRobotPose_FieldSpace2D()
  {
    return toPose2D(robotPose_FieldSpace);
  }

  /**
   * Get the AprilTag's 2D pose in target space
   *
   * @return {@link Pose2d} object representing the AprilTag's position and orientation relative to the robot
   */
  public Pose2d getRobotPose_TargetSpace2D()
  {
    return toPose2D(robotPose_TargetSpace);
  }

  /**
   * Get the AprilTag's 2D pose in camera space
   *
   * @return {@link Pose2d} object representing the AprilTag's position and orientation relative to the camera
   */
  public Pose2d getTargetPose_CameraSpace2D()
  {
    return toPose2D(targetPose_CameraSpace);
  }

  /**
   * Get the AprilTag's 2D pose in robot space
   *
   * @return {@link Pose2d} object representing the AprilTag's position and orientation relative to the robot
   */
  public Pose2d getTargetPose_RobotSpace2D()
  {
    return toPose2D(targetPose_RobotSpace);
  }
}
