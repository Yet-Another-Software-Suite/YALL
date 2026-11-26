package yall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wpilib.units.Units.DegreesPerSecond;

import com.limelightvision.Limelight;
import java.util.ArrayList;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.photonvision.PhotonCamera;
import org.photonvision.estimation.TargetModel;
import org.photonvision.simulation.VisionTargetSim;
import org.wpilib.datalog.DataLogReader;
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
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.simulation.SimHooks;
import org.wpilib.system.DataLogManager;
import synapse.SynapseCamera;
import yall.cameras.LimelightWrapper;
import yall.cameras.PhotonVisionWrapper;
import yall.cameras.SynapseWrapper;
import yall.results.VisionPoseEstimate;
import yall.results.VisionTarget;
import yall.sim.VisionSim;

/** Every vendor's camera gives simulated results from the same PhotonVision world. */
class SimulatedCamerasTest {

  private static final Field FIELD = Field.loadField(Fields.DEFAULT_FIELD);

  @BeforeAll
  static void setUp() {
    HAL.initialize();
    SimHooks.pauseTiming();
  }

  @AfterAll
  static void tearDown() {
    SimHooks.resumeTiming();
  }

  /** A robot 2 m in front of the first tag on the field, facing it. */
  private static Pose2d facingFirstTag() {
    var tag = FIELD.getTags().get(0).getPose().toPose2d();
    return tag.transformBy(new Transform2d(2, 0, Rotation2d.PI));
  }

  private static VisionCameraConfig config(VisionSim sim) {
    return config(sim, PoseEstimationMode.TAGS_ONLY);
  }

  private static VisionCameraConfig config(VisionSim sim, PoseEstimationMode mode) {
    return new VisionCameraConfig()
        .withPoseEstimationMode(mode)
        .withField(FIELD)
        .withSimulation(sim)
        .withRobotToCamera(new Transform3d(0, 0, 0.5, Rotation3d.ZERO));
  }

  @ParameterizedTest
  @EnumSource(PoseEstimationMode.class)
  void everyVendorSeesTheTagAndEstimatesThePose(PoseEstimationMode mode) {
    var sim = new VisionSim("test-" + mode, FIELD);
    var robot = facingFirstTag();
    try (var vision =
        new VisionSystem(
            new PhotonVisionWrapper(new PhotonCamera("photon-" + mode), config(sim, mode)),
            new LimelightWrapper(new Limelight("limelight-" + mode), config(sim, mode)),
            new SynapseWrapper(new SynapseCamera("synapse-" + mode), config(sim, mode)))) {
      var estimates = new ArrayList<VisionPoseEstimate>();
      for (int i = 0; i < 50; i++) {
        SimHooks.stepTiming(0.02);
        vision.updateSimulation(robot);
        vision.setRobotOrientation(robot.getRotation());
        vision.update();
        estimates.addAll(vision.getEstimatedPoses());
      }
      for (VisionCamera camera : vision.getCameras()) {
        assertTrue(camera.isSimulated(), camera.getName());
        assertTrue(camera.hasTargets(), camera.getName() + " sees no targets");
        int tagId = FIELD.getTags().get(0).getID();
        assertTrue(
            camera.getTargets().stream().anyMatch(t -> t.fiducialId() == tagId),
            camera.getName() + " doesn't see tag " + tagId);
        List<VisionPoseEstimate> mine =
            estimates.stream().filter(e -> e.cameraName().equals(camera.getName())).toList();
        assertFalse(
            mine.isEmpty(),
            camera.getName()
                + " has no accepted estimates; last rejection: "
                + camera.getLastRejectionReason());
        for (VisionPoseEstimate e : mine) {
          double error = e.pose2d().getTranslation().getDistance(robot.getTranslation());
          assertTrue(error < 0.25, camera.getName() + " is " + error + " m off");
          // In simulation every camera estimates through PhotonVision, which has a heading-assisted
          // solve, so the mode decides the method.
          assertEquals(mode == PoseEstimationMode.HEADING_ASSISTED, e.headingAssisted());
        }
      }
    }
  }

  @Test
  void photonVisionRejectsVendorConfigs() {
    var sim = new VisionSim("vendor-config", FIELD);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PhotonVisionWrapper(
                new PhotonCamera("photon-vendor"), config(sim).withVendorConfig("not a config")));
  }

  @Test
  void simulatedGamePiecesComeBackAsDetectedObjects() {
    var sim = new VisionSim("objects", FIELD);
    var robot = new Pose2d(4, 4, Rotation2d.ZERO);
    // A 0.3 m ball 2 m ahead of the robot, on the floor, as class 3 with 0.9 confidence.
    sim.addVisionTargets(
        "balls",
        new VisionTargetSim(
            new Pose3d(6, 4, 0.15, Rotation3d.ZERO), new TargetModel(0.3), 3, 0.9f));
    try (var camera =
        new PhotonVisionWrapper(
            new PhotonCamera("objects"),
            new VisionCameraConfig()
                .withField(FIELD)
                .withSimulation(sim)
                .withRobotToCamera(
                    new Transform3d(0, 0, 0.5, new Rotation3d(0, Math.toRadians(15), 0))))) {
      boolean seen = false;
      for (int i = 0; i < 50 && !seen; i++) {
        SimHooks.stepTiming(0.02);
        sim.update(robot);
        camera.update();
        seen = camera.getTargets().stream().anyMatch(VisionTarget::isDetectedObject);
      }
      assertTrue(seen, "the camera never saw the ball");
      var ball =
          camera.getTargets().stream().filter(VisionTarget::isDetectedObject).findFirst().get();
      assertEquals(3, ball.classId());
      assertEquals(0.9, ball.confidence(), 1e-6);
      assertEquals(-1, ball.fiducialId());
      // Straight ahead. The camera is tilted 15° down and the ball is atan(0.35 / 2) = 9.9° below
      // it, so the ball is about 5.1° above the crosshair: pitch is positive up.
      assertEquals(0, ball.yawDegrees(), 1);
      assertEquals(15 - Math.toDegrees(Math.atan2(0.35, 2)), ball.pitchDegrees(), 1);
      for (VisionTarget tag :
          camera.getTargets().stream().filter(VisionTarget::isAprilTag).toList()) {
        assertEquals(-1, tag.classId());
        assertEquals(0, tag.confidence());
      }
    }
  }

  @Test
  void aTurretCameraMeasuresItsAngle() {
    var sim = new VisionSim("turret", FIELD);
    var robot = facingFirstTag();
    var turretAngle = Rotation2d.fromDegrees(20);
    try (var camera =
        new PhotonVisionWrapper(
            new PhotonCamera("turret"),
            config(sim)
                .withRobotToCamera(
                    new Transform3d(
                        0.1, 0, 0.5, new Rotation3d(0, 0, turretAngle.getRadians()))))) {
      for (int i = 0; i < 25; i++) {
        SimHooks.stepTiming(0.02);
        sim.update(robot);
        camera.update();
      }
      var measured = camera.getCameraYawOnRobot(robot.getRotation());
      assertTrue(measured.isPresent(), "no tag solved");
      assertEquals(20, measured.get().getDegrees(), 2);

      // Turn the turret: the measurement follows.
      camera.setRobotToCamera(
          new Transform3d(0.1, 0, 0.5, new Rotation3d(0, 0, Math.toRadians(-15))));
      for (int i = 0; i < 25; i++) {
        SimHooks.stepTiming(0.02);
        sim.update(robot);
        camera.update();
      }
      assertEquals(-15, camera.getCameraYawOnRobot(robot.getRotation()).get().getDegrees(), 2);
    }
  }

  @Test
  void filterStatsAreCountedAndPublished() {
    var sim = new VisionSim("stats", FIELD);
    var robot = facingFirstTag();
    try (var camera =
        new PhotonVisionWrapper(
            new PhotonCamera("stats"),
            config(sim)
                .withTelemetry(
                    new yall.telemetry.VisionTelemetryConfig()
                        .withVerbosity(yall.telemetry.TelemetryVerbosity.HIGH)))) {
      for (int i = 0; i < 50; i++) {
        SimHooks.stepTiming(0.02);
        sim.update(robot);
        camera.update();
      }
      var stats = camera.getFilterStats();
      assertTrue(stats.getAcceptedCount() > 5, "accepted " + stats.getAcceptedCount());
      // A robot standing still: the spread is the camera's noise, a few centimeters at most.
      assertTrue(stats.getStdDevs().get(0, 0) < 0.1, "x std dev " + stats.getStdDevs().get(0, 0));
      var table = NetworkTableInstance.getDefault().getTable("YALL/stats/stats");
      assertEquals(stats.getAcceptedCount(), table.getEntry("accepted").getInteger(-1));
      // A dashboard can reset the stats.
      table.getEntry("reset").setBoolean(true);
      SimHooks.stepTiming(0.02);
      camera.update();
      assertTrue(camera.getFilterStats().getAcceptedCount() <= 2);
    }
  }

  @Test
  void telemetryCanGoToADataLogInsteadOfNetworkTables(@TempDir Path dir) throws Exception {
    var sim = new VisionSim("datalog", FIELD);
    var robot = facingFirstTag();
    // Start the log before logNetworkTables, which starts one with the defaults.
    DataLogManager.start(dir.toString(), "yall.wpilog");
    DataLogManager.logNetworkTables(false);
    long accepted;
    try (var camera =
        new PhotonVisionWrapper(
            new PhotonCamera("datalog"),
            config(sim)
                .withTelemetry(
                    new yall.telemetry.VisionTelemetryConfig()
                        .withVerbosity(yall.telemetry.TelemetryVerbosity.HIGH)
                        .withNetworkTables(false)
                        .withDataLog()))) {
      for (int i = 0; i < 20; i++) {
        SimHooks.stepTiming(0.02);
        sim.update(robot);
        camera.update();
      }
      accepted = camera.getFilterStats().getAcceptedCount();
      assertTrue(accepted > 0);
    } finally {
      DataLogManager.stop();
    }
    assertFalse(
        NetworkTableInstance.getDefault().getTopic("/YALL/datalog/acceptedPoses").exists(),
        "published to NetworkTables");

    // stop() returns before the log's writer thread finishes the file, so read it until it's
    // complete: until it parses and has the last accepted count, or 5 seconds pass.
    var file = dir.resolve("yall.wpilog").toString();
    var names = new java.util.HashMap<Integer, String>();
    var lastInteger = new java.util.HashMap<String, Long>();
    long deadline = System.nanoTime() + 5_000_000_000L;
    while (true) {
      names.clear();
      lastInteger.clear();
      try {
        for (var record : new DataLogReader(file)) {
          if (record.isStart()) {
            names.put(record.getStartData().entry, record.getStartData().name);
          } else if (!record.isControl()) {
            var name = names.get(record.getEntry());
            if (name != null && name.equals("YALL/datalog/stats/accepted")) {
              lastInteger.put(name, record.getInteger());
            }
          }
        }
      } catch (java.io.IOException | RuntimeException e) {
        // Not written yet, or cut off partway through a record.
        if (System.nanoTime() > deadline) {
          throw e;
        }
      }
      Long last = lastInteger.get("YALL/datalog/stats/accepted");
      if ((last != null && last == accepted) || System.nanoTime() > deadline) {
        break;
      }
      Thread.sleep(50);
    }
    assertTrue(names.containsValue("YALL/datalog/connected"), names.values().toString());
    assertTrue(names.containsValue("YALL/datalog/acceptedPoses"), names.values().toString());
    assertTrue(names.containsValue("YALL/datalog/targets/fiducialIds"), names.values().toString());
    assertEquals(accepted, lastInteger.get("YALL/datalog/stats/accepted"));
  }

  @Test
  void aVisionSystemFeedsItsPoseEstimator() {
    var sim = new VisionSim("estimator", FIELD);
    var robot = facingFirstTag();
    // Odometry starts half a meter off.
    var wrong = robot.plus(new org.wpilib.math.geometry.Transform2d(0.5, 0.3, Rotation2d.ZERO));
    var estimator = new DifferentialDrivePoseEstimator(robot.getRotation(), 0, 0, wrong);
    try (var vision =
        new VisionSystem(new PhotonVisionWrapper(new PhotonCamera("estimator"), config(sim)))) {
      for (int i = 0; i < 100; i++) {
        SimHooks.stepTiming(0.02);
        estimator.update(robot.getRotation(), 0, 0);
        vision.updateSimulation(robot);
        vision.update();
        vision.addVisionMeasurements(estimator);
      }
      double error =
          estimator.getEstimatedPosition().getTranslation().getDistance(robot.getTranslation());
      // It starts 0.58 m off. One tag 2 m away is trusted little (standard deviation about 4.5 m),
      // so each measurement moves the estimate about 2% of the way: well over half in 2 seconds.
      assertTrue(error < 0.25, "pose estimator is still " + error + " m off");
      assertTrue(vision.getCameras().get(0).getCameraSim().isPresent());
    }
  }

  @Test
  void aHeadingWithAYawRateFeedsTheYawRateCheck() {
    var sim = new VisionSim("yaw-rate", FIELD);
    var robot = facingFirstTag();
    try (var camera =
        new PhotonVisionWrapper(
            new PhotonCamera("yaw-rate"), config(sim, PoseEstimationMode.HEADING_ASSISTED))) {
      // Spinning faster than the filter's 720 deg/s limit: every estimate is rejected.
      boolean anyAccepted = false;
      for (int i = 0; i < 25; i++) {
        SimHooks.stepTiming(0.02);
        sim.update(robot);
        camera.setRobotOrientation(robot.getRotation(), DegreesPerSecond.of(1000));
        camera.update();
        anyAccepted |= camera.getEstimatedPose().isPresent();
      }
      assertFalse(anyAccepted, "accepted an estimate while spinning at 1000 deg/s");
      // Standing still: heading-assisted estimates are accepted.
      var accepted = new ArrayList<VisionPoseEstimate>();
      for (int i = 0; i < 25; i++) {
        SimHooks.stepTiming(0.02);
        sim.update(robot);
        camera.setRobotOrientation(robot.getRotation(), DegreesPerSecond.of(0));
        camera.update();
        accepted.addAll(camera.getEstimatedPoses());
      }
      assertFalse(accepted.isEmpty(), "no estimate while standing still");
      assertTrue(accepted.stream().allMatch(VisionPoseEstimate::headingAssisted));
    }
  }
}
