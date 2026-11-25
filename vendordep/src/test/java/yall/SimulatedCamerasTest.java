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
import org.wpilib.fields.Field;
import org.wpilib.fields.Fields;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.math.geometry.Pose2d;
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
      var fused = vision.getFusedPose();
      assertTrue(fused.isPresent());
      assertTrue(fused.get().pose2d().getTranslation().getDistance(robot.getTranslation()) < 0.25);
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
}
