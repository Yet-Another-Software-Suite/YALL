package limelight.networktables;

import static limelight.networktables.LimelightUtils.toPose3D;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.networktables.DoubleArrayEntry;
import edu.wpi.first.wpilibj.DriverStation;
import java.util.Optional;
import limelight.Limelight;

/**
 * Pose estimator for {@link Limelight}.
 */
public class LimelightPoseEstimator {
  /**
   * Represents the pose estimation mode used by the Limelight camera.
   * <p>
   * Each mode corresponds to a different vision pipeline for AprilTag-based
   * localization, with distinct methods for combining visual and robot data.
   */
  public static enum EstimationMode {
    /**
     * Uses the original Limelight MegaTag1 pipeline for pose estimation.
     * <p>
     * MegaTag1 estimates the robot's pose using data from a single detected
     * AprilTag. It relies entirely on visual information from the camera,
     * without using any external robot state data.
     */
    MEGATAG1,

    /**
     * Uses the Limelight MegaTag2 pipeline for pose estimation.
     * <p>
     * MegaTag2 fuses AprilTag data with the robot's current heading (from a
     * gyro or odometry source) to refine the estimated pose. This results in
     * smoother and more accurate global localization, especially when tags
     * are viewed from oblique angles or at long range.
     * @see LimelightSettings#withRobotOrientation
     */
    MEGATAG2
  }

  /**
   * {@link Limelight} name to use.
   */
  private final Limelight limelight;
  /**
   * Estimation mode to use (MEGATAG1 / MEGATAG2).
   */
  private final EstimationMode estimationMode;
  /**
   * Old botpose from megatag1
   */
  @Deprecated
  private DoubleArrayEntry botpose;

  /**
   * Construct {@link LimelightPoseEstimator} which fetches data from
   * NetworkTables
   *
   * @param camera         {@link Limelight} to use.
   * @param estimationMode Estimation mode to use (MEGATAG1 / MEGATAG2).
   */
  public LimelightPoseEstimator(Limelight camera, EstimationMode estimationMode) {
    limelight = camera;
    this.estimationMode = estimationMode;
    botpose = limelight.getNTTable().getDoubleArrayTopic("botpose").getEntry(new double[0]);
  }

  /**
   * Get the MegaTag1 bot pose.
   *
   * @return {@link Pose3d} of the MegaTag1 pose estimate for the bot pose.
   */
  @Deprecated
  public Pose3d getBotPose() {
    return toPose3D(botpose.get());
  }

  /**
   * Get the {@link PoseEstimate} corresponding with your alliance color.
   * <p>
   * Alliance comes from DriverStation. If simulation, then the sim GUI must not
   * be in "disconnected" mode.
   *
   * @return {@link Optional} of {@link PoseEstimate} of your given alliance.
   */
  public Optional<PoseEstimate> getAlliancePoseEstimate() {
    if (DriverStation.getAlliance().isEmpty()) {
      return Optional.empty();
    }

    switch (DriverStation.getAlliance().get()) {
      case Red -> {
        return estimationMode == EstimationMode.MEGATAG2 ? BotPose.RED_MEGATAG2.get(limelight)
            : BotPose.RED.get(limelight);
      }
      case Blue -> {
        return estimationMode == EstimationMode.MEGATAG2 ? BotPose.BLUE_MEGATAG2.get(limelight)
            : BotPose.BLUE.get(limelight);
      }
    }
    return Optional.empty();
  }

  /**
   * Get the global pose estimate based off WPILib coordinates, blue-origin
   *
   * @return {@link Optional} of {@link PoseEstimate} for blue-origin based poses.
   */
  public Optional<PoseEstimate> getPoseEstimate() {
    return estimationMode == EstimationMode.MEGATAG2 ? BotPose.BLUE_MEGATAG2.get(limelight)
        : BotPose.BLUE.get(limelight);
  }

  /**
   * BotPose enum for easier decoding.
   */
  public enum BotPose {
    /**
     * (Not Recommended) The robot's pose in the WPILib Red Alliance Coordinate
     * System.
     */
    RED("botpose_wpired", false),
    /**
     * (Not Recommended) The robot's pose in the WPILib Red Alliance Coordinate
     * System with MegaTag2.
     */
    RED_MEGATAG2("botpose_orb_wpired", true),
    /**
     * (Recommended) The robot's 3D pose in the WPILib Blue Alliance Coordinate
     * System.
     */
    BLUE("botpose_wpiblue", false),
    /**
     * (Recommended) The robot's 3D pose in the WPILib Blue Alliance Coordinate
     * System with MegaTag2.
     */
    BLUE_MEGATAG2("botpose_orb_wpiblue", true);

    /**
     * {@link Limelight} botpose entry name.
     */
    private final String entry;
    /**
     * Is megatag2 reading?
     */
    private final boolean isMegaTag2;
    /**
     * Current {@link PoseEstimate}
     */
    private Optional<PoseEstimate> poseEstimate;

    /**
     * Create {@link BotPose} enum with given entry names and megatag2 state.
     *
     * @param entryName Bot Pose entry name for {@link Limelight}
     * @param megatag2  MegaTag2 reading.
     */
    BotPose(String entryName, boolean megatag2) {
      entry = entryName;
      isMegaTag2 = megatag2;
      poseEstimate = Optional.empty();
    }

    /**
     * Fetch the {@link PoseEstimate} if it exists.
     *
     * @param camera {@link Limelight} to use.
     * @return Current {@link PoseEstimate}.
     */
    public Optional<PoseEstimate> get(Limelight camera) {
      if (poseEstimate.isEmpty()) {
        PoseEstimate estimate = new PoseEstimate(camera, entry, isMegaTag2);
        poseEstimate = Optional.of(estimate);
      }
      return poseEstimate.get().getPoseEstimate();
    }

  }

}
