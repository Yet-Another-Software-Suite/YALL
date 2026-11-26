package yall.cameras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Meters;
import static org.wpilib.units.Units.Seconds;

import gg.questnav.questnav.PoseFrame;
import gg.questnav.questnav.QuestNav;
import gg.questnav.questnav.protos.generated.Commands;
import gg.questnav.questnav.protos.wpilib.CommandProto;
import gg.questnav.questnav.protos.wpilib.CommandResponseProto;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wpilib.fields.Field;
import org.wpilib.fields.Fields;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.math.estimator.DifferentialDrivePoseEstimator;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform2d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.networktables.NetworkTable;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.ProtobufPublisher;
import org.wpilib.networktables.ProtobufSubscriber;
import org.wpilib.networktables.PubSubOption;
import org.wpilib.simulation.SimHooks;
import org.wpilib.system.Timer;
import yall.VisionCameraConfig;
import yall.VisionSystem;
import yall.filter.VisionPoseFilter;
import yall.results.VisionPoseEstimate;
import yall.sim.VisionSim;

/** QuestNav headsets, simulated and through QuestNavLib with a fake headset on NetworkTables. */
class QuestNavWrapperTest {

  private static final Field FIELD = Field.loadField(Fields.DEFAULT_FIELD);

  /** A headset 0.25 m behind the robot center and 0.4 m up, facing backward. */
  private static final Transform3d ROBOT_TO_QUEST =
      new Transform3d(-0.25, 0.1, 0.4, new Rotation3d(0, 0, Math.PI));

  private static final Pose2d START = new Pose2d(3, 2, Rotation2d.fromDegrees(30));

  // QuestNav doesn't send a request equal to the topic's current value, and every new QuestNav's
  // first reset has command ID 1, so each fake headset test resets to a pose of its own.
  private static final Pose2d REAL_START = new Pose2d(3.5, 2.5, Rotation2d.fromDegrees(40));
  private static final Pose2d FAILED_RESET_START = new Pose2d(4, 3, Rotation2d.fromDegrees(50));
  private static final Pose2d OLD_RESPONSE_START = new Pose2d(4.5, 3.5, Rotation2d.fromDegrees(60));

  private static int simCount;

  private QuestNavWrapper quest;

  @BeforeAll
  static void setUp() {
    HAL.initialize();
    SimHooks.pauseTiming();
  }

  @AfterAll
  static void tearDown() {
    SimHooks.resumeTiming();
  }

  @AfterEach
  void closeQuest() {
    if (quest != null) {
      quest.close();
      quest = null;
    }
  }

  private static VisionCameraConfig config() {
    return new VisionCameraConfig()
        .withField(FIELD)
        .withRobotToCamera(ROBOT_TO_QUEST)
        .withTelemetry(false);
  }

  private static VisionSim newSim() {
    return new VisionSim("QuestNavWrapperTest" + simCount++, FIELD);
  }

  private static void assertPoseNear(Pose2d expected, Pose2d actual) {
    assertEquals(expected.getX(), actual.getX(), 1e-6, "x");
    assertEquals(expected.getY(), actual.getY(), 1e-6, "y");
    assertEquals(
        0, expected.getRotation().minus(actual.getRotation()).getRadians(), 1e-6, "heading");
  }

  private void step() {
    SimHooks.stepTiming(0.02);
    quest.update();
  }

  // Simulation

  @Test
  void simulatedHeadsetIsRejectedUntilItsPoseIsSet() {
    var sim = newSim();
    quest = new QuestNavWrapper(new QuestNav(), config().withSimulation(sim));
    assertTrue(quest.isSimulated());
    assertTrue(quest.isConnected());
    assertTrue(quest.getVisionSim().isPresent());

    sim.update(START);
    step();
    assertTrue(quest.getEstimatedPose().isEmpty());
    assertEquals(1, quest.getRejectedPoses().size());
    assertEquals("pose not set", quest.getLastRejectionReason());
    assertFalse(quest.isPoseSet());
    // Its tracking started where the robot was, so it reports the robot near its own origin.
    var unsetPose = quest.getRejectedPoses().get(0).pose2d();
    assertPoseNear(
        new Pose3d().transformBy(ROBOT_TO_QUEST.inverse()).toPose2d(), unsetPose);

    quest.setRobotPose(START);
    assertTrue(quest.isPoseSet());
    step();
    var estimate = quest.getEstimatedPose().orElseThrow();
    assertPoseNear(START, estimate.pose2d());
    assertEquals(0, estimate.tagCount());
    assertEquals(QuestNavWrapper.NAME, estimate.cameraName());
  }

  @Test
  void simulatedHeadsetFollowsTheRobotFromWhereItWasTold() {
    var sim = newSim();
    quest = new QuestNavWrapper(new QuestNav(), config().withSimulation(sim));
    sim.update(START);
    step();

    // Told the wrong pose, it reports the robot's movement from that pose.
    var told = new Pose2d(10, 5, Rotation2d.fromDegrees(-90));
    quest.setRobotPose(told);
    var move = new Transform2d(1.5, -0.5, Rotation2d.fromDegrees(45));
    sim.update(START.transformBy(move));
    step();
    assertPoseNear(told.transformBy(move), quest.getEstimatedPose().orElseThrow().pose2d());

    // Told the true pose, it reports the true pose.
    quest.setRobotPose(START.transformBy(move));
    var secondMove = new Transform2d(-0.75, 1, Rotation2d.fromDegrees(-120));
    sim.update(START.transformBy(move).transformBy(secondMove));
    step();
    assertPoseNear(
        START.transformBy(move).transformBy(secondMove),
        quest.getEstimatedPose().orElseThrow().pose2d());
  }

  @Test
  void acceptedEstimatesGetTheHeadsetsStandardDeviations() {
    var sim = newSim();
    quest =
        new QuestNavWrapper(new QuestNav(), config().withSimulation(sim))
            .withStdDevs(Meters.of(0.05), Degrees.of(3));
    sim.update(START);
    quest.setRobotPose(START);
    step();
    var stdDevs = quest.getEstimatedPose().orElseThrow().stdDevs();
    assertEquals(0.05, stdDevs.get(0, 0), 1e-9);
    assertEquals(0.05, stdDevs.get(1, 0), 1e-9);
    assertEquals(Math.toRadians(3), stdDevs.get(2, 0), 1e-9);

    assertThrows(IllegalArgumentException.class, () -> quest.withStdDevs(Meters.of(0), Degrees.of(1)));
    assertThrows(
        IllegalArgumentException.class,
        () -> quest.withStdDevs(Meters.of(0.1), Degrees.of(Double.NaN)));
  }

  @Test
  void simulatedHeadsetLosesTrackingDuringABlackout() {
    var sim = newSim();
    quest = new QuestNavWrapper(new QuestNav(), config().withSimulation(sim));
    sim.update(START);
    quest.setRobotPose(START);
    step();
    assertTrue(quest.isTracking());

    sim.injectBlackout(Timer.getMonotonicTimestamp(), Seconds.of(1));
    step();
    assertFalse(quest.isTracking());
    assertTrue(quest.getEstimatedPoses().isEmpty());
    sim.clearFaults();
    step();
    assertTrue(quest.getEstimatedPose().isPresent());
  }

  @Test
  void tagChecksDontApplyButThePoseChecksDo() {
    var sim = newSim();
    var current = new Pose2d[] {START};
    var filter =
        new VisionPoseFilter().withMaxDistanceFromPose(() -> current[0], Meters.of(1));
    quest =
        new QuestNavWrapper(new QuestNav(), config().withSimulation(sim).withPoseFilter(filter));
    sim.update(START);
    quest.setRobotPose(START);
    step();
    // No tags, yet accepted: the default filter's tag count and tag distance checks don't apply.
    assertTrue(quest.getEstimatedPose().isPresent());

    current[0] = START.transformBy(new Transform2d(3, 0, Rotation2d.ZERO));
    step();
    assertTrue(quest.getEstimatedPose().isEmpty());
    assertEquals("too far from the current pose", quest.getLastRejectionReason());

    quest.setRobotPose(new Pose2d(-5, -5, Rotation2d.ZERO));
    current[0] = new Pose2d(-5, -5, Rotation2d.ZERO);
    step();
    assertEquals("off the field", quest.getLastRejectionReason());
  }

  @Test
  void checkWithoutTagsSkipsOnlyTheTagChecks() {
    var filter = new VisionPoseFilter().withMaxTilt(Degrees.of(10));
    double now = Timer.getMonotonicTimestamp();
    var onField =
        VisionPoseEstimate.unfiltered(
            "q", new Pose3d(START), now, List.of(), Double.NaN, Double.NaN, false);
    assertEquals("no tags", filter.check(onField, FIELD, now, Double.NaN).orElseThrow());
    assertTrue(filter.checkWithoutTags(onField, FIELD, now).isEmpty());

    var old =
        VisionPoseEstimate.unfiltered(
            "q", new Pose3d(START), now - 1, List.of(), Double.NaN, Double.NaN, false);
    assertEquals("too old", filter.checkWithoutTags(old, FIELD, now).orElseThrow());

    var tilted =
        VisionPoseEstimate.unfiltered(
            "q",
            new Pose3d(3, 2, 0, new Rotation3d(Math.toRadians(20), 0, 0)),
            now,
            List.of(),
            Double.NaN,
            Double.NaN,
            false);
    assertEquals("tilted", filter.checkWithoutTags(tilted, FIELD, now).orElseThrow());

    filter.withCustomCheck("left half", e -> e.pose().getX() < 2);
    assertEquals("left half", filter.checkWithoutTags(onField, FIELD, now).orElseThrow());
  }

  @Test
  void visionSystemUpdatesTheSimulationAndFeedsThePoseEstimator() {
    var sim = newSim();
    quest = new QuestNavWrapper(new QuestNav(), config().withSimulation(sim));
    try (var vision = new VisionSystem(quest)) {
      var estimator =
          new DifferentialDrivePoseEstimator(
              Rotation2d.ZERO, 0, 0, Pose2d.ZERO);
      var truePose = new Pose2d(4, 3, Rotation2d.fromDegrees(120));
      vision.updateSimulation(truePose);
      quest.setRobotPose(truePose);
      for (int i = 0; i < 50; i++) {
        SimHooks.stepTiming(0.02);
        vision.updateSimulation(truePose);
        vision.update();
        estimator.updateWithTime(Timer.getTimestamp(), Rotation2d.ZERO, 0, 0);
        vision.addVisionMeasurements(estimator);
      }
      var estimate = estimator.getEstimatedPosition();
      assertEquals(truePose.getX(), estimate.getX(), 0.05);
      assertEquals(truePose.getY(), estimate.getY(), 0.05);
      assertEquals(
          0, truePose.getRotation().minus(estimate.getRotation()).getDegrees(), 2);
      quest = null; // Closed by the vision system.
    }
  }

  @Test
  void vendorConfigsAreRefused() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new QuestNavWrapper(new QuestNav(), config().withVendorConfig("nope")));
  }

  // A real headset, faked on QuestNav's NetworkTables topics

  /** Publishes what a Quest headset would, and reads the commands sent to it. */
  private static final class FakeHeadset implements AutoCloseable {
    private final ProtobufPublisher<PoseFrame> frames;
    private final ProtobufPublisher<Commands.ProtobufQuestNavCommandResponse> responses;
    private final ProtobufSubscriber<Commands.ProtobufQuestNavCommand> requests;
    private int frameCount;

    FakeHeadset() {
      NetworkTable table = NetworkTableInstance.getDefault().getTable("QuestNav");
      frames =
          table
              .getProtobufTopic("frameData", PoseFrame.proto)
              .publish(PubSubOption.SEND_ALL, PubSubOption.KEEP_DUPLICATES);
      responses =
          table
              .getProtobufTopic("response", new CommandResponseProto())
              .publish(PubSubOption.SEND_ALL, PubSubOption.KEEP_DUPLICATES);
      requests =
          table
              .getProtobufTopic("request", new CommandProto())
              .subscribe(
                  Commands.ProtobufQuestNavCommand.newInstance(),
                  PubSubOption.KEEP_DUPLICATES,
                  PubSubOption.pollStorage(20));
    }

    void sendFrame(Pose3d questPose, boolean tracking) {
      frames.set(
          new PoseFrame(questPose, Timer.getTimestamp(), 0, ++frameCount, tracking));
    }

    void restartApp() {
      frameCount = 0;
    }

    /** The command ID of the newest pose reset sent to the headset. */
    int lastResetId() {
      var queue = requests.readQueueValues();
      assertTrue(queue.length > 0, "no pose reset was sent");
      var request = queue[queue.length - 1];
      assertEquals(Commands.QuestNavCommandType.POSE_RESET, request.getType());
      return request.getCommandId();
    }

    void respond(int commandId, boolean success) {
      responses.set(
          Commands.ProtobufQuestNavCommandResponse.newInstance()
              .setCommandId(commandId)
              .setSuccess(success)
              .setErrorMessage(success ? "" : "test failure"));
    }

    @Override
    public void close() {
      frames.close();
      responses.close();
      requests.close();
    }
  }

  @Test
  void realHeadsetIsAcceptedOnlyAfterItConfirmsTheReset() {
    try (var headset = new FakeHeadset()) {
      quest = new QuestNavWrapper(new QuestNav(), config().withSimulation(false));
      assertFalse(quest.isSimulated());

      // Before any reset: its own tracking frame.
      headset.sendFrame(new Pose3d(1, 1, 0.4, Rotation3d.ZERO), true);
      step();
      assertEquals("pose not set", quest.getLastRejectionReason());
      assertTrue(quest.isConnected());

      // Reset sent, but not confirmed: frames are still in the old frame.
      quest.setRobotPose(REAL_START);
      int id = headset.lastResetId();
      headset.sendFrame(new Pose3d(1, 1, 0.4, Rotation3d.ZERO), true);
      step();
      assertTrue(quest.getEstimatedPose().isEmpty());
      assertEquals("pose reset pending", quest.getLastRejectionReason());
      assertFalse(quest.isPoseSet());

      // Confirmed: frames from after the response are accepted, moved to the robot origin.
      headset.respond(id, true);
      SimHooks.stepTiming(0.02);
      headset.sendFrame(new Pose3d(REAL_START).transformBy(ROBOT_TO_QUEST), true);
      step();
      assertTrue(quest.isPoseSet());
      assertPoseNear(REAL_START, quest.getEstimatedPose().orElseThrow().pose2d());
      assertEquals(0.02, quest.getEstimatedPose().get().stdDevs().get(0, 0), 1e-9);

      // Lost tracking.
      headset.sendFrame(new Pose3d(REAL_START).transformBy(ROBOT_TO_QUEST), false);
      step();
      assertEquals("not tracking", quest.getLastRejectionReason());

      // The QuestNav app restarted: its tracking started over.
      headset.restartApp();
      headset.sendFrame(new Pose3d(REAL_START).transformBy(ROBOT_TO_QUEST), true);
      step();
      assertEquals("pose not set", quest.getLastRejectionReason());
      assertFalse(quest.isPoseSet());

      // After a robot program restart, the pose the headset still has can be trusted.
      quest.trustCurrentPose();
      headset.sendFrame(new Pose3d(REAL_START).transformBy(ROBOT_TO_QUEST), true);
      step();
      assertTrue(quest.getEstimatedPose().isPresent());
    }
  }

  @Test
  void aFailedResetKeepsThePoseTheHeadsetHad() {
    try (var headset = new FakeHeadset()) {
      quest = new QuestNavWrapper(new QuestNav(), config().withSimulation(false));
      quest.setRobotPose(FAILED_RESET_START);
      headset.respond(headset.lastResetId(), true);
      SimHooks.stepTiming(0.02);
      headset.sendFrame(new Pose3d(FAILED_RESET_START).transformBy(ROBOT_TO_QUEST), true);
      step();
      assertTrue(quest.getEstimatedPose().isPresent());

      quest.setRobotPose(new Pose2d(8, 4, Rotation2d.ZERO));
      headset.respond(headset.lastResetId(), false);
      SimHooks.stepTiming(0.02);
      headset.sendFrame(new Pose3d(FAILED_RESET_START).transformBy(ROBOT_TO_QUEST), true);
      step();
      assertTrue(quest.isPoseSet());
      assertPoseNear(FAILED_RESET_START, quest.getEstimatedPose().orElseThrow().pose2d());
    }
  }

  @Test
  void anOldResponseWithTheSameIdDoesntConfirmANewReset() {
    try (var headset = new FakeHeadset()) {
      // The headset's last response from an earlier robot program, before this reset was sent.
      headset.respond(1, true);
      SimHooks.stepTiming(0.02);
      quest = new QuestNavWrapper(new QuestNav(), config().withSimulation(false));
      quest.setRobotPose(OLD_RESPONSE_START);
      assertEquals(1, headset.lastResetId());
      headset.sendFrame(new Pose3d(OLD_RESPONSE_START).transformBy(ROBOT_TO_QUEST), true);
      step();
      assertEquals("pose reset pending", quest.getLastRejectionReason());
    }
  }
}
