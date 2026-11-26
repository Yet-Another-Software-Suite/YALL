package yall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import org.wpilib.math.estimator.PoseEstimator;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.units.measure.AngularVelocity;
import yall.results.VisionPoseEstimate;
import yall.sim.VisionSim;

/**
 * Several {@link VisionCamera}s working together, from any mix of vendors.
 *
 * <pre>{@code
 * VisionSystem vision = new VisionSystem(frontLimelight, backPhoton, sideSynapse);
 *
 * public void periodic() {
 *   vision.setRobotOrientation(poseEstimator.getEstimatedPosition().getRotation(), gyro.getYawRate());
 *   vision.update();
 *   vision.addVisionMeasurements(poseEstimator);
 * }
 *
 * public void simulationPeriodic() {
 *   vision.updateSimulation(drivetrainSim.getPose());
 * }
 * }</pre>
 *
 * <p>Add every estimate to your drivetrain's pose estimator with {@link
 * #addVisionMeasurements(PoseEstimator)}, or with {@link #forEachEstimatedPose(Consumer)} for a
 * drivetrain with its own {@code addVisionMeasurement} method. Each estimate goes in on its own, with
 * the time its image was taken and its own standard deviations, so the pose estimator applies it
 * against the odometry from that moment and trusts each camera as much as it should. Don't combine
 * the cameras' estimates first: that loses their timestamps and standard deviations.
 */
public class VisionSystem implements AutoCloseable {

  private final List<VisionCamera> cameras;
  private List<VisionPoseEstimate> estimates = List.of();

  /**
   * Creates a vision system.
   *
   * @param cameras The cameras.
   */
  public VisionSystem(VisionCamera... cameras) {
    this.cameras = List.of(cameras);
  }

  /** Updates every camera. Call once per loop. */
  public void update() {
    var all = new ArrayList<VisionPoseEstimate>();
    for (VisionCamera camera : cameras) {
      camera.update();
      all.addAll(camera.getEstimatedPoses());
    }
    all.sort(Comparator.comparingDouble(VisionPoseEstimate::timestampSeconds));
    estimates = List.copyOf(all);
  }

  /**
   * Updates the simulated worlds the cameras are in. Call once per loop from {@code
   * simulationPeriodic}.
   *
   * @param robotPose The robot's true pose.
   */
  public void updateSimulation(Pose2d robotPose) {
    var sims = new LinkedHashSet<VisionSim>();
    cameras.forEach(c -> c.getVisionSim().ifPresent(sims::add));
    sims.forEach(sim -> sim.update(robotPose));
  }

  /**
   * Gives every camera the robot's orientation on the field: the yaw from your pose estimator, the
   * roll and pitch from the gyro.
   *
   * @param orientation Robot orientation on the field.
   * @param yawRate Robot yaw rate.
   * @see VisionCamera#setRobotOrientation(Rotation3d, AngularVelocity)
   */
  public void setRobotOrientation(Rotation3d orientation, AngularVelocity yawRate) {
    cameras.forEach(c -> c.setRobotOrientation(orientation, yawRate));
  }

  /**
   * Gives every camera the robot's heading on the field and its yaw rate.
   *
   * @param heading Robot heading on the field, your pose estimator's rotation.
   * @param yawRate Robot yaw rate.
   * @see VisionCamera#setRobotOrientation(Rotation2d, AngularVelocity)
   */
  public void setRobotOrientation(Rotation2d heading, AngularVelocity yawRate) {
    cameras.forEach(c -> c.setRobotOrientation(heading, yawRate));
  }

  /**
   * Gives every camera the robot's heading on the field, without a yaw rate.
   *
   * @param heading Robot heading on the field, your pose estimator's rotation.
   * @see VisionCamera#setRobotOrientation(Rotation2d)
   */
  public void setRobotOrientation(Rotation2d heading) {
    cameras.forEach(c -> c.setRobotOrientation(heading));
  }

  /**
   * The cameras in this system.
   *
   * @return The cameras.
   */
  public List<VisionCamera> getCameras() {
    return cameras;
  }

  /**
   * Every accepted estimate from every camera in the last {@link #update()}.
   *
   * @return The estimates, oldest first.
   */
  public List<VisionPoseEstimate> getEstimatedPoses() {
    return estimates;
  }

  /**
   * Passes every accepted estimate from the last {@link #update()} to a consumer, oldest first. Use
   * it for drivetrains with their own vision measurement method, passing each estimate's {@link
   * VisionPoseEstimate#pose2d() pose}, {@link VisionPoseEstimate#timestampSeconds() timestamp} and
   * {@link VisionPoseEstimate#stdDevs() standard deviations}.
   *
   * @param consumer Receives each estimate.
   */
  public void forEachEstimatedPose(Consumer<VisionPoseEstimate> consumer) {
    estimates.forEach(consumer);
  }

  /**
   * Adds every accepted estimate from the last {@link #update()} to a WPILib pose estimator, oldest
   * first, with its timestamp and standard deviations. Call it once per loop, after {@link
   * #update()}.
   *
   * @param poseEstimator The drivetrain's pose estimator.
   */
  public void addVisionMeasurements(PoseEstimator<?> poseEstimator) {
    forEachEstimatedPose(
        e -> poseEstimator.addVisionMeasurement(e.pose2d(), e.timestampSeconds(), e.stdDevs()));
  }

  /** Closes every camera. */
  @Override
  public void close() {
    cameras.forEach(VisionCamera::close);
  }
}
