package frc.robot.vision;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Inches;

import com.limelightvision.Limelight;
import frc.robot.mechanisms.SwerveDriveMechanism;
import org.photonvision.PhotonCamera;
import yall.PoseEstimationMode;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.VisionSystem;
import yall.cameras.LimelightWrapper;
import yall.cameras.PhotonVisionWrapper;
import yall.sim.CameraModel;

/**
 * Two cameras that correct the swerve drive's pose with AprilTags, through YALL.
 *
 * <p>The front camera runs PhotonVision. The back camera is a Limelight 4 using MegaTag2, which takes the robot's
 * heading from {@link #update()}. In simulation both are simulated with PhotonVision's simulator, so the example runs
 * without a camera.
 */
public class Vision
{

  private final SwerveDriveMechanism swerve;
  private final VisionSystem         vision;

  /**
   * Create the cameras.
   *
   * @param swerve Swerve drive to correct.
   */
  public Vision(SwerveDriveMechanism swerve)
  {
    this.swerve = swerve;

    // A PhotonVision camera 12 in forward of the robot's center, 10 in up, tilted 15 degrees up.
    VisionCamera front = new PhotonVisionWrapper(
        new PhotonCamera("front"),
        new VisionCameraConfig()
            .withCameraModel(CameraModel.ARDUCAM_OV9281)
            .withRobotToCamera(Inches.of(12), Inches.zero(), Inches.of(10),
                               Degrees.zero(), Degrees.of(-15), Degrees.zero()));

    // A Limelight 4 12 in behind the robot's center, 10 in up, facing backward and tilted 15 degrees up. MegaTag2
    // (HEADING_ASSISTED) uses the heading given in update().
    VisionCamera back = new LimelightWrapper(
        new Limelight("limelight-back"),
        new VisionCameraConfig()
            .withCameraModel(CameraModel.LIMELIGHT_4)
            .withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED)
            .withRobotToCamera(Inches.of(-12), Inches.zero(), Inches.of(10),
                               Degrees.zero(), Degrees.of(-15), Degrees.of(180)));

    vision = new VisionSystem(front, back);
  }

  /**
   * Read the cameras and correct the drive's pose. Call once per loop from {@code Robot.robotPeriodic()}, after the
   * scheduler runs, so the drive's odometry is up to date.
   */
  public void update()
  {
    // The heading is the drive's odometry heading, not the gyro's: resetting odometry (for an auto's starting pose,
    // or with the zero gyro button) turns the odometry heading without turning the gyro.
    vision.setRobotOrientation(swerve.getHeading(), swerve.getYawRate());
    vision.update();
    // Every estimate that passed the pose filter, oldest first, each with its own timestamp and standard deviations.
    vision.forEachEstimatedPose(
        estimate -> swerve.addVisionMeasurement(estimate.pose2d(), estimate.timestampSeconds(), estimate.stdDevs()));
  }

  /**
   * Move the simulated cameras with the simulated robot. Call once per loop from {@code Robot.simulationPeriodic()}.
   */
  public void updateSimulation()
  {
    vision.updateSimulation(swerve.getSimPose());
  }

  /**
   * The cameras, for tests and dashboards.
   *
   * @return The {@link VisionSystem}.
   */
  public VisionSystem getVisionSystem()
  {
    return vision;
  }
}
