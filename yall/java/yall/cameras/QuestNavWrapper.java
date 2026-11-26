package yall.cameras;

import static org.wpilib.units.Units.Meters;
import static org.wpilib.units.Units.Radians;

import gg.questnav.questnav.PoseFrame;
import gg.questnav.questnav.QuestNav;
import gg.questnav.questnav.protos.generated.Commands;
import gg.questnav.questnav.protos.wpilib.CommandProto;
import gg.questnav.questnav.protos.wpilib.CommandResponseProto;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.wpilib.fields.Field;
import org.wpilib.framework.RobotBase;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.linalg.VecBuilder;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import org.wpilib.networktables.NetworkTable;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.NetworkTablesJNI;
import org.wpilib.networktables.ProtobufSubscriber;
import org.wpilib.networktables.PubSubOption;
import org.wpilib.networktables.TimestampedObject;
import org.wpilib.system.Timer;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.Distance;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.VisionFrame;
import yall.filter.VisionPoseFilter;
import yall.results.VisionPoseEstimate;
import yall.results.VisionResult;
import yall.sim.VisionSim;

/**
 * A QuestNav headset: a Meta Quest on the robot that tracks its own pose with its cameras and IMU,
 * through QuestNavLib. It isn't a camera and sees no targets, but it gives robot pose estimates like
 * one, so it works with {@link yall.VisionSystem}, the pose filter and telemetry like every other
 * {@link VisionCamera}.
 *
 * <pre>{@code
 * QuestNavWrapper quest = new QuestNavWrapper(new QuestNav(),
 *     new VisionCameraConfig()
 *         .withRobotToCamera(Inches.of(-10), Inches.of(0), Inches.of(14),   // where the headset is
 *                            Degrees.of(0), Degrees.of(0), Degrees.of(180)));
 *
 * // Once the robot's pose is known (at the start of autonomous, or from AprilTags while disabled):
 * quest.setRobotPose(startingPose);
 * }</pre>
 *
 * <p>The config's robot-to-camera transform is where the headset is on the robot. The Quest reports
 * its own pose, and the wrapper moves it back to the robot's origin with that transform, so measure
 * it carefully: an error in the headset's yaw turns every estimate.
 *
 * <p>The Quest tracks its pose relative to where it was when its tracking started, not on the field,
 * so its poses mean nothing until it is told where the robot is with {@link
 * #setRobotPose(Pose2d)}. Until then, and from each pose reset until the headset confirms it, its
 * estimates are rejected. A reset sent with {@link QuestNav#setPose} directly counts too, once the
 * wrapper exists. If the robot
 * program restarts while the headset keeps tracking (a brownout, for example), {@link
 * #trustCurrentPose()} accepts the pose the headset already has. If the QuestNav app on the headset
 * restarts, its tracking starts over, so its estimates are rejected again until the next reset.
 *
 * <p>Estimates are checked with {@link VisionPoseFilter#checkWithoutTags}: the age, field bounds,
 * height, tilt, pose jump and custom checks apply, the tag checks don't. Estimates taken while the
 * headset has lost tracking are rejected. Accepted estimates get the standard deviations from {@link
 * #withStdDevs(Distance, Angle)}, not the filter's.
 *
 * <p>{@link #update()} calls {@link QuestNav#commandPeriodic()}, so QuestNav's callbacks still fire,
 * and reads {@link QuestNav#getAllUnreadPoseFrames()}. Don't call either elsewhere: each frame is
 * handed out only once. The headset has no pipelines; pipeline settings are ignored.
 *
 * <p>In simulation, the headset reports the robot's true pose from its {@link VisionSim} world in
 * its own tracking frame, which starts where the robot was on the first update, as a real headset's
 * does. {@link #setRobotPose(Pose2d)} resets it at once. Use the wrapper's {@link
 * #setRobotPose(Pose2d)} in simulation, since the simulated headset doesn't read {@link
 * QuestNav#setPose}. The world's {@link VisionSim#injectBlackout blackouts} and {@link
 * VisionSim#withDropoutProbability dropouts} drop the headset's frames too.
 *
 * <p>No vendor configs are accepted; use {@link #getCamera()} for QuestNav settings.
 */
public class QuestNavWrapper extends VisionCamera {

  /** The camera name QuestNav estimates and telemetry are reported under. */
  public static final String NAME = "QuestNav";

  /** QuestNav's NetworkTables table. */
  private static final String TABLE = "QuestNav";

  private final QuestNav questNav;
  private final VisionSim sim;
  private final ProtobufSubscriber<Commands.ProtobufQuestNavCommand> requestSubscriber;
  private final ProtobufSubscriber<Commands.ProtobufQuestNavCommandResponse> responseSubscriber;

  // QuestNav's recommended standard deviations: 2 cm, 2 cm and 2 degrees.
  private Matrix<N3, N1> stdDevs = VecBuilder.fill(0.02, 0.02, 0.035);

  /** Whether the headset's poses are on the field. */
  private boolean poseSet;

  /** Frames captured before this time are from before the last confirmed pose reset. */
  private double poseSetSinceSeconds = Double.NEGATIVE_INFINITY;

  /** Command ID of the pose reset waiting for the headset's response, or -1 if none. */
  private long pendingResetId = -1;

  /**
   * When the pending pose reset was sent, in local NetworkTables time (nanoseconds). The robot is
   * the NetworkTables server, so this is the time base of QuestNav's frame timestamps.
   */
  private long pendingResetSentNanos;

  /** When this wrapper was made, in local NetworkTables time (nanoseconds). */
  private final long createdNanos = NetworkTablesJNI.now();

  /** Frame count of the last frame read, to notice the QuestNav app restarting. */
  private long lastFrameCount = Long.MIN_VALUE;

  /** The simulated headset's tracking origin on the field, or null before the first update. */
  private Pose3d simTrackingOrigin;

  /**
   * Wraps a QuestNav headset.
   *
   * @param questNav The QuestNav object.
   * @param config The headset's config. Its robot-to-camera transform is where the headset is on the
   *     robot.
   */
  public QuestNavWrapper(QuestNav questNav, VisionCameraConfig config) {
    super(NAME, config, null);
    this.questNav = questNav;
    config
        .getVendorConfig()
        .ifPresent(
            vendorConfig -> {
              throw new IllegalArgumentException(
                  "QuestNavWrapper takes no vendor config, got "
                      + vendorConfig.getClass().getName()
                      + ". Use getCamera() for QuestNav settings.");
            });
    sim =
        RobotBase.isSimulation() && config.isSimulationEnabled()
            ? config.getVisionSim().orElseGet(VisionSim::getDefault)
            : null;
    // Our own subscribers, so watching pose resets doesn't take anything from QuestNav's queues.
    NetworkTable table = NetworkTableInstance.getDefault().getTable(TABLE);
    requestSubscriber =
        table
            .getProtobufTopic("request", new CommandProto())
            .subscribe(
                Commands.ProtobufQuestNavCommand.newInstance(),
                PubSubOption.KEEP_DUPLICATES,
                PubSubOption.pollStorage(20));
    responseSubscriber =
        table
            .getProtobufTopic("response", new CommandResponseProto())
            .subscribe(
                Commands.ProtobufQuestNavCommandResponse.newInstance(),
                PubSubOption.SEND_ALL,
                PubSubOption.KEEP_DUPLICATES,
                PubSubOption.pollStorage(20));
  }

  /**
   * Sets the standard deviations given to accepted estimates. Defaults to QuestNav's recommended 2
   * cm for x and y and 2 degrees for the heading.
   *
   * @param xy Standard deviation of x and y.
   * @param heading Standard deviation of the heading.
   * @return This wrapper, for chaining.
   */
  public QuestNavWrapper withStdDevs(Distance xy, Angle heading) {
    double meters = xy.in(Meters);
    double radians = heading.in(Radians);
    if (!Double.isFinite(meters) || meters <= 0 || !Double.isFinite(radians) || radians <= 0) {
      throw new IllegalArgumentException("Standard deviations must be finite and positive");
    }
    stdDevs = VecBuilder.fill(meters, meters, radians);
    return this;
  }

  /**
   * The standard deviations given to accepted estimates.
   *
   * @return Standard deviations of x (meters), y (meters) and heading (radians).
   */
  public Matrix<N3, N1> getStdDevs() {
    return stdDevs;
  }

  /**
   * Tells the headset where the robot is, which puts its poses on the field. Call it when the robot's
   * pose is known: at the start of autonomous, or from AprilTag estimates while disabled. Estimates
   * are rejected until the headset confirms the reset.
   *
   * @param robotPose The robot's pose on the field.
   */
  public void setRobotPose(Pose3d robotPose) {
    var questPose = robotPose.transformBy(getRobotToCamera());
    if (sim != null) {
      // The tracking origin that makes the headset report questPose where it is now.
      var originToQuest = new Transform3d(questPose.getTranslation(), questPose.getRotation());
      simTrackingOrigin = trueQuestPose().transformBy(originToQuest.inverse());
      poseSet = true;
      poseSetSinceSeconds = Timer.getTimestamp();
      pendingResetId = -1;
    } else {
      questNav.setPose(questPose);
      readResets();
    }
  }

  /**
   * Tells the headset where the robot is, on the floor.
   *
   * @param robotPose The robot's pose on the field.
   * @see #setRobotPose(Pose3d)
   */
  public void setRobotPose(Pose2d robotPose) {
    setRobotPose(new Pose3d(robotPose));
  }

  /**
   * Accepts the pose the headset already has, without resetting it. Use it when the robot program
   * restarted while the headset kept tracking, such as after a brownout mid-match, so the headset's
   * poses are still on the field from an earlier reset.
   */
  public void trustCurrentPose() {
    poseSet = true;
    poseSetSinceSeconds = Double.NEGATIVE_INFINITY;
    pendingResetId = -1;
  }

  /**
   * Whether the headset's poses are on the field: it has confirmed a pose reset (or {@link
   * #trustCurrentPose()} was called), and no newer reset is waiting for its response.
   *
   * @return true if its estimates can be accepted.
   */
  public boolean isPoseSet() {
    return poseSet && pendingResetId < 0;
  }

  /**
   * Whether the headset is tracking its pose. Simulated headsets track unless their world is blacked
   * out.
   *
   * @return true if tracking.
   */
  public boolean isTracking() {
    if (sim != null) {
      return !sim.isBlackedOut(Timer.getMonotonicTimestamp());
    }
    return questNav.isTracking();
  }

  @Override
  protected List<VisionFrame> readFrames() {
    if (sim != null) {
      return readSimulatedFrames();
    }
    questNav.commandPeriodic();
    // Resets first, so frames from before a reset the headset hasn't confirmed are rejected.
    readResets();
    var frames = new ArrayList<VisionFrame>();
    for (PoseFrame frame : questNav.getAllUnreadPoseFrames()) {
      // The frame count starts again at 0 when the QuestNav app restarts, and so does its tracking,
      // so its poses are no longer on the field.
      if (frame.frameCount() < lastFrameCount) {
        poseSet = false;
      }
      lastFrameCount = frame.frameCount();
      frames.add(toFrame(frame.questPose3d(), frame.dataTimestamp(), frame.isTracking()));
    }
    return frames;
  }

  /** Follows pose resets sent to the headset and its responses to them. */
  private void readResets() {
    for (TimestampedObject<Commands.ProtobufQuestNavCommand> request :
        requestSubscriber.readQueue()) {
      // A new subscriber is handed the topic's last value, which may be from before this wrapper.
      if (request.value.getType() == Commands.QuestNavCommandType.POSE_RESET
          && request.timestamp >= createdNanos) {
        pendingResetId = request.value.getCommandId();
        pendingResetSentNanos = request.timestamp;
      }
    }
    for (TimestampedObject<Commands.ProtobufQuestNavCommandResponse> response :
        responseSubscriber.readQueue()) {
      // Command IDs start again at 1 in each robot program, and the headset sends its last
      // response again when it reconnects, so a response from before the request is an old one.
      if (pendingResetId < 0
          || response.value.getCommandId() != pendingResetId
          || response.timestamp < pendingResetSentNanos) {
        continue;
      }
      pendingResetId = -1;
      if (response.value.getSuccess()) {
        // Frames captured before the response arrived may be from before the reset.
        poseSet = true;
        poseSetSinceSeconds = response.timestamp * 1e-9;
      }
      // A failed reset leaves the headset's poses where they were, so poseSet doesn't change.
    }
  }

  private List<VisionFrame> readSimulatedFrames() {
    double now = Timer.getTimestamp();
    var questPose = trueQuestPose();
    if (simTrackingOrigin == null) {
      simTrackingOrigin = questPose;
    }
    if (sim.shouldDropFrame(Timer.getMonotonicTimestamp())) {
      return List.of();
    }
    return List.of(toFrame(questPose.relativeTo(simTrackingOrigin), now, true));
  }

  /** Where the simulated headset really is on the field. */
  private Pose3d trueQuestPose() {
    return sim.getVisionSystemSim().getRobotPose().transformBy(getRobotToCamera());
  }

  private VisionFrame toFrame(Pose3d questPose, double timestamp, boolean tracking) {
    double latencyMillis = Math.max(0.0, (Timer.getTimestamp() - timestamp) * 1000.0);
    var result = new VisionResult(name, timestamp, latencyMillis, List.of());
    var estimate =
        VisionPoseEstimate.unfiltered(
            name,
            questPose.transformBy(getRobotToCamera().inverse()),
            timestamp,
            List.of(),
            Double.NaN,
            Double.NaN,
            false);
    Optional<String> rejection;
    if (!tracking) {
      rejection = Optional.of("not tracking");
    } else if (pendingResetId >= 0) {
      rejection = Optional.of("pose reset pending");
    } else if (!poseSet) {
      rejection = Optional.of("pose not set");
    } else if (timestamp < poseSetSinceSeconds) {
      rejection = Optional.of("pose reset pending");
    } else {
      rejection = Optional.empty();
    }
    return new VisionFrame(result, Optional.of(estimate), rejection, Optional.of(stdDevs));
  }

  @Override
  protected Optional<String> checkEstimate(
      VisionPoseEstimate estimate,
      VisionPoseFilter filter,
      Field field,
      double nowSeconds,
      double averageTagAreaPercent) {
    return filter.checkWithoutTags(estimate, field, nowSeconds);
  }

  @Override
  protected void applyPipelineIndex(int index) {}

  @Override
  protected int readPipelineIndex() {
    return 0;
  }

  @Override
  protected boolean readConnected() {
    return sim != null || questNav.isConnected();
  }

  /**
   * Whether this headset is giving simulated poses.
   *
   * @return true in simulation with simulation enabled in the config.
   */
  @Override
  public boolean isSimulated() {
    return sim != null;
  }

  /**
   * The simulated world this headset is in, which {@link yall.VisionSystem#updateSimulation} updates.
   *
   * @return The world, or empty when not simulating.
   */
  @Override
  public Optional<VisionSim> getVisionSim() {
    return Optional.ofNullable(sim);
  }

  @Override
  public QuestNav getCamera() {
    return questNav;
  }

  /** Closes the reset subscribers, then the parts every camera shares. */
  @Override
  public void close() {
    requestSubscriber.close();
    responseSubscriber.close();
    super.close();
  }
}
