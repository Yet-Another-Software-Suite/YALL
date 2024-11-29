package frc.robot.limelight.structures;

import static frc.robot.limelight.LimelightHelpers.sanitizeName;
import static frc.robot.limelight.structures.LimelightUtils.pose3dToArray;
import static frc.robot.limelight.structures.LimelightUtils.toPose3D;
import static frc.robot.limelight.structures.LimelightUtils.translation3dToArray;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.networktables.DoubleArrayEntry;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import frc.robot.limelight.Limelight;
import frc.robot.limelight.LimelightConfig;
import java.util.List;

/**
 * Settings class to apply configurable options to the {@link Limelight}
 */
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
  private DoubleArrayEntry fiducial3DOffset;
  /**
   * Robot orientation for MegaTag2 localization algorithm. DoubleArray [yaw(degrees), *yawRaw(degreesPerSecond),
   * *pitch(degrees), *pitchRate(degreesPerSecond), *roll(degrees), *rollRate(degreesPerSecond)]
   */
  private NetworkTableEntry robotOrientationSet;
  /**
   * DoubleArray of valid apriltag id's to track.
   */
  private DoubleArrayEntry fiducialIDFiltersOverride;
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
  private DoubleArrayEntry  cameraToRobot;

  /**
   * Create a {@link LimelightSettings} object with all configurable features of a {@link Limelight}.
   * @param camera
   */
  public LimelightSettings(Limelight camera)
  {
    limelightTable = limelight.getNTTable();
    ledMode = limelightTable.getEntry("ledMode");
    pipelineIndex = limelightTable.getEntry("pipeline");
    priorityTagID = limelightTable.getEntry("priorityid");
    streamMode = limelightTable.getEntry("stream");
    cropWindow = limelightTable.getEntry("crop");
    robotOrientationSet = limelightTable.getEntry("robot_orientation_set");
    downscale = limelightTable.getEntry("fiducial_downscale_set");

    fiducial3DOffset = limelightTable.getDoubleArrayTopic("fiducial_offset_set").getEntry(new double[0]);
    cameraToRobot = limelightTable.getDoubleArrayTopic("camerapose_robotspace_set").getEntry(new double[0]);
    fiducialIDFiltersOverride = limelightTable.getDoubleArrayTopic("fiducial_id_filters_set").getEntry(new double[0]);
  }
  /**
   * Set the offset from the AprilTag that is of interest. More information here.
   * https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltag-3d#point-of-interest-tracking
   * @param offset {@link Translation3d} offset.
   * @return {@link LimelightSettings} for chaining.
   */
  public LimelightSettings withAprilTagOffset(Translation3d offset)
  {
    fiducial3DOffset.set(translation3dToArray(offset));
    return this;
  }
  /**
   * Set the {@link Limelight} AprilTagID filter/override of which to track.
   *
   * @param idFilter Array of AprilTag ID's to track
   * @return {@link LimelightSettings} for chaining.
   */
  public LimelightSettings withArilTagIdFilter(List<Double> idFilter)
  {
    fiducialIDFiltersOverride.set(idFilter.stream().mapToDouble(Double::doubleValue).toArray());
    return this;
  }

  /**
   * Set the {@link Limelight} offset.
   * @param offset {@link Pose3d} of the {@link Limelight} with the {@link edu.wpi.first.math.geometry.Rotation3d} set.
   * @return {@link LimelightSettings} for chaining.
   */
  public LimelightSettings withCameraOffset(Pose3d offset)
  {
    cameraToRobot.set(pose3dToArray(offset));
    return this;
  }

  /**
   * Get a {@link LimelightConfig} from the current settings.
   * @return {@link LimelightConfig} from current settings.
   */
  public LimelightConfig getConfig()
  {

    return new LimelightConfig().withCameraOffset(toPose3D(cameraToRobot.get()));
  }
}
