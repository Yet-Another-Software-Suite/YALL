package limelight.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.Optional;
import limelight.Limelight;
import limelight.networktables.LimelightPoseEstimator;
import limelight.networktables.LimelightPoseEstimator.EstimationMode;
import limelight.networktables.LimelightResults;
import limelight.networktables.PoseEstimate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End to end tests that drive {@link LimelightSim} through real NetworkTables and read the results back with the
 * same reader classes ({@link Limelight}, {@link LimelightPoseEstimator}) a robot program would use.
 */
class LimelightSimIntegrationTest
{

  private static AprilTagFieldLayout fieldLayout;

  @BeforeAll
  static void loadField()
  {
    fieldLayout = AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField);
  }

  @BeforeEach
  void initHal()
  {
    assertTrue(HAL.initialize(500, 0));
  }

  /**
   * Compute a robot pose sitting {@code distanceMeters} away from a tag, along the tag's outward facing normal, with
   * the robot yawed to look straight back at the tag (pitch/roll ignored, as most field tags are mounted close to
   * level with a modest height offset from the camera).
   */
  private static Pose3d poseFacingTag(AprilTag tag, double distanceMeters)
  {
    Translation3d normal = new Translation3d(1, 0, 0).rotateBy(tag.pose.getRotation());
    Translation3d cameraTranslation = tag.pose.getTranslation().plus(normal.times(distanceMeters));
    double yaw = Math.atan2(-normal.getY(), -normal.getX());
    return new Pose3d(cameraTranslation, new Rotation3d(0, 0, yaw));
  }

  @Test
  void noTagsVisibleWhenNoneAreInRange()
  {
    Limelight limelight = new Limelight("limelight-sim-it-notags");
    LimelightSim sim = new LimelightSim(limelight,
                                        LimelightSimSettings.perfect().withMaxDetectionRange(0.01));

    sim.update(new Pose3d());

    NetworkTable table = limelight.getNTTable();
    assertEquals(0, table.getEntry("tv").getDouble(-1));
    assertEquals(0, table.getEntry("rawfiducials").getDoubleArray(new double[0]).length);
    assertEquals(0, table.getEntry("botpose_wpiblue").getDoubleArray(new double[0]).length);
    assertTrue(limelight.createPoseEstimator(EstimationMode.MEGATAG1).getPoseEstimate().isEmpty());
  }

  @Test
  void visibleTagPublishesConsistentlyShapedArrays()
  {
    // A narrow FOV keeps this deterministic: only the tag dead ahead can possibly be seen, regardless of how the
    // official field happens to cluster other tags nearby.
    AprilTag tag = fieldLayout.getTags().get(0);
    Limelight limelight = new Limelight("limelight-sim-it-schema");
    LimelightSim sim = new LimelightSim(limelight, LimelightSimSettings.perfect().withFOV(10, 10));

    sim.update(poseFacingTag(tag, 4.0));

    NetworkTable table = limelight.getNTTable();
    assertEquals(1, table.getEntry("tv").getDouble(-1));
    assertEquals(tag.ID, (int) table.getEntry("tid").getDouble(-1));
    assertEquals(7, table.getEntry("rawfiducials").getDoubleArray(new double[0]).length);
    assertEquals(18, table.getEntry("botpose_wpiblue").getDoubleArray(new double[0]).length);
    assertEquals(18, table.getEntry("botpose_orb_wpiblue").getDoubleArray(new double[0]).length);
    assertEquals(17, table.getEntry("t2d").getDoubleArray(new double[0]).length);
    double ta = table.getEntry("ta").getDouble(-1);
    assertTrue(ta > 0 && ta <= 100, "ta should be a percentage of the image, was " + ta);
    assertEquals(0, table.getEntry("tx").getDouble(99), 2.0);
  }

  @Test
  void perfectSettingsPublishNoiseFreeBotpose()
  {
    AprilTag tag = fieldLayout.getTags().get(1);
    Pose3d robotPose = poseFacingTag(tag, 4.0);
    Limelight limelight = new Limelight("limelight-sim-it-perfect");
    LimelightSim sim = new LimelightSim(limelight, LimelightSimSettings.perfect());

    sim.update(robotPose);

    NetworkTable table = limelight.getNTTable();
    double[] botpose = table.getEntry("botpose_wpiblue").getDoubleArray(new double[0]);
    assertEquals(robotPose.getX(), botpose[0], 1e-6);
    assertEquals(robotPose.getY(), botpose[1], 1e-6);
    assertEquals(robotPose.getZ(), botpose[2], 1e-6);

    double[] stddevs = table.getEntry("stddevs").getDoubleArray(new double[0]);
    for (double stddev : stddevs)
    {
      assertEquals(0, stddev);
    }
  }

  @Test
  void redAlliancePoseIsBlueReflectedAboutFieldCenter()
  {
    AprilTag tag = fieldLayout.getTags().get(2);
    Limelight limelight = new Limelight("limelight-sim-it-red");
    LimelightSim sim = new LimelightSim(limelight, LimelightSimSettings.perfect());

    sim.update(poseFacingTag(tag, 4.0));

    NetworkTable table = limelight.getNTTable();
    double[] blue = table.getEntry("botpose_wpiblue").getDoubleArray(new double[0]);
    double[] red = table.getEntry("botpose_wpired").getDoubleArray(new double[0]);

    assertEquals(fieldLayout.getFieldLength() - blue[0], red[0], 1e-6);
    assertEquals(fieldLayout.getFieldWidth() - blue[1], red[1], 1e-6);
    assertEquals(blue[2], red[2], 1e-6);
    double expectedYaw = MathUtil.inputModulus(blue[5] + 180, -180, 180);
    assertEquals(expectedYaw, MathUtil.inputModulus(red[5], -180, 180), 1e-6);
  }

  @Test
  void megaTag2UsesSuppliedRobotOrientationYawInsteadOfGroundTruth()
  {
    AprilTag tag = fieldLayout.getTags().get(3);
    Pose3d robotPose = poseFacingTag(tag, 4.0);
    Limelight limelight = new Limelight("limelight-sim-it-mt2");
    LimelightSim sim = new LimelightSim(limelight, LimelightSimSettings.perfect());

    // Same NT array shape LimelightSettings#withRobotOrientation publishes: [yaw, yawRate, pitch, pitchRate, roll, rollRate]
    limelight.getNTTable().getDoubleArrayTopic("robot_orientation_set").getEntry(new double[0])
             .set(new double[]{45, 0, 0, 0, 0, 0});

    sim.update(robotPose);

    NetworkTable table = limelight.getNTTable();
    double[] mt1 = table.getEntry("botpose_wpiblue").getDoubleArray(new double[0]);
    double[] mt2 = table.getEntry("botpose_orb_wpiblue").getDoubleArray(new double[0]);

    assertEquals(Math.toDegrees(robotPose.getRotation().getZ()), mt1[5], 1e-6);
    assertEquals(45.0, mt2[5], 1e-6);
  }

  @Test
  void poseEstimatorReadsBackWhatLimelightSimPublished()
  {
    AprilTag tag = fieldLayout.getTags().get(4);
    Pose3d robotPose = poseFacingTag(tag, 4.0);
    Limelight limelight = new Limelight("limelight-sim-it-estimator");
    LimelightSim sim = new LimelightSim(limelight, LimelightSimSettings.perfect().withFOV(10, 10));
    LimelightPoseEstimator poseEstimator = limelight.createPoseEstimator(EstimationMode.MEGATAG1);

    sim.update(robotPose);

    Optional<PoseEstimate> estimate = poseEstimator.getPoseEstimate();
    assertTrue(estimate.isPresent());
    assertEquals(1, estimate.get().tagCount);
    assertEquals(robotPose.getX(), estimate.get().pose.getX(), 1e-6);
    assertEquals(robotPose.getY(), estimate.get().pose.getY(), 1e-6);
    assertTrue(estimate.get().hasData);
  }

  @Test
  void jsonResultsRoundTripThroughLimelightResults()
  {
    AprilTag tag = fieldLayout.getTags().get(5);
    Limelight limelight = new Limelight("limelight-sim-it-json");
    LimelightSim sim = new LimelightSim(limelight, LimelightSimSettings.perfect().withFOV(10, 10));

    sim.update(poseFacingTag(tag, 4.0));

    Optional<LimelightResults> results = limelight.getLatestResults();
    assertTrue(results.isPresent());
    assertTrue(results.get().valid);
    assertEquals(1, results.get().targets_Fiducials.length);
    assertEquals(tag.ID, (int) results.get().targets_Fiducials[0].fiducialID);
    assertEquals(1.0, results.get().botpose_tagcount);
    assertFalse(results.get().pipeline_type.isEmpty());
  }

}
