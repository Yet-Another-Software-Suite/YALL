package yall;

import static org.wpilib.units.Units.DegreesPerSecond;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.simulation.PhotonCameraSim;
import org.wpilib.datalog.BooleanLogEntry;
import org.wpilib.datalog.DataLog;
import org.wpilib.datalog.DataLogEntry;
import org.wpilib.datalog.DoubleArrayLogEntry;
import org.wpilib.datalog.DoubleLogEntry;
import org.wpilib.datalog.IntegerArrayLogEntry;
import org.wpilib.datalog.IntegerLogEntry;
import org.wpilib.datalog.StringLogEntry;
import org.wpilib.datalog.StructArrayLogEntry;
import org.wpilib.datalog.StructLogEntry;
import org.wpilib.math.estimator.PoseEstimator;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.networktables.BooleanEntry;
import org.wpilib.networktables.NetworkTable;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.system.DataLogManager;
import org.wpilib.system.Timer;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.AngularVelocity;
import org.wpilib.units.measure.Distance;
import yall.cameras.PhotonVisionFrames;
import yall.filter.PoseFilterStats;
import yall.results.VisionPoseEstimate;
import yall.results.VisionResult;
import yall.results.VisionTarget;
import yall.sim.VisionSim;
import yall.telemetry.VisionTelemetryConfig;
import yall.telemetry.VisionTelemetryField;

/**
 * A camera from any supported vendor, with one API for targets and robot pose estimates. On a real
 * robot it reads the vendor's library; in simulation it reads a PhotonVision camera simulation in a
 * {@link VisionSim} world, so the same robot code runs in both.
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

  /** Name of the camera. */
  protected final String name;

  /** The camera's config. */
  protected final VisionCameraConfig config;

  private final PhotonCameraSim cameraSim;
  private final VisionSim visionSim;
  private final PhotonPoseEstimator simPoseEstimator;
  private int simPipelineIndex;
  private Transform3d robotToCamera;

  private List<VisionResult> results = List.of();
  private Optional<VisionResult> latestResult = Optional.empty();
  private List<VisionPoseEstimate> acceptedPoses = List.of();
  private List<VisionPoseEstimate> rejectedPoses = List.of();
  private String lastRejectionReason = "";
  private double yawRateDegreesPerSecond = Double.NaN;
  private final PoseFilterStats filterStats = new PoseFilterStats();

  private final Telemetry telemetry;

  /**
   * Sets up the parts every camera shares.
   *
   * @param name Name of the camera.
   * @param config The camera's config.
   * @param cameraSim The camera's PhotonVision simulation, from {@link VisionSim#createCameraSim},
   *     or null when not simulating.
   */
  protected VisionCamera(String name, VisionCameraConfig config, PhotonCameraSim cameraSim) {
    this.name = name;
    this.config = config;
    this.cameraSim = cameraSim;
    robotToCamera = config.getRobotToCamera().orElse(Transform3d.ZERO);
    if (cameraSim != null) {
      visionSim = config.getVisionSim().orElseGet(VisionSim::getDefault);
      simPoseEstimator = new PhotonPoseEstimator(config.getField(), robotToCamera);
    } else {
      visionSim = null;
      simPoseEstimator = null;
    }
    telemetry = config.getTelemetryConfig().map(t -> new Telemetry(name, t)).orElse(null);
  }

  /**
   * Reads the frames the real camera produced since the last call. Call once per loop.
   *
   * @return The frames, oldest first.
   */
  protected abstract List<VisionFrame> readFrames();

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
   * Passes the robot orientation to the real camera, for cameras that use it themselves.
   *
   * @param timestampSeconds When the orientation was measured.
   * @param orientation Robot orientation on the field.
   * @param yawRateDegreesPerSecond Yaw rate, or NaN if unknown.
   */
  protected void applyRobotOrientation(
      double timestampSeconds, Rotation3d orientation, double yawRateDegreesPerSecond) {}

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
   * Reads new frames from the camera (or its simulation), filters their pose estimates, counts the
   * filter's decisions and publishes telemetry. Call once per loop, before the getters.
   */
  public void update() {
    List<VisionFrame> frames =
        cameraSim != null
            ? PhotonVisionFrames.read(
                name, cameraSim.getCamera(), simPoseEstimator, config.getPoseEstimationMode())
            : readFrames();
    var field = config.getField();
    var filter = config.getPoseFilter();
    double now = Timer.getMonotonicTimestamp();

    var newResults = new ArrayList<VisionResult>(frames.size());
    var accepted = new ArrayList<VisionPoseEstimate>();
    var rejected = new ArrayList<VisionPoseEstimate>();
    for (VisionFrame frame : frames) {
      if (cameraSim != null && visionSim != null && visionSim.shouldDropFrame(now)) {
        continue;
      }
      newResults.add(frame.result());
      if (frame.estimate().isEmpty()) {
        continue;
      }
      var rawEstimate = frame.estimate().get();
      var estimate =
          (cameraSim != null && visionSim != null)
              ? new VisionPoseEstimate(
                  rawEstimate.cameraName(),
                  visionSim.applyFieldTagJitter(rawEstimate.pose()),
                  rawEstimate.timestampSeconds(),
                  rawEstimate.tagIds(),
                  rawEstimate.averageTagDistanceMeters(),
                  rawEstimate.ambiguity(),
                  rawEstimate.headingAssisted(),
                  rawEstimate.stdDevs())
              : rawEstimate;
      double avgTagArea = averageAprilTagAreaPercent(frame.result());
      var reason =
          frame
              .vendorRejection()
              .or(() -> filter.check(estimate, field, now, yawRateDegreesPerSecond, avgTagArea));
      if (reason.isPresent()) {
        rejected.add(estimate);
        lastRejectionReason = reason.get();
        filterStats.recordRejected(reason.get());
      } else {
        var weighed =
            frame
                .vendorStdDevs()
                .map(estimate::withStdDevs)
                .orElseGet(() -> filter.applyStdDevs(estimate));
        accepted.add(weighed);
        filterStats.recordAccepted(weighed);
      }
    }
    results = List.copyOf(newResults);
    if (!results.isEmpty()) {
      latestResult = Optional.of(results.get(results.size() - 1));
    }
    accepted.sort(Comparator.comparingDouble(VisionPoseEstimate::timestampSeconds));
    acceptedPoses = List.copyOf(accepted);
    rejectedPoses = List.copyOf(rejected);
    if (telemetry != null) {
      telemetry.publish(this);
    }
  }

  /**
   * Gives the camera the robot's orientation on the field. {@link
   * PoseEstimationMode#HEADING_ASSISTED} needs it every loop, and the pose filter's yaw rate check
   * uses the yaw rate.
   *
   * <p>The yaw must be the robot's heading on the field, your pose estimator's rotation, not the
   * gyro's own reading: resetting the pose estimator turns its heading without turning the gyro.
   * The roll and pitch come from the gyro.
   *
   * @param orientation Robot orientation on the field (blue-origin, counterclockwise positive yaw).
   * @param yawRate Robot yaw rate.
   */
  public void setRobotOrientation(Rotation3d orientation, AngularVelocity yawRate) {
    setRobotOrientation(orientation, yawRate.in(DegreesPerSecond));
  }

  /**
   * Gives the camera the robot's heading on the field and its yaw rate, with no roll or pitch.
   *
   * @param heading Robot heading on the field, your pose estimator's rotation.
   * @param yawRate Robot yaw rate.
   * @see #setRobotOrientation(Rotation3d, AngularVelocity)
   */
  public void setRobotOrientation(Rotation2d heading, AngularVelocity yawRate) {
    setRobotOrientation(new Rotation3d(heading), yawRate.in(DegreesPerSecond));
  }

  /**
   * Gives the camera the robot's heading on the field, without a yaw rate. The pose filter's yaw
   * rate check is skipped.
   *
   * @param heading Robot heading on the field, your pose estimator's rotation.
   */
  public void setRobotOrientation(Rotation2d heading) {
    setRobotOrientation(new Rotation3d(heading), Double.NaN);
  }

  private void setRobotOrientation(Rotation3d orientation, double yawRateDegreesPerSecond) {
    this.yawRateDegreesPerSecond = yawRateDegreesPerSecond;
    double now = Timer.getMonotonicTimestamp();
    config.getPoseFilter().recordYawRate(now, yawRateDegreesPerSecond);
    if (cameraSim != null) {
      simPoseEstimator.addHeadingData(now, orientation);
    } else {
      applyRobotOrientation(now, orientation, yawRateDegreesPerSecond);
    }
  }

  /**
   * Moves the camera on the robot, for a camera on a turret or other moving mechanism.
   *
   * @param robotToCamera Transform from the robot origin to the camera.
   */
  public void setRobotToCamera(Transform3d robotToCamera) {
    this.robotToCamera = robotToCamera;
    if (cameraSim != null) {
      simPoseEstimator.setRobotToCameraTransform(robotToCamera);
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
   * Where the camera itself is on the field, worked out from the AprilTags in the newest frame
   * alone. It doesn't use the robot pose or the camera offset, so it can measure them: for example
   * a turret's angle, with {@link #getCameraYawOnRobot(Rotation2d)}.
   *
   * <p>Each AprilTag the camera solved gives a camera pose: the tag's place on the field, moved back
   * along the camera-to-tag transform. The closest tag's is returned, because it is the most
   * accurate.
   *
   * @return The camera's pose on the field, or empty if the newest frame has no solved AprilTag on
   *     the field.
   */
  public Optional<Pose3d> getCameraFieldPose() {
    var field = config.getField();
    Pose3d best = null;
    double bestDistance = Double.POSITIVE_INFINITY;
    for (VisionTarget target : getTargets()) {
      if (!target.isAprilTag() || target.cameraToTarget().isEmpty()) {
        continue;
      }
      var tagPose = field.getTagPose(target.fiducialId());
      if (tagPose.isEmpty()) {
        continue;
      }
      var cameraToTag = target.cameraToTarget().get();
      double distance = cameraToTag.getTranslation().getNorm();
      if (distance < bestDistance) {
        bestDistance = distance;
        best = tagPose.get().transformBy(cameraToTag.inverse());
      }
    }
    return Optional.ofNullable(best);
  }

  /**
   * Which way the camera points relative to the robot, from what it sees: the camera's heading on
   * the field ({@link #getCameraFieldPose()}) minus the robot's heading. For a camera on a turret,
   * this is the turret's angle (plus any angle the camera is mounted at on the turret), so it can
   * check or zero the turret's encoder.
   *
   * @param robotHeading The robot's heading on the field, your pose estimator's rotation.
   * @return The camera's yaw on the robot, counterclockwise positive, or empty if the newest frame
   *     has no solved AprilTag on the field.
   */
  public Optional<Rotation2d> getCameraYawOnRobot(Rotation2d robotHeading) {
    return getCameraFieldPose().map(pose -> pose.getRotation().toRotation2d().minus(robotHeading));
  }

  /**
   * Switches the camera's pipeline.
   *
   * @param index Pipeline index.
   */
  public void setPipelineIndex(int index) {
    if (cameraSim != null) {
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
    return cameraSim != null ? simPipelineIndex : readPipelineIndex();
  }

  /**
   * Whether the camera is sending data. Simulated cameras are always connected.
   *
   * @return true if connected.
   */
  public boolean isConnected() {
    return cameraSim != null || readConnected();
  }

  /**
   * Whether this camera is giving simulated results.
   *
   * @return true in simulation with simulation enabled in the config.
   */
  public boolean isSimulated() {
    return cameraSim != null;
  }

  /**
   * The camera's PhotonVision simulation, for settings YALL doesn't cover.
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
   * PhotonVision's pose estimator for this camera's simulated results, for settings YALL doesn't
   * cover.
   *
   * @return The pose estimator, or empty when not simulating.
   */
  public Optional<PhotonPoseEstimator> getSimulatedPoseEstimator() {
    return Optional.ofNullable(simPoseEstimator);
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
   * What the pose filter has accepted and rejected since the stats were last reset, and how spread
   * out the accepted poses are.
   *
   * @return The stats.
   */
  public PoseFilterStats getFilterStats() {
    return filterStats;
  }

  /** Starts the pose filter stats counting again. */
  public void resetFilterStats() {
    filterStats.reset();
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

  /** Removes the camera from simulation and closes its telemetry. */
  @Override
  public void close() {
    if (cameraSim != null) {
      visionSim.removeCamera(cameraSim);
      var photonCamera = cameraSim.getCamera();
      cameraSim.close();
      // Close the private camera made for a non-PhotonVision camera, not a team's own.
      if (photonCamera.getName().equals(VisionSim.privateCameraName(name))) {
        photonCamera.close();
      }
    }
    if (telemetry != null) {
      telemetry.close();
    }
  }

  /**
   * The camera's NetworkTables publishers and DataLog entries, for the fields its telemetry config
   * turns on.
   */
  private static final class Telemetry implements AutoCloseable {
    /** Writes a boolean to NetworkTables, the DataLog or both. */
    private interface BooleanOut {
      void set(boolean value);
    }

    /** Writes an integer to NetworkTables, the DataLog or both. */
    private interface IntegerOut {
      void set(long value);
    }

    /** Writes a number to NetworkTables, the DataLog or both. */
    private interface DoubleOut {
      void set(double value);
    }

    /** Writes a value of another type to NetworkTables, the DataLog or both. */
    private interface Out<T> {
      void set(T value);
    }

    private final NetworkTable table;
    private final DataLog log;
    private final String logPath;
    private final List<AutoCloseable> closeables = new ArrayList<>();
    private BooleanOut connected;
    private BooleanOut hasTargets;
    private IntegerOut targetCount;
    private Out<Pose3d[]> acceptedPoses;
    private Out<Pose3d[]> rejectedPoses;
    private Out<String> rejectionReason;
    private DoubleOut latency;
    private boolean stats;
    private IntegerOut statsAccepted;
    private IntegerOut statsRejected;
    private DoubleOut statsAcceptanceRate;
    private IntegerOut statsSamples;
    private Out<Pose2d> statsMeanPose;
    private Out<double[]> statsStdDevs;
    private Out<double[]> statsSuggestedBase;
    private DoubleOut statsDistance;
    private BooleanEntry statsReset;
    private final Map<String, IntegerOut> statsByReason = new HashMap<>();
    private Out<long[]> targetIds;
    private Out<double[]> targetYaws;
    private Out<double[]> targetPitches;
    private Out<double[]> targetAreas;

    Telemetry(String cameraName, VisionTelemetryConfig config) {
      table =
          config.isNetworkTablesEnabled()
              ? NetworkTableInstance.getDefault().getTable(config.getTablePath(cameraName))
              : null;
      var path = config.getDataLogPath(cameraName);
      log = path.isPresent() ? DataLogManager.getLog() : null;
      logPath = path.map(p -> p.endsWith("/") ? p : p + "/").orElse(null);
      if (config.isEnabled(VisionTelemetryField.CONNECTED)) {
        connected = booleanOut("connected");
      }
      if (config.isEnabled(VisionTelemetryField.HAS_TARGETS)) {
        hasTargets = booleanOut("hasTargets");
      }
      if (config.isEnabled(VisionTelemetryField.TARGET_COUNT)) {
        targetCount = integerOut("targetCount");
      }
      if (config.isEnabled(VisionTelemetryField.ACCEPTED_POSES)) {
        acceptedPoses = poseArrayOut("acceptedPoses");
      }
      if (config.isEnabled(VisionTelemetryField.REJECTED_POSES)) {
        rejectedPoses = poseArrayOut("rejectedPoses");
      }
      if (config.isEnabled(VisionTelemetryField.REJECTION_REASON)) {
        rejectionReason = stringOut("rejectionReason");
      }
      if (config.isEnabled(VisionTelemetryField.LATENCY)) {
        latency = doubleOut("latencyMillis");
      }
      if (config.isEnabled(VisionTelemetryField.FILTER_STATS)) {
        stats = true;
        statsAccepted = integerOut("stats/accepted");
        statsRejected = integerOut("stats/rejected");
        statsAcceptanceRate = doubleOut("stats/acceptanceRate");
        statsSamples = integerOut("stats/sampleCount");
        statsMeanPose = poseOut("stats/meanPose");
        statsStdDevs = doubleArrayOut("stats/stdDevs");
        statsSuggestedBase = doubleArrayOut("stats/suggestedBaseStdDevs");
        statsDistance = doubleOut("stats/averageTagDistanceMeters");
        if (table != null) {
          statsReset = keep(table.getBooleanTopic("stats/reset").getEntry(false));
          statsReset.set(false);
        }
      }
      if (config.isEnabled(VisionTelemetryField.TARGETS)) {
        targetIds = integerArrayOut("targets/fiducialIds");
        targetYaws = doubleArrayOut("targets/yawDegrees");
        targetPitches = doubleArrayOut("targets/pitchDegrees");
        targetAreas = doubleArrayOut("targets/areaPercent");
      }
    }

    private <T extends AutoCloseable> T keep(T closeable) {
      closeables.add(closeable);
      return closeable;
    }

    private <T extends DataLogEntry> T keepEntry(T entry) {
      closeables.add(entry::finish);
      return entry;
    }

    private BooleanOut booleanOut(String key) {
      var nt = table != null ? keep(table.getBooleanTopic(key).publish()) : null;
      var entry = log != null ? keepEntry(new BooleanLogEntry(log, logPath + key)) : null;
      return value -> {
        if (nt != null) {
          nt.set(value);
        }
        if (entry != null) {
          entry.update(value);
        }
      };
    }

    private IntegerOut integerOut(String key) {
      var nt = table != null ? keep(table.getIntegerTopic(key).publish()) : null;
      var entry = log != null ? keepEntry(new IntegerLogEntry(log, logPath + key)) : null;
      return value -> {
        if (nt != null) {
          nt.set(value);
        }
        if (entry != null) {
          entry.update(value);
        }
      };
    }

    private DoubleOut doubleOut(String key) {
      var nt = table != null ? keep(table.getDoubleTopic(key).publish()) : null;
      var entry = log != null ? keepEntry(new DoubleLogEntry(log, logPath + key)) : null;
      return value -> {
        if (nt != null) {
          nt.set(value);
        }
        if (entry != null) {
          entry.update(value);
        }
      };
    }

    private Out<String> stringOut(String key) {
      var nt = table != null ? keep(table.getStringTopic(key).publish()) : null;
      var entry = log != null ? keepEntry(new StringLogEntry(log, logPath + key)) : null;
      return value -> {
        if (nt != null) {
          nt.set(value);
        }
        if (entry != null) {
          entry.update(value);
        }
      };
    }

    private Out<double[]> doubleArrayOut(String key) {
      var nt = table != null ? keep(table.getDoubleArrayTopic(key).publish()) : null;
      var entry = log != null ? keepEntry(new DoubleArrayLogEntry(log, logPath + key)) : null;
      return value -> {
        if (nt != null) {
          nt.set(value);
        }
        if (entry != null) {
          entry.update(value);
        }
      };
    }

    private Out<long[]> integerArrayOut(String key) {
      var nt = table != null ? keep(table.getIntegerArrayTopic(key).publish()) : null;
      var entry = log != null ? keepEntry(new IntegerArrayLogEntry(log, logPath + key)) : null;
      return value -> {
        if (nt != null) {
          nt.set(value);
        }
        if (entry != null) {
          entry.update(value);
        }
      };
    }

    private Out<Pose2d> poseOut(String key) {
      var nt = table != null ? keep(table.getStructTopic(key, Pose2d.struct).publish()) : null;
      var entry =
          log != null ? keepEntry(StructLogEntry.create(log, logPath + key, Pose2d.struct)) : null;
      return value -> {
        if (nt != null) {
          nt.set(value);
        }
        if (entry != null) {
          entry.update(value);
        }
      };
    }

    private Out<Pose3d[]> poseArrayOut(String key) {
      var nt = table != null ? keep(table.getStructArrayTopic(key, Pose3d.struct).publish()) : null;
      var entry =
          log != null
              ? keepEntry(StructArrayLogEntry.create(log, logPath + key, Pose3d.struct))
              : null;
      return value -> {
        if (nt != null) {
          nt.set(value);
        }
        if (entry != null) {
          entry.update(value);
        }
      };
    }

    void publish(VisionCamera camera) {
      if (connected != null) {
        connected.set(camera.isConnected());
      }
      if (hasTargets != null) {
        hasTargets.set(camera.hasTargets());
      }
      if (targetCount != null) {
        targetCount.set(camera.getTargets().size());
      }
      if (acceptedPoses != null) {
        acceptedPoses.set(
            camera.acceptedPoses.stream().map(VisionPoseEstimate::pose).toArray(Pose3d[]::new));
      }
      if (rejectedPoses != null) {
        rejectedPoses.set(
            camera.rejectedPoses.stream().map(VisionPoseEstimate::pose).toArray(Pose3d[]::new));
      }
      if (rejectionReason != null) {
        rejectionReason.set(camera.lastRejectionReason);
      }
      if (latency != null) {
        camera.latestResult.ifPresent(r -> latency.set(r.latencyMillis()));
      }
      if (stats) {
        publishStats(camera);
      }
      if (targetIds != null) {
        var targets = camera.getTargets();
        targetIds.set(targets.stream().mapToLong(VisionTarget::fiducialId).toArray());
        targetYaws.set(targets.stream().mapToDouble(VisionTarget::yawDegrees).toArray());
        targetPitches.set(targets.stream().mapToDouble(VisionTarget::pitchDegrees).toArray());
        targetAreas.set(targets.stream().mapToDouble(VisionTarget::areaPercent).toArray());
      }
    }

    private void publishStats(VisionCamera camera) {
      var stats = camera.filterStats;
      if (statsReset != null && statsReset.get()) {
        stats.reset();
        statsReset.set(false);
      }
      statsAccepted.set(stats.getAcceptedCount());
      statsRejected.set(stats.getRejectedCount());
      statsAcceptanceRate.set(stats.getAcceptanceRate());
      statsSamples.set(stats.getSampleCount());
      stats.getMeanPose().ifPresent(statsMeanPose::set);
      statsStdDevs.set(stats.getStdDevs().getData());
      statsSuggestedBase.set(
          stats
              .getSuggestedBaseStdDevs(camera.config.getPoseFilter().getDistanceDivisor())
              .getData());
      statsDistance.set(stats.getAverageTagDistanceMeters());
      for (var reason : stats.getRejectionCounts().entrySet()) {
        statsByReason
            .computeIfAbsent(reason.getKey(), key -> integerOut("stats/rejectedBy/" + key))
            .set(reason.getValue());
      }
      // A reset clears the counts, so zero the reasons that are no longer counted.
      for (var reason : statsByReason.entrySet()) {
        if (!stats.getRejectionCounts().containsKey(reason.getKey())) {
          reason.getValue().set(0);
        }
      }
    }

    @Override
    public void close() {
      for (AutoCloseable closeable : closeables) {
        try {
          closeable.close();
        } catch (Exception e) {
          // Closing a NetworkTables handle or finishing a log entry doesn't throw.
        }
      }
    }
  }

  private static double averageAprilTagAreaPercent(VisionResult result) {
    var targets = result.targets();
    double sum = 0.0;
    int count = 0;
    for (int i = 0; i < targets.size(); i++) {
      var t = targets.get(i);
      if (t.isAprilTag() && Double.isFinite(t.areaPercent())) {
        sum += t.areaPercent();
        count++;
      }
    }
    return count > 0 ? sum / count : Double.NaN;
  }
}
