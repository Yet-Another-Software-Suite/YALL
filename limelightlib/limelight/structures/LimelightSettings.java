package frc.robot.limelight.structures;

import static frc.robot.limelight.LimelightHelpers.sanitizeName;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import frc.robot.limelight.Limelight;

public class LimelightSettings
{

  /**
   * {@link NetworkTable} for the {@link Limelight}
   */
  private NetworkTable      limelightTable;
  /**
   * {@link Limelight} to fetch data for.
   */
  private Limelight         limelight;
  /**
   * LED Mode for the limelight. 0 = Pipeline Control, 1 = Force Off, 2 = Force Blink, 3 = Force On
   */
  private NetworkTableEntry ledMode;
  /**
   * Limelight PipelineIndex to use.
   */
  private NetworkTableEntry pipelineIndex;
  /**
   * Priority TagID for the limelight.
   */
  private NetworkTableEntry priorityTagID;
  /**
   * Stream mode, 0 = Side-by-side, 1 = Picture-in-Picture (second in corner), 2 = Picture-in-Picture (primary in
   * corner)
   */
  private NetworkTableEntry streamMode;
  /**
   * Crop window for the camera. The crop window in the UI must be completely open. DoubleArray
   * [cropXMin,cropXMax,cropYMin,cropYMax] values between -1 and 1
   */
  private NetworkTableEntry cropWindow;
  /**
   * Sets 3d offset point for easy 3d targeting Sets the 3D point-of-interest offset for the current fiducial pipeline.
   * https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltag-3d#point-of-interest-tracking
   * <p>
   * DoubleArray [offsetX(meters), offsetY(meters), offsetZ(meters)]
   */
  private NetworkTableEntry fiducial3DOffset;
  /**
   * Robot orientation for MegaTag2 localization algorithm. DoubleArray [yaw(degrees), *yawRaw(degreesPerSecond),
   * *pitch(degrees), *pitchRate(degreesPerSecond), *roll(degrees), *rollRate(degreesPerSecond)]
   */
  private NetworkTableEntry robotOrientationSet;
  /**
   * DoubleArray of valid apriltag id's to track.
   */
  private NetworkTableEntry fiducialIDFiltersOverride;
  /**
   * Downscaling factor for AprilTag detection.
   * Increasing downscale can improve performance at the cost of potentially reduced detection range.
   * Valid values ar [0 (pipeline control), 1 (no downscale), 2, 3, 4]
   */
  private NetworkTableEntry downscale;
  /**
   * Camera pose relative to the robot.
   * DoubleArray [forward(meters), side(meters), up(meters), roll(degrees), pitch(degrees), yaw(degrees)]
   */
  private NetworkTableEntry cameraToRobot;

  public LimelightSettings(Limelight camera)
  {
    limelightTable = NetworkTableInstance.getDefault().getTable(sanitizeName(camera.limelightName));
    ledMode = limelightTable.getEntry("ledMode");
    pipelineIndex = limelightTable.getEntry("pipeline");
    priorityTagID = limelightTable.getEntry("priorityid");
    streamMode = limelightTable.getEntry("stream");
    cropWindow = limelightTable.getEntry("crop");
    fiducial3DOffset = limelightTable.getEntry("fiducial_offset_set");
    robotOrientationSet = limelightTable.getEntry("robot_orientation_set");
    fiducialIDFiltersOverride = limelightTable.getEntry("fiducial_id_filters_set");
    downscale = limelightTable.getEntry("fiducial_downscale_set");
  }

  /**
   * Flush the NetworkTable data to server.
   */
  public void flush()
  {
    NetworkTableInstance.getDefault().flush();
  }
}
