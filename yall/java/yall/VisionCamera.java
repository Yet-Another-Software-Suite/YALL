package yall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.photonvision.EstimatedRobotPose;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.simulation.PhotonCameraSim;
import org.photonvision.simulation.SimCameraProperties;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.wpilib.framework.RobotBase;
import org.wpilib.math.estimator.PoseEstimator;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import org.wpilib.networktables.BooleanPublisher;
import org.wpilib.networktables.DoublePublisher;
import org.wpilib.networktables.IntegerPublisher;
import org.wpilib.networktables.NetworkTable;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.StringPublisher;
import org.wpilib.networktables.StructArrayPublisher;
import org.wpilib.system.Timer;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.AngularVelocity;
import org.wpilib.units.measure.Distance;
import yall.results.VisionPoseEstimate;
import yall.results.VisionResult;
import yall.results.VisionTarget;
import yall.sim.VisionSim;

/**
 * A camera from any supported vendor, with one API for targets and robot pose estimates. On a real
 * robot it reads the vendor's library; in simulation it reads a PhotonVision camera simulation, so the
 * same robot code runs in both.
 *
 * <p>Create one with the wrapper for the camera's vendor:
 *
 * <pre>{@code
 * VisionCamera front = new LimelightWrapper(new Limelight("limelight-front"), frontConfig);
 * VisionCamera back = new PhotonVisionWrapper(new PhotonCamera("back"), backConfig);
 * VisionCamera side = new SynapseWrapper(new SynapseCamera("side"), sideConfig);
 * }</pre>
 *
 * <p>Call {@link #update()} once per loop (or {@link VisionSystem#update()} for several cameras),
 * then read {@link #getLatestResult()}, {@link #getEstimatedPose()} and the other getters. They all
 * report what the last {@link #update()} read.
 */
public abstract class VisionCamera implements AutoCloseable {

  /**
   * One frame read from the camera, before the pose filter.
   *
   * @param result The frame's targets.
   * @param estimate The robot pose estimated from the frame, if any.
   * @param vendorRejection Why the vendor's own filter rejected the estimate, if it did.
   * @param vendorStdDevs Standard deviations from the vendor, used instead of the filter's.
   */
  public record Frame(
      VisionResult result,
      Optional<VisionPoseEstimate> estimate,
      Optional<String> vendorRejection,
      Optional<Matrix<N3, N1>> vendorStdDevs) {

    /**
     * A frame with no vendor filtering.
     *
     * @param result The frame's targets.
     * @param estimate The robot pose estimated from the frame, if any.
     */
    public Frame(VisionResult result, Optional<VisionPoseEstimate> estimate) {
      this(result, estimate, Optional.empty(), Optional.empty());
    }
  }

  /** Name of the camera. */
  protected final String name;

  /** The camera's config. */
  protected final VisionCameraConfig config;

  private final PhotonPoseEstimator photonPoseEstimator;
  private final boolean simulated;
  private Transform3d robotToCamera;
  private PhotonCamera simCamera;
  private boolean ownsSimCamera;
  private PhotonCameraSim cameraSim;
  private VisionSim visionSim;
  private int simPipelineIndex;

  private List<VisionResult> results = List.of();
  private Optional<VisionResult> latestResult = Optional.empty();
  private List<VisionPoseEstimate> acceptedPoses = List.of();
  private List<VisionPoseEstimate> rejectedPoses = List.of();
  private String lastRejectionReason = "";
  private double yawRateDegreesPerSecond = Double.NaN;

  private StructArrayPublisher<Pose3d> acceptedPublisher;
  private StructArrayPublisher<Pose3d> rejectedPublisher;
  private StringPublisher rejectionPublisher;
  private BooleanPublisher hasTargetsPublisher;
  private IntegerPublisher targetCountPublisher;
  private DoublePublisher latencyPublisher;
  private BooleanPublisher connectedPublisher;

  /**
   * Sets up the parts every camera shares: the pose filter, PhotonVision's pose estimator (used by
   * PhotonVision cameras and by every simulated camera) and, in simulation, the simulated camera.
   *
   * @param name Name of the camera.
   * @param config The camera's config.
   * @param cameraForSimulation PhotonVision camera that the simulation publishes to, or null to create
   *     a private one.
   */
  protected VisionCamera(String name, VisionCameraConfig config, PhotonCamera cameraForSimulation) {
    this.name = name;
    this.config = config;
    robotToCamera = config.getRobotToCamera().orElse(Transform3d.ZERO);
    photonPoseEstimator = new PhotonPoseEstimator(config.getField(), robotToCamera);
    simulated = RobotBase.isSimulation() && config.isSimulationEnabled();
    if (simulated) {
      startSimulation(cameraForSimulation);
    }
    if (config.isTelemetryEnabled()) {
      startTelemetry();
    }
  }

  private void startSimulation(PhotonCamera cameraForSimulation) {
    ownsSimCamera = cameraForSimulation == null;
    simCamera = ownsSimCamera ? new PhotonCamera("YALL-" + name) : cameraForSimulation;
    var properties = config.getCameraProperties().orElseGet(this::defaultCameraProperties);
    cameraSim = new PhotonCameraSim(simCamera, properties, config.getField());
    cameraSim.enableRawStream(config.isSimulatedVideoStreamsEnabled());
    cameraSim.enableProcessedStream(config.isSimulatedVideoStreamsEnabled());
    visionSim = config.getVisionSim().orElseGet(VisionSim::getDefault);
    visionSim.addCamera(cameraSim, robotToCamera);
  }

  private void startTelemetry() {
    NetworkTable table = NetworkTableInstance.getDefault().getTable("YALL").getSubTable(name);
    acceptedPublisher = table.getStructArrayTopic("acceptedPoses", Pose3d.struct).publish();
    rejectedPublisher = table.getStructArrayTopic("rejectedPoses", Pose3d.struct).publish();
    rejectionPublisher = table.getStringTopic("rejectionReason").publish();
    hasTargetsPublisher = table.getBooleanTopic("hasTargets").publish();
    targetCountPublisher = table.getIntegerTopic("targetCount").publish();
    latencyPublisher = table.getDoubleTopic("latencyMillis").publish();
    connectedPublisher = table.getBooleanTopic("connected").publish();
  }

  /**
   * Reads the frames the camera produced since the last call. Call once per loop.
   *
   * @return The frames, oldest first.
   */
  protected abstract List<Frame> readFrames();

  /**
   * Switches the real camera's pipeline.
   *
   * @param index Pipeline index.
   */
  protected abstract void applyPipelineIndex(int index);

  /**
   * Reads the real camera's pipeline.
   *
   * @return Pipeline index.
   */
  protected abstract int readPipelineIndex();

  /**
   * Whether the real camera is sending data.
   *
   * @return true if connected.
   */
  protected abstract boolean readConnected();

  /**
   * Camera properties to simulate when the config doesn't set any. Called from the constructor, so
   * it must not use the subclass's fields.
   *
   * @return Camera properties.
   */
  protected abstract SimCameraProperties defaultCameraProperties();

  /**
   * Passes the robot orientation to the real camera, for cameras that use it themselves.
   *
   * @param orientation Robot orientation on the field.
   * @param yawRateDegreesPerSecond Yaw rate, or NaN if unknown.
   */
  protected void applyRobotOrientation(Rotation3d orientation, double yawRateDegreesPerSecond) {}

  /**
   * Passes a new camera position to the real camera, for cameras that use it themselves.
   *
   * @param robotToCamera Transform from the robot origin to the camera.
   */
  protected void applyRobotToCamera(Transform3d robotToCamera) {}

  /**
   * The vendor's camera object, for settings YALL doesn't cover. Each wrapper narrows the return
   * type to its vendor's class.
   *
   * @return The vendor's camera object.
   */
  public abstract Object getCamera();

  /**
   * Reads new frames from the camera (or its simulation), filters their pose estimates and publishes
   * telemetry. Call once per loop, before the getters.
   */
  public void update() {
    List<Frame> frames = simulated ? readPhotonFrames(simCamera) : readFrames();
    var field = config.getField();
    var filter = config.getPoseFilter();
    double now = Timer.getMonotonicTimestamp();

    var newResults = new ArrayList<VisionResult>(frames.size());
    var accepted = new ArrayList<VisionPoseEstimate>();
    var rejected = new ArrayList<VisionPoseEstimate>();
    for (Frame frame : frames) {
      newResults.add(frame.result());
      if (frame.estimate().isEmpty()) {
        continue;
      }
      var estimate = frame.estimate().get();
      var reason =
          frame
              .vendorRejection()
              .or(() -> filter.check(estimate, field, now, yawRateDegreesPerSecond));
      if (reason.isPresent()) {
        rejected.add(estimate);
        lastRejectionReason = reason.get();
      } else {
        accepted.add(
            frame
                .vendorStdDevs()
                .map(estimate::withStdDevs)
                .orElseGet(() -> filter.applyStdDevs(estimate)));
      }
    }
    results = List.copyOf(newResults);
    if (!results.isEmpty()) {
      latestResult = Optional.of(results.get(results.size() - 1));
    }
    accepted.sort(Comparator.comparingDouble(VisionPoseEstimate::timestampSeconds));
    acceptedPoses = List.copyOf(accepted);
    rejectedPoses = List.copyOf(rejected);
    publishTelemetry();
  }

  private void publishTelemetry() {
    if (acceptedPublisher == null) {
      return;
    }
    acceptedPublisher.set(
        acceptedPoses.stream().map(VisionPoseEstimate::pose).toArray(Pose3d[]::new));
    rejectedPublisher.set(
        rejectedPoses.stream().map(VisionPoseEstimate::pose).toArray(Pose3d[]::new));
    rejectionPublisher.set(lastRejectionReason);
    hasTargetsPublisher.set(hasTargets());
    targetCountPublisher.set(getTargets().size());
    latestResult.ifPresent(r -> latencyPublisher.set(r.latencyMillis()));
    connectedPublisher.set(isConnected());
  }

  /**
   * Reads and converts the unread results of a PhotonVision camera. PhotonVision cameras use this on
   * real robots, and every camera uses it in simulation.
   *
   * @param camera The PhotonVision camera.
   * @return The frames, oldest first.
   */
  protected final List<Frame> readPhotonFrames(PhotonCamera camera) {
    var frames = new ArrayList<Frame>();
    for (PhotonPipelineResult result : camera.getAllUnreadResults()) {
      frames.add(new Frame(toVisionResult(result), estimatePhotonPose(result)));
    }
    return frames;
  }

  private VisionResult toVisionResult(PhotonPipelineResult result) {
    var targets = new ArrayList<VisionTarget>(result.getTargets().size());
    for (PhotonTrackedTarget target : result.getTargets()) {
      boolean isTag = target.getFiducialId() >= 0;
      targets.add(
          new VisionTarget(
              target.getYaw(),
              target.getPitch(),
              target.getArea(),
              target.getFiducialId(),
              target.getDetectedObjectClassID(),
              target.getDetectedObjectConfidence(),
              isTag && target.getPoseAmbiguity() >= 0 ? target.getPoseAmbiguity() : Double.NaN,
              isTag ? Optional.of(target.getBestCameraToTarget()) : Optional.empty()));
    }
    return new VisionResult(
        name, result.getTimestampSeconds(), result.metadata.getLatencyMillis(), targets);
  }

  private Optional<VisionPoseEstimate> estimatePhotonPose(PhotonPipelineResult result) {
    if (!result.hasTargets()) {
      return Optional.empty();
    }
    boolean headingAssisted = config.getPoseEstimationMode() == PoseEstimationMode.HEADING_ASSISTED;
    Optional<EstimatedRobotPose> estimate = Optional.empty();
    if (headingAssisted) {
      estimate = photonPoseEstimator.estimatePnpDistanceTrigSolvePose(result);
    }
    if (estimate.isEmpty()) {
      headingAssisted = false;
      estimate = photonPoseEstimator.estimateCoprocMultiTagPose(result);
    }
    if (estimate.isEmpty()) {
      estimate = photonPoseEstimator.estimateLowestAmbiguityPose(result);
    }
    if (estimate.isEmpty()) {
      return Optional.empty();
    }
    var used = estimate.get().targetsUsed.stream().filter(t -> t.getFiducialId() >= 0).toList();
    var tagIds = used.stream().map(PhotonTrackedTarget::getFiducialId).toList();
    double distance =
        used.stream()
            .mapToDouble(t -> t.getBestCameraToTarget().getTranslation().getNorm())
            .average()
            .orElse(Double.NaN);
    double ambiguity =
        used.size() == 1 && used.get(0).getPoseAmbiguity() >= 0
            ? used.get(0).getPoseAmbiguity()
            : used.size() == 1 ? Double.NaN : 0;
    return Optional.of(
        VisionPoseEstimate.unfiltered(
            name,
            estimate.get().estimatedPose,
            estimate.get().timestampSeconds,
            tagIds,
            distance,
            ambiguity,
            headingAssisted));
  }

  /**
   * Gives the camera the robot's orientation from the gyro. {@link
   * PoseEstimationMode#HEADING_ASSISTED} needs it every loop, and the pose filter's yaw rate check
   * uses the yaw rate.
   *
   * @param orientation Robot orientation on the field (blue-origin, counterclockwise positive yaw).
   * @param yawRate Robot yaw rate.
   */
  public void setRobotOrientation(Rotation3d orientation, AngularVelocity yawRate) {
    setRobotOrientation(orientation, yawRate.in(org.wpilib.units.Units.DegreesPerSecond));
  }

  /**
   * Gives the camera the robot's heading from the gyro, without a yaw rate. The pose filter's yaw
   * rate check is skipped.
   *
   * @param heading Robot heading on the field.
   */
  public void setRobotOrientation(Rotation2d heading) {
    setRobotOrientation(new Rotation3d(heading), Double.NaN);
  }

  private void setRobotOrientation(Rotation3d orientation, double yawRateDegreesPerSecond) {
    this.yawRateDegreesPerSecond = yawRateDegreesPerSecond;
    photonPoseEstimator.addHeadingData(Timer.getMonotonicTimestamp(), orientation);
    if (!simulated) {
      applyRobotOrientation(orientation, yawRateDegreesPerSecond);
    }
  }

  /**
   * Moves the camera on the robot, for a camera on a turret or other moving mechanism.
   *
   * @param robotToCamera Transform from the robot origin to the camera.
   */
  public void setRobotToCamera(Transform3d robotToCamera) {
    this.robotToCamera = robotToCamera;
    photonPoseEstimator.setRobotToCameraTransform(robotToCamera);
    if (simulated) {
      visionSim.adjustCamera(cameraSim, robotToCamera);
    } else {
      applyRobotToCamera(robotToCamera);
    }
  }

  /**
   * Moves the camera on the robot, for a camera on a turret or other moving mechanism. Takes the
   * same measurements as {@link VisionCameraConfig#withRobotToCamera(Distance, Distance, Distance,
   * Angle, Angle, Angle)}.
   *
   * @param forward Distance forward of the robot center.
   * @param left Distance left of the robot center.
   * @param up Height above the floor.
   * @param roll Rotation about the camera's forward axis.
   * @param pitch Rotation about the left axis. Negative tilts the camera up.
   * @param yaw Rotation about the up axis, counterclockwise positive.
   */
  public void setRobotToCamera(
      Distance forward, Distance left, Distance up, Angle roll, Angle pitch, Angle yaw) {
    setRobotToCamera(VisionCameraConfig.robotToCamera(forward, left, up, roll, pitch, yaw));
  }

  /**
   * Where the camera is on the robot.
   *
   * @return Transform from the robot origin to the camera.
   */
  public Transform3d getRobotToCamera() {
    return robotToCamera;
  }

  /**
   * Switches the camera's pipeline.
   *
   * @param index Pipeline index.
   */
  public void setPipelineIndex(int index) {
    if (simulated) {
      simPipelineIndex = index;
    } else {
      applyPipelineIndex(index);
    }
  }

  /**
   * The camera's current pipeline.
   *
   * @return Pipeline index.
   */
  public int getPipelineIndex() {
    return simulated ? simPipelineIndex : readPipelineIndex();
  }

  /**
   * Whether the camera is sending data. Simulated cameras are always connected.
   *
   * @return true if connected.
   */
  public boolean isConnected() {
    return simulated || readConnected();
  }

  /**
   * Whether this camera is giving simulated results.
   *
   * @return true in simulation with simulation enabled in the config.
   */
  public boolean isSimulated() {
    return simulated;
  }

  /**
   * Name of the camera.
   *
   * @return The name.
   */
  public String getName() {
    return name;
  }

  /**
   * The camera's config.
   *
   * @return The config.
   */
  public VisionCameraConfig getConfig() {
    return config;
  }

  /**
   * The frames read by the last {@link #update()}.
   *
   * @return The frames, oldest first.
   */
  public List<VisionResult> getUnreadResults() {
    return results;
  }

  /**
   * The newest frame the camera has produced, even if the last {@link #update()} read none.
   *
   * @return The newest frame, or empty if there hasn't been one.
   */
  public Optional<VisionResult> getLatestResult() {
    return latestResult;
  }

  /**
   * Whether the newest frame has targets.
   *
   * @return true if it has targets.
   */
  public boolean hasTargets() {
    return latestResult.map(VisionResult::hasTargets).orElse(false);
  }

  /**
   * The targets in the newest frame.
   *
   * @return The targets, best first.
   */
  public List<VisionTarget> getTargets() {
    return latestResult.map(VisionResult::targets).orElse(List.of());
  }

  /**
   * The best target in the newest frame.
   *
   * @return The best target, if any.
   */
  public Optional<VisionTarget> getBestTarget() {
    return latestResult.flatMap(VisionResult::getBestTarget);
  }

  /**
   * The newest robot pose estimate from the last {@link #update()} that passed the pose filter.
   *
   * @return The estimate with its timestamp and standard deviations, or empty if none passed.
   */
  public Optional<VisionPoseEstimate> getEstimatedPose() {
    return acceptedPoses.isEmpty()
        ? Optional.empty()
        : Optional.of(acceptedPoses.get(acceptedPoses.size() - 1));
  }

  /**
   * Every robot pose estimate from the last {@link #update()} that passed the pose filter.
   *
   * @return The estimates, oldest first.
   */
  public List<VisionPoseEstimate> getEstimatedPoses() {
    return acceptedPoses;
  }

  /**
   * The robot pose estimates from the last {@link #update()} that the pose filter rejected.
   *
   * @return The rejected estimates.
   */
  public List<VisionPoseEstimate> getRejectedPoses() {
    return rejectedPoses;
  }

  /**
   * Why the pose filter last rejected an estimate.
   *
   * @return The reason, or an empty string if it hasn't rejected any.
   */
  public String getLastRejectionReason() {
    return lastRejectionReason;
  }

  /**
   * Passes every accepted estimate from the last {@link #update()} to a consumer, oldest first.
   *
   * @param consumer Receives each estimate.
   */
  public void forEachEstimatedPose(Consumer<VisionPoseEstimate> consumer) {
    acceptedPoses.forEach(consumer);
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
   * The PhotonVision simulation of this camera.
   *
   * @return The simulated camera, or empty when not simulating.
   */
  public Optional<PhotonCameraSim> getCameraSim() {
    return Optional.ofNullable(cameraSim);
  }

  /**
   * The simulated world this camera is in.
   *
   * @return The world, or empty when not simulating.
   */
  public Optional<VisionSim> getVisionSim() {
    return Optional.ofNullable(visionSim);
  }

  /**
   * The PhotonVision pose estimator this camera uses in simulation (and on PhotonVision cameras), for
   * settings YALL doesn't cover.
   *
   * @return PhotonVision's pose estimator.
   */
  public PhotonPoseEstimator getPhotonPoseEstimator() {
    return photonPoseEstimator;
  }

  /** Removes the camera from simulation and closes its telemetry. */
  @Override
  public void close() {
    if (cameraSim != null) {
      visionSim.removeCamera(cameraSim);
      cameraSim.close();
      if (ownsSimCamera) {
        simCamera.close();
      }
    }
    if (acceptedPublisher != null) {
      acceptedPublisher.close();
      rejectedPublisher.close();
      rejectionPublisher.close();
      hasTargetsPublisher.close();
      targetCountPublisher.close();
      latencyPublisher.close();
      connectedPublisher.close();
    }
  }
}
