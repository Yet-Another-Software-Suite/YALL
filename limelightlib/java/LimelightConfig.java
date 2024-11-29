package frc.robot.limelight;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.List;

/**
 * {@link Limelight} class that contains all robot configurable options.
 */
public class LimelightConfig
{

  /**
   * Robot to camera pose.
   */
  public Pose3d        robotToCamera;
  /**
   * LED Control on the limelight
   */
  public LEDMode       ledMode;
  /**
   * Point of interest offset in Meters from AprilTag center, could be offset from tag to goal. More information here.
   * https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltag-3d#point-of-interest-tracking
   */
  public Translation3d aprilTagOffset;
  /**
   * Stream mode for the Limelight.
   */
  public StreamMode    streamMode;
  /**
   * AprilTag ID's to track.
   */
  public List<Double>  aprilTagOverride;


  /**
   * Set the offset from the AprilTag that is of interest. More information here.
   * https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltag-3d#point-of-interest-tracking
   *
   * @param offset {@link Translation3d} offset.
   * @return {@link LimelightConfig} for chaining.
   */
  public LimelightConfig withAprilTagOffset(Translation3d offset)
  {
    aprilTagOffset = offset;
    return this;
  }

  /**
   * Set the AprilTag override of which the {@link Limelight} is supposed to track.
   *
   * @param override {@link List} of {@link Double}s containing the AprilTag ID's to track.
   * @return {@link LimelightConfig} for chaining.
   */
  public LimelightConfig withAprilTagOverride(List<Double> override)
  {
    this.aprilTagOverride = override;
    return this;
  }

  /**
   * Construct the {@link LimelightConfig}
   */
  public LimelightConfig()
  {

  }

  /**
   * Offset of the {@link Limelight} relative to robot center.
   *
   * @param robotToCamera {@link Pose3d} in Meters, with {@link Rotation3d} which will be converted to Degrees.
   * @return {@link LimelightConfig} for chaining.
   */
  public LimelightConfig withCameraOffset(Pose3d robotToCamera)
  {
    this.robotToCamera = robotToCamera;
    return this;
  }

  /**
   * Setter for {@link LimelightConfig#streamMode}
   *
   * @param streamMode {@link StreamMode} to use.
   * @return {@link LimelightConfig} for chaining.
   */
  public LimelightConfig withStreamMode(StreamMode streamMode)
  {
    this.streamMode = streamMode;
    return this;
  }

  /**
   * LED Mode for the {@link Limelight}.
   */
  enum LEDMode
  {
    PipelineControl,
    ForceOff,
    ForceBlink,
    ForceOn
  }

  /**
   * Stream mode for the {@link Limelight}
   */
  enum StreamMode
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
}
