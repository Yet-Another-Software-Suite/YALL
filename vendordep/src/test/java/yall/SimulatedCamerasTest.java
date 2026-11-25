package yall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.limelightvision.Limelight;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.photonvision.PhotonCamera;
import org.photonvision.estimation.TargetModel;
import org.photonvision.simulation.VisionTargetSim;
import org.wpilib.fields.Field;
import org.wpilib.fields.Fields;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform2d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.simulation.SimHooks;
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
      // The fused pose only covers the last update, which may have no new frame, so keep every one.
      var fused = new ArrayList<VisionPoseEstimate>();
      for (int i = 0; i < 50; i++) {
        SimHooks.stepTiming(0.02);
        vision.updateSimulation(robot);
        vision.setRobotOrientation(robot.getRotation());
        vision.update();
        estimates.addAll(vision.getEstimatedPoses());
        vision.getFusedPose().ifPresent(fused::add);
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
      assertFalse(fused.isEmpty(), "no fused pose");
      for (VisionPoseEstimate e : fused) {
        double error = e.pose2d().getTranslation().getDistance(robot.getTranslation());
        assertTrue(error < 0.25, "fused pose is " + error + " m off");
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
}
