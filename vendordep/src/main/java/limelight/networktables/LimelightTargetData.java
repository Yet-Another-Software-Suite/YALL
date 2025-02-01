package limelight.networktables;


import static limelight.networktables.LimelightUtils.toPose3D;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.networktables.DoubleArrayEntry;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import limelight.Limelight;

public class LimelightTargetData
{

  /**
   * {@link NetworkTable} for the {@link Limelight}
   */
  private NetworkTable limelightTable;
  /**
   * {@link Limelight} to fetch data for.
   */
  private Limelight    limelight;

  /**
   * NetworkTable entry for target validity
   */
  private NetworkTableEntry targetValid;
  /**
   * Color of the targets in view.
   */
  private DoubleArrayEntry  targetColor;
  /**
   * Horizontal Offset from the crosshair to the target in degrees.
   */
  private NetworkTableEntry horizontalOffset;
  /**
   * Vertical offset from the crosshair to the target in degrees.
   */
  private NetworkTableEntry verticalOffset;
  /**
   * Horizontal offset from the principal pixel/point to the target in degrees.
   */
  private NetworkTableEntry horizontalOffsetFromPrincipal;
  /**
   * Vertical offset from the principal pixel/point to the target in degrees.
   */
  private NetworkTableEntry verticalOffsetFromPrincipal;
  /**
   * Target area as a percentage of the image (0-100%).
   */
  private NetworkTableEntry targetArea;
  /**
   * Array containing  [targetValid, targetCount, targetLatency, captureLatency, tx, ty, txnc, tync, ta, tid,
   * targetClassIndexDetector, targetClassIndexClassifier, targetLongSidePixels, targetShortSidePixels,
   * targetHorizontalExtentPixels, targetVerticalExtentPixels, targetSkewDegrees]
   */
  private DoubleArrayEntry  targetMetrics;
  /**
   * {@link Pose3d} object representing the target's position and orientation relative to the robot
   */
  private DoubleArrayEntry  target2RobotPose;
  /**
   * {@link Pose3d} object representing the target's position and orientation relative to the camera.
   */
  private DoubleArrayEntry  target2CameraPose;
  /**
   * {@link Pose3d} object representing the camera's position and orientation relative to the target.
   */
  private DoubleArrayEntry  camera2TargetPose;
  /**
   * {@link Pose3d} object representing the robot's position and orientation relative to the target
   */
  private DoubleArrayEntry  robot2TargetPose;
  /**
   * Current AprilTag fiducial ID.
   */
  private NetworkTableEntry fiducialID;
  /**
   * Current Neural class ID.
   */
  private NetworkTableEntry neuralClassID;

  /**
   * Construct data for targets.
   *
   * @param camera {@link Limelight} to use.
   */
  public LimelightTargetData(Limelight camera)
  {
    limelight = camera;
    limelightTable = limelight.getNTTable();
    targetValid = limelightTable.getEntry("tv");
    targetColor = limelightTable.getDoubleArrayTopic("tc").getEntry(new double[0]);
    fiducialID = limelightTable.getEntry("tid");
    neuralClassID = limelightTable.getEntry("tclass");
    horizontalOffset = limelightTable.getEntry("tx");
    verticalOffset = limelightTable.getEntry("ty");
    horizontalOffsetFromPrincipal = limelightTable.getEntry("txnc");
    verticalOffsetFromPrincipal = limelightTable.getEntry("tync");
    targetArea = limelightTable.getEntry("ta");
    targetMetrics = limelightTable.getDoubleArrayTopic("t2d").getEntry(new double[0]);
    target2RobotPose = limelightTable.getDoubleArrayTopic("targetpose_robotspace").getEntry(new double[0]);
    target2CameraPose = limelightTable.getDoubleArrayTopic("targetpose_cameraspace").getEntry(new double[0]);
    camera2TargetPose = limelightTable.getDoubleArrayTopic("camerapose_targetspace").getEntry(new double[0]);
    robot2TargetPose = limelightTable.getDoubleArrayTopic("botpose_targetspace").getEntry(new double[0]);

  }

  /**
   * Get the current AprilTag ID targetted
   *
   * @return AprilTag ID.
   */
  public double getAprilTagID()
  {
    return fiducialID.getDouble(0.0);
  }

  /**
   * Get the neural class name of the target.
   *
   * @return Neural class name.
   */
  public String getNeuralClassID()
  {
    return neuralClassID.getString("");
  }

  /**
   * Get the target color in HSV/RGB format.
   *
   * @return Target color in HSV/RGB format
   */
  public double[] getTargetColor()
  {
    return targetColor.get();
  }

  /**
   * Gets the robot's 3D pose with respect to the currently tracked target's coordinate system.
   *
   * @return {@link Pose3d} object representing the robot's position and orientation relative to the target
   */
  public Pose3d getRobotToTarget()
  {
    return toPose3D(robot2TargetPose.get());
  }

  /**
   * Gets the camera's 3D pose with respect to the currently tracked target's coordinate system.
   *
   * @return {@link Pose3d} object representing the camera's position and orientation relative to the target
   */
  public Pose3d getCameraToTarget()
  {
    return toPose3D(camera2TargetPose.get());
  }

  /**
   * Gets the target's 3D pose with respect to the camera's coordinate system.
   *
   * @return {@link Pose3d} object representing the target's position and orientation relative to the camera
   */
  public Pose3d getTargetToCamera()
  {
    return toPose3D(target2CameraPose.get());
  }

  /**
   * Gets the target's 3D pose with respect to the robot's coordinate system.
   *
   * @return {@link Pose3d} object representing the target's position and orientation relative to the robot
   */
  public Pose3d getTargetToRobot()
  {
    return toPose3D(target2RobotPose.get());
  }

  /**
   * Does the {@link Limelight} have a valid target?
   *
   * @return True if a valid target is present, false otherwise
   */
  public boolean getTargetStatus()
  {
    return targetValid.getDouble(0) == 1.0;
  }

  /**
   * Gets the horizontal offset from the crosshair to the target in degrees.
   *
   * @return Horizontal offset angle in degrees
   */
  public double getHorizontalOffset()
  {
    return horizontalOffset.getDouble(0);
  }

  /**
   * Gets the vertical offset from the crosshair to the target in degrees.
   *
   * @return Vertical offset angle in degrees
   */
  public double getVerticalOffset()
  {
    return verticalOffset.getDouble(0);
  }

  /**
   * Gets the horizontal offset from the principal pixel/point to the target in degrees.  This is the most accurate 2d
   * metric if you are using a calibrated camera and you don't need adjustable crosshair functionality.
   *
   * @return Horizontal offset angle in degrees
   */
  public double getHorizontalOffsetFromPrincipal()
  {
    return horizontalOffsetFromPrincipal.getDouble(0);
  }

  /**
   * Gets the vertical offset from the principal pixel/point to the target in degrees.  This is the most accurate 2d
   * metric if you are using a calibrated camera and you don't need adjustable crosshair functionality.
   *
   * @return Horizontal offset angle in degrees
   */
  public double getVerticalOffsetFromPrincipal()
  {
    return verticalOffsetFromPrincipal.getDouble(0);
  }

  /**
   * Gets the target area as a percentage of the image (0-100%).
   *
   * @return Target area percentage (0-100)
   */
  public double getTargetArea()
  {
    return targetArea.getDouble(0);
  }

  /**
   * T2D is an array that contains several targeting metrcis
   *
   * @return Array containing  [targetValid, targetCount, targetLatency, captureLatency, tx, ty, txnc, tync, ta, tid,
   * targetClassIndexDetector, targetClassIndexClassifier, targetLongSidePixels, targetShortSidePixels,
   * targetHorizontalExtentPixels, targetVerticalExtentPixels, targetSkewDegrees]
   */
  public double[] getTargetMetrics()
  {
    // TODO: Create TargetMetrics class for this data.
    return targetMetrics.get();
  }

  /**
   * Gets the number of targets currently detected.
   *
   * @return Number of detected targets
   */
  public int getTargetCount()
  {
    double[] t2d = getTargetMetrics();
    if (t2d.length == 17)
    {
      return (int) t2d[1];
    }
    return 0;
  }

  /**
   * Gets the classifier class index from the currently running neural classifier pipeline
   *
   * @return Class index from classifier pipeline
   */
  public int getClassifierClassIndex()
  {
    double[] t2d = getTargetMetrics();
    if (t2d.length == 17)
    {
      return (int) t2d[10];
    }
    return 0;
  }

  /**
   * Gets the detector class index from the primary result of the currently running neural detector pipeline.
   *
   * @return Class index from detector pipeline
   */
  public int getDetectorClassIndex()
  {
    double[] t2d = getTargetMetrics();
    if (t2d.length == 17)
    {
      return (int) t2d[11];
    }
    return 0;
  }


}