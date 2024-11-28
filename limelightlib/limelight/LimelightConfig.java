package frc.robot.limelight;

import edu.wpi.first.math.geometry.Pose3d;

public class LimelightConfig
{

  /**
   * Robot to camera pose.
   */
  public Pose3d robotToCamera;
  /**
   * LED Control on the limelight
   */
  public LEDMode ledMode;

  enum LEDMode {
    PipelineControl,
    ForceOff,
    ForceBlink,
    ForceOn
  }

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
