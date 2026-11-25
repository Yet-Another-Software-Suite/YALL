package yall;

import static org.wpilib.units.Units.Seconds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.wpilib.math.estimator.PoseEstimator;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform2d;
import org.wpilib.math.geometry.Translation3d;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.linalg.VecBuilder;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.StructPublisher;
import org.wpilib.units.measure.AngularVelocity;
import org.wpilib.units.measure.Time;
import yall.results.VisionPoseEstimate;
import yall.sim.VisionSim;

/**
 * Several {@link VisionCamera}s working together, from any mix of vendors.
 *
 * <pre>{@code
 * VisionSystem vision = new VisionSystem(frontLimelight, backPhoton, sideSynapse);
 *
 * public void periodic() {
 *   vision.setRobotOrientation(gyro.getRotation3d(), gyro.getYawRate());
 *   vision.update();
 *   vision.addVisionMeasurements(poseEstimator);
 * }
 *
 * public void simulationPeriodic() {
 *   vision.updateSimulation(drivetrainSim.getPose());
 * }
 * }</pre>
 *
 * <p>{@link #addVisionMeasurements(PoseEstimator)} is the most accurate way to use several cameras:
 * the pose estimator weighs each estimate at the moment its image was taken. Give the system the
 * drivetrain's pose estimator with {@link #withPoseEstimator(PoseEstimator)} and {@link #update()}
 * does it for you.
 *
 * <p>{@link #getFusedPose()} combines the cameras' newest estimates into one pose, for code that
 * wants a single answer. It uses the same math as WPILib's pose estimators: each estimate moves the
 * fused pose toward itself by the gain {@code q / (q + sqrt(q * r))} on each axis, where {@code q}
 * is the squared {@link #withFusionStateStdDevs state standard deviation} and {@code r} the
 * estimate's squared standard deviation. With a pose estimator given, the fused pose is the pose
 * estimator's, which also has odometry.
 */
public class VisionSystem implements AutoCloseable {

  private final List<VisionCamera> cameras;
  private double maxFusionTimeSkewSeconds = 0.1;
  private Matrix<N3, N1> fusionStateStdDevs = VecBuilder.fill(0.1, 0.1, 0.1);
  private Optional<PoseEstimator<?>> poseEstimator = Optional.empty();
  private List<VisionPoseEstimate> estimates = List.of();
  private final StructPublisher<Pose3d> fusedPublisher;

  /**
   * Creates a vision system.
   *
   * @param cameras The cameras.
   */
  public VisionSystem(VisionCamera... cameras) {
    this.cameras = List.of(cameras);
    fusedPublisher =
        NetworkTableInstance.getDefault()
            .getTable("YALL")
            .getStructTopic("fusedPose", Pose3d.struct)
            .publish();
  }

  /**
   * Sets how far apart in time the cameras' newest estimates can be and still be fused. Older ones
   * are left out of {@link #getFusedPose()}. Defaults to 0.1 s.
   *
   * @param maxSkew Largest time between the newest and oldest fused estimate.
   * @return This system, for chaining.
   */
  public VisionSystem withMaxFusionTimeSkew(Time maxSkew) {
    maxFusionTimeSkewSeconds = maxSkew.in(Seconds);
    return this;
  }

  /**
   * Sets how much {@link #getFusedPose()} trusts the pose it has so far against each new estimate,
   * as a WPILib pose estimator's state standard deviations do. Defaults to 0.1 m, 0.1 m, 0.1 rad,
   * WPILib's default for swerve drives.
   *
   * @param stateStdDevs Standard deviations of x (meters), y (meters) and heading (radians).
   * @return This system, for chaining.
   */
  public VisionSystem withFusionStateStdDevs(Matrix<N3, N1> stateStdDevs) {
    fusionStateStdDevs = stateStdDevs;
    return this;
  }

  /**
   * Gives the system the drivetrain's pose estimator. {@link #update()} then adds every accepted
   * estimate to it, and {@link #getFusedPose()} returns its pose.
   *
   * @param poseEstimator The drivetrain's pose estimator, or null to stop.
   * @return This system, for chaining.
   */
  public VisionSystem withPoseEstimator(PoseEstimator<?> poseEstimator) {
    this.poseEstimator = Optional.ofNullable(poseEstimator);
    return this;
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
    poseEstimator.ifPresent(this::addVisionMeasurements);
    getFusedPose().ifPresent(e -> fusedPublisher.set(e.pose()));
  }

  /**
   * Updates the simulated worlds the cameras are in. Call once per loop from {@code
   * simulationPeriodic}.
   *
   * @param robotPose The robot's true pose.
   */
  public void updateSimulation(Pose2d robotPose) {
    var sims = new LinkedHashSet<VisionSim>();
    cameras.forEach(c -> c.getSimulation().ifPresent(sim -> sims.add(sim.getVisionSim())));
    sims.forEach(sim -> sim.update(robotPose));
  }

  /**
   * Gives every camera the robot's orientation from the gyro.
   *
   * @param orientation Robot orientation on the field.
   * @param yawRate Robot yaw rate.
   * @see VisionCamera#setRobotOrientation(Rotation3d, AngularVelocity)
   */
  public void setRobotOrientation(Rotation3d orientation, AngularVelocity yawRate) {
    cameras.forEach(c -> c.setRobotOrientation(orientation, yawRate));
  }

  /**
   * Gives every camera the robot's heading from the gyro.
   *
   * @param heading Robot heading on the field.
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
   * it for drivetrains with their own vision measurement method.
   *
   * @param consumer Receives each estimate.
   */
  public void forEachEstimatedPose(Consumer<VisionPoseEstimate> consumer) {
    estimates.forEach(consumer);
  }

  /**
   * Adds every accepted estimate from the last {@link #update()} to a WPILib pose estimator, oldest
   * first, with its timestamp and standard deviations.
   *
   * @param poseEstimator The drivetrain's pose estimator.
   */
  public void addVisionMeasurements(PoseEstimator<?> poseEstimator) {
    forEachEstimatedPose(
        e -> poseEstimator.addVisionMeasurement(e.pose2d(), e.timestampSeconds(), e.stdDevs()));
  }

  /**
   * Combines each camera's newest accepted estimate from the last {@link #update()} into one pose.
   * Estimates more than the {@link #withMaxFusionTimeSkew(Time) time skew} older than the newest are
   * left out. With a {@link #withPoseEstimator pose estimator}, returns the pose estimator's pose
   * instead.
   *
   * @return The fused estimate, named "fused", or empty if no camera has one.
   */
  public Optional<VisionPoseEstimate> getFusedPose() {
    var newest = new ArrayList<VisionPoseEstimate>();
    for (VisionCamera camera : cameras) {
      camera.getEstimatedPose().ifPresent(newest::add);
    }
    if (newest.isEmpty()) {
      return Optional.empty();
    }
    double latest =
        newest.stream().mapToDouble(VisionPoseEstimate::timestampSeconds).max().getAsDouble();
    newest.removeIf(e -> latest - e.timestampSeconds() > maxFusionTimeSkewSeconds);
    var fused = fuse(newest, fusionStateStdDevs);
    if (poseEstimator.isPresent()) {
      fused =
          new VisionPoseEstimate(
              "fused",
              new Pose3d(poseEstimator.get().getEstimatedPosition()),
              fused.timestampSeconds(),
              fused.tagIds(),
              fused.averageTagDistanceMeters(),
              fused.ambiguity(),
              fused.headingAssisted(),
              fused.stdDevs());
    }
    return Optional.of(fused);
  }

  /**
   * Combines estimates into one, with WPILib's pose estimator math and its default state standard
   * deviations (0.1 m, 0.1 m, 0.1 rad).
   *
   * @param estimates The estimates. Must not be empty.
   * @return The fused estimate.
   */
  public static VisionPoseEstimate fuse(List<VisionPoseEstimate> estimates) {
    return fuse(estimates, VecBuilder.fill(0.1, 0.1, 0.1));
  }

  /**
   * Combines estimates into one, with WPILib's pose estimator math: starting from the oldest, each
   * estimate moves the fused pose toward itself by the gain {@code q / (q + sqrt(q * r))} on x, y
   * and heading, where {@code q} is the squared state standard deviation and {@code r} the
   * estimate's squared standard deviation. The fused standard deviations are those of independent
   * measurements combined: {@code 1 / sqrt(sum(1 / stdDev²))} on each axis.
   *
   * @param estimates The estimates. Must not be empty.
   * @param stateStdDevs How much to trust the pose so far: x (m), y (m) and heading (rad).
   * @return The fused estimate.
   */
  public static VisionPoseEstimate fuse(
      List<VisionPoseEstimate> estimates, Matrix<N3, N1> stateStdDevs) {
    if (estimates.size() == 1) {
      return estimates.get(0);
    }
    var sorted = new ArrayList<>(estimates);
    sorted.sort(Comparator.comparingDouble(VisionPoseEstimate::timestampSeconds));
    Pose2d pose = sorted.get(0).pose2d();
    double z = 0, roll = 0, pitch = 0;
    double invX = 0, invY = 0, invTheta = 0, distance = 0, distanceWeight = 0;
    var tagIds = new LinkedHashSet<Integer>();
    boolean headingAssisted = true;
    for (int i = 0; i < sorted.size(); i++) {
      var e = sorted.get(i);
      if (i > 0) {
        var transform = e.pose2d().minus(pose);
        pose =
            pose.plus(
                new Transform2d(
                    gain(stateStdDevs.get(0, 0), e.stdDevs().get(0, 0)) * transform.getX(),
                    gain(stateStdDevs.get(1, 0), e.stdDevs().get(1, 0)) * transform.getY(),
                    Rotation2d.fromRadians(
                        gain(stateStdDevs.get(2, 0), e.stdDevs().get(2, 0))
                            * transform.getRotation().getRadians())));
      }
      z += e.pose().getZ() / sorted.size();
      roll += e.pose().getRotation().getX() / sorted.size();
      pitch += e.pose().getRotation().getY() / sorted.size();
      double ix = 1 / square(e.stdDevs().get(0, 0));
      invX += ix;
      invY += 1 / square(e.stdDevs().get(1, 0));
      invTheta += 1 / square(e.stdDevs().get(2, 0));
      if (Double.isFinite(e.averageTagDistanceMeters())) {
        distance += ix * e.averageTagDistanceMeters();
        distanceWeight += ix;
      }
      tagIds.addAll(e.tagIds());
      headingAssisted &= e.headingAssisted();
    }
    var fusedPose =
        new Pose3d(
            new Translation3d(pose.getX(), pose.getY(), z),
            new Rotation3d(roll, pitch, pose.getRotation().getRadians()));
    return new VisionPoseEstimate(
        "fused",
        fusedPose,
        sorted.get(sorted.size() - 1).timestampSeconds(),
        List.copyOf(tagIds),
        distanceWeight > 0 ? distance / distanceWeight : Double.NaN,
        0,
        headingAssisted,
        VecBuilder.fill(1 / Math.sqrt(invX), 1 / Math.sqrt(invY), 1 / Math.sqrt(invTheta)));
  }

  /** WPILib's pose estimator gain for one axis, from standard deviations. */
  private static double gain(double stateStdDev, double visionStdDev) {
    double q = square(stateStdDev);
    if (q == 0) {
      return 0;
    }
    return q / (q + Math.sqrt(q * square(visionStdDev)));
  }

  private static double square(double value) {
    return value * value;
  }

  /** Closes every camera and the fused pose telemetry. */
  @Override
  public void close() {
    cameras.forEach(VisionCamera::close);
    fusedPublisher.close();
  }
}
