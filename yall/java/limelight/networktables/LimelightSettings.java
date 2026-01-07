package limelight.networktables;

import static limelight.networktables.LimelightUtils.orientation3dToArray;
import static limelight.networktables.LimelightUtils.pose3dToArray;
import static limelight.networktables.LimelightUtils.translation3dToArray;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.networktables.DoubleArrayEntry;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.List;
import limelight.Limelight;

/**
 * Settings class to apply configurable options to the {@link Limelight}
 * <p>
 * These settings are sent from the roboRIO back to the Limelight to affect the LL.
 * <p>
 * One or more chains of ".withXXXX" methods can change the LL settings. The action of each ".withXXXX" method is
 * essentially immediate, however, some slight delay is possible and the {@link #save} method will immediately save any
 * settings that had not yet been saved.
 * <p>
 * <p>
 * Initially, at constructor time, settings are fetched from the LL, however, there is no provision to programatically
 * access those values - they are dead, useless.
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
   * {@link Limelight} PipelineIndex to use.
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
  private DoubleArrayEntry  cropWindow;
  /**
   * Imu Mode for the Limelight 4.
   */
  private NetworkTableEntry imuMode;
  /**
   * Imu Assist Alpha value for the Limelight 4.
   */
  private NetworkTableEntry imuAssistAlpha;
  /**
   * Sets 3d offset point for easy 3d targeting Sets the 3D point-of-interest offset for the current fiducial pipeline.
   * <p>
   * https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltag-3d#point-of-interest-tracking
   * <p>
   * DoubleArray [offsetX(meters), offsetY(meters), offsetZ(meters)]
   */
  private DoubleArrayEntry  fiducial3DOffset;
  /**
   * Robot orientation for MegaTag2 localization algorithm.
   * <p>
   * DoubleArray [yaw(degrees), *yawRaw(degreesPerSecond), *pitch(degrees), *pitchRate(degreesPerSecond),
   * *roll(degrees), *rollRate(degreesPerSecond)]
   */
  private DoubleArrayEntry  robotOrientationSet;
  /**
   * DoubleArray of valid apriltag id's to track.
   */
  private DoubleArrayEntry  fiducialIDFiltersOverride;
  /**
   * Downscaling factor for AprilTag detection. Increasing downscale can improve performance at the cost of potentially
   * reduced detection range. Valid values ar [0 (pipeline control), 1 (no downscale), 2, 3, 4]
   */
  private NetworkTableEntry downscale;
  /**
   * Camera pose relative to the robot. DoubleArray [forward(meters), side(meters), up(meters), roll(degrees),
   * pitch(degrees), yaw(degrees)]
   */
  private DoubleArrayEntry  cameraToRobot;

  /**
   * Create a {@link LimelightSettings} object with all configurable features of a {@link Limelight}.
   *
   * @param camera {@link Limelight} to use.
   */
  public LimelightSettings(Limelight camera)
  {
    limelight = camera;
    limelightTable = limelight.getNTTable();
    ledMode = limelightTable.getEntry("ledMode");

    pipelineIndex = limelightTable.getEntry("pipeline");
    priorityTagID = limelightTable.getEntry("priorityid");
    streamMode = limelightTable.getEntry("stream");
    cropWindow = limelightTable.getDoubleArrayTopic("crop").getEntry(new double[0]);
    imuMode = limelightTable.getEntry("imumode_set");
    imuAssistAlpha = limelightTable.getEntry("imuassistalpha_set");
    robotOrientationSet = limelightTable.getDoubleArrayTopic("robot_orientation_set").getEntry(new double[0]);
    downscale = limelightTable.getEntry("fiducial_downscale_set");
    fiducial3DOffset = limelightTable.getDoubleArrayTopic("fiducial_offset_set").getEntry(new double[0]);
    cameraToRobot = limelightTable.getDoubleArrayTopic("camerapose_robotspace_set").getEntry(new double[0]);
    fiducialIDFiltersOverride = limelightTable.getDoubleArrayTopic("fiducial_id_filters_set").getEntry(new double[0]);
  }

  /**
   * Set the {@link Limelight} {@link LEDMode}.
   * <p> This method changes the Limelight - normally immediately.
   *
   * @param mode {@link LEDMode} enum
   * @return {@link LimelightSettings} for chaining.
   */
  public LimelightSettings withLimelightLEDMode(LEDMode mode)
  {
    ledMode.setNumber(mode.ordinal());
    return this;
  }

  /**
   * Set the current pipeline index for the {@link Limelight}
   * <p> This method changes the Limelight - normally immediately.
   *
   * @param index Pipeline index to use.
   * @return {@link LimelightSettings}
   */
  public LimelightSettings withPipelineIndex(int index)
  {
    pipelineIndex.setNumber(index);
    return this;
  }

  /**
   * Set the Priority Tag ID for {@link Limelight}
   * <p> This method changes the Limelight - normally immediately.
   *
   * @param aprilTagId AprilTag ID to set as a priority.
   * @return {@link LimelightSettings} for chaining.
   */
  public LimelightSettings withPriorityTagId(int aprilTagId)
  {
    priorityTagID.setNumber(aprilTagId);
    return this;
  }

  /**
   * Set the Stream mode based on the {@link StreamMode} enum
   * <p> This method changes the Limelight - normally immediately.
   *
   * @param mode {@link StreamMode} to use.
   * @return {@link LimelightSettings} for chaining.
   */
  public LimelightSettings withStreamMode(StreamMode mode)
  {
    streamMode.setNumber(mode.ordinal());
    return this;
  }

  /**
   * Sets the crop window for the camera. The crop window in the UI must be completely open.
   * <p> This method changes the Limelight - normally immediately.
   *
   * @param minX Minimum X value (-1 to 1)
   * @param maxX Maximum X value (-1 to 1)
   * @param minY Minimum Y value (-1 to 1)
   * @param maxY Maximum Y value (-1 to 1)
   * @return {@link LimelightSettings} for chaining.
   */
  public LimelightSettings withCropWindow(double minX, double maxX, double minY, double maxY)
  {
    cropWindow.set(new double[]{minX, maxX, minY, maxY});
    return this;
  }

  /**
   * Set the IMU Mode based on the {@link ImuMode} enum. 
   * <p> This method changes the Limelight - normally immediately.
   *
   * @param mode {@link ImuMode} to use.
   * @return {@link LimelightSettings} for chaining.
   */
  public LimelightSettings withImuMode(ImuMode mode)
  {
    imuMode.setNumber(mode.ordinal());
    return this;
  }

  /**
   * Set the IMU Assist filter alpha/strength value. Higher values will cause the internal imu to converge on
   * assist source more rapidly.
   * <p> This method changes the Limelight - normally immediately.
   *
   * @param alpha Assist alpha value (0.001 by default).
   * @return {@link LimelightSettings} for chaining.
   */
  public LimelightSettings withImuAssistAlpha(double alpha)
  {
    imuAssistAlpha.setNumber(alpha);
    return this;
  }

  /**
   * Set the current robot {@link Orientation3d} (normally given by the robot gyro) for LL to use in its MegaTag2
   * determination.
   * <p> This method changes the Limelight - normally immediately.
   *
   * @param orientation {@link Orientation3d} object to set the current orientation to.
   * @return {@link LimelightSettings} for chaining.
   */
  public LimelightSettings withRobotOrientation(Orientation3d orientation)
  {
    robotOrientationSet.set(orientation3dToArray(orientation));
    save();
    return this;
  }

  /**
   * Sets the downscaling factor for AprilTag detection. Increasing downscale can improve performance at the cost of
   * potentially reduced detection range.
   * <p> This method changes the Limelight - normally immediately.
   *
   * @param downscalingOverride Downscale factor. Valid values: 1.0 (no downscale), 1.5, 2.0, 3.0, 4.0. Set to 0 for
   *                            pipeline control.
   * @return {@link LimelightSettings} for chaining.
   */
  public LimelightSettings withFiducialDownscalingOverride(DownscalingOverride downscalingOverride)
  {
    downscale.setDouble(downscalingOverride.ordinal());
    return this;
  }

  /**
   * Set the offset from the AprilTag that is of interest. More information here. <a
   * href="https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltag-3d#point-of-interest-tracking">Docs
   * page</a>
   * <p> This method changes the Limelight - normally immediately.
   *
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
   * <p> This method changes the Limelight - normally immediately.
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
   * <p> This method changes the Limelight - normally immediately.
   *
   * @param offset {@link Pose3d} of the {@link Limelight} with the {@link edu.wpi.first.math.geometry.Rotation3d} set
   *               in Meters.
   * @return {@link LimelightSettings} for chaining.
   */
  public LimelightSettings withCameraOffset(Pose3d offset)
  {
    cameraToRobot.set(pose3dToArray(offset));
    return this;
  }

  /**
   * Push any pending changes to the {@link NetworkTable} instance immediately.
   * <p> This method changes the Limelight immediately.
   * <p> Most setting changes are done essentially immediately and this method
   * isn't needed but does no harm to assure changes.
   */
  public void save()
  {
    NetworkTableInstance.getDefault().flush();
  }


  /**
   * LED Mode for the {@link Limelight}.
   */
  public enum LEDMode
  {
    /**
     * LED Mode Pipeline Control.
     */
    PipelineControl,
    /**
     * LED Mode Force Off.
     */
    ForceOff,
    /**
     * LED Mode Force Blink.
     */
    ForceBlink,
    /**
     * LED Mode Force On.
     */
    ForceOn
  }

  /**
   * Stream mode for the {@link Limelight}
   */
  public enum StreamMode
  {
    /**
     * Side by side.
     */
    Standard,
    /**
     * Picture in picture, with secondary in corner.
     */
    PictureInPictureMain,
    /**
     * Picture in picture, with main in corner.
     */
    PictureInPictureSecondary
  }


  /**
   * Downscaling Override Enum for {@link Limelight}
   */
  public enum DownscalingOverride
  {
    /**
     * Pipeline downscaling, equivalent to 0
     */
    Pipeline,
    /**
     * No downscaling, equivalent to 1
     */
    NoDownscale,
    /**
     * Half downscaling, equivalent to 1.5
     */
    HalfDownscale,
    /**
     * Double downscaling, equivalent to 2
     */
    DoubleDownscale,
    /**
     * Triple downscaling, equivalent to 3
     */
    TripleDownscale,
    /**
     * Quadruple downscaling, equivalent to 4
     */
    QuadrupleDownscale
  }
  /**
   * IMU Mode Enum for the {@link Limelight}
   */
  public enum ImuMode
  {
    /**
     * Use external IMU yaw submitted via {@link LimelightSettings#withRobotOrientation(Orientation3d)} for MT2 localization. The internal IMU is ignored entirely.
     */
    ExternalImu,
    /**
     * Use external IMU yaw submitted via {@link LimelightSettings#withRobotOrientation(Orientation3d)} for MT2 localization. The internal IMU is synced with the external IMU.
     */
    SyncInternalImu,
    /**
     * Use internal IMU for MT2 localization. Ignores external IMU updates from {@link LimelightSettings#withRobotOrientation(Orientation3d)}.
     */
    InternalImu,
    /**
     * Use internal IMU for MT2 localization. The internal IMU will utilize filtered MT1 yaw estimates for continuous heading correction.
     */
    InternalImuMT1Assist,
    /**
     * Use internal IMU for MT2 localization. The internal IMU will utilize the external IMU for continuous heading correction.
     */
    InternalImuExternalAssist
  }

}
