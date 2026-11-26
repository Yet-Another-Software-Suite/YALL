package yall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.DegreesPerSecond;
import static org.wpilib.units.Units.Meters;
import static org.wpilib.units.Units.Millimeters;
import static org.wpilib.units.Units.Seconds;

import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.wpilib.fields.Field;
import org.wpilib.fields.Fields;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.linalg.VecBuilder;
import yall.filter.VisionPoseFilter;
import yall.results.VisionPoseEstimate;
import yall.sim.VisionSim;

class VisionFilterAndSimParityTest {

  private static final Field FIELD = Field.loadField(Fields.DEFAULT_FIELD);
  private static final double NOW = 100.0;

  private static VisionPoseEstimate estimate(
      Pose3d pose, double timestamp, List<Integer> tags, double distance, double ambiguity) {
    return VisionPoseEstimate.unfiltered("test", pose, timestamp, tags, distance, ambiguity, false);
  }

  @Test
  void windowedPeakYawRateRejectsRecentSpinBurstEvenWhenInstantRateIsZero() {
    var filter =
        new VisionPoseFilter()
            .withMaxYawRate(DegreesPerSecond.of(300))
            .withYawRateWindow(Seconds.of(0.30));

    // Record a 450 deg/s spin spike at t = 99.80 s (0.15 s before capture at 99.95 s)
    filter.recordYawRate(99.80, 450.0);
    filter.recordYawRate(99.95, 0.0);

    var e = estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), 99.95, List.of(1, 2), 2.0, 0.0);
    assertEquals(Optional.of("spinning too fast"), filter.check(e, FIELD, NOW, 0.0));

    // Estimate well outside the +/-0.30 s window passes cleanly
    var later = estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), 100.50, List.of(1, 2), 2.0, 0.0);
    assertEquals(Optional.empty(), filter.check(later, FIELD, 100.55, 0.0));
  }

  @Test
  void minSingleTagAreaRejectsTinySingleTagAndIgnoresMultiTagOrUnknown() {
    var filter = new VisionPoseFilter().withMinSingleTagArea(0.15);
    var single = estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW - 0.02, List.of(1), 2.0, 0.05);
    var multi = estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW - 0.02, List.of(1, 2), 2.0, 0.0);

    assertEquals(Optional.of("tags too small"), filter.check(single, FIELD, NOW, 0.0, 0.08));
    assertEquals(Optional.empty(), filter.check(single, FIELD, NOW, 0.0, 0.25));
    assertEquals(Optional.empty(), filter.check(single, FIELD, NOW, 0.0, Double.NaN));
    assertEquals(Optional.empty(), filter.check(multi, FIELD, NOW, 0.0, 0.05));
  }

  @Test
  void dynamicStdDevScaleAndHighTagCountBoostTightenXyStdDevs() {
    AtomicReference<Double> modeScale = new AtomicReference<>(1.0);
    var filter =
        new VisionPoseFilter()
            .withStdDevs(VecBuilder.fill(4, 4, 8), VecBuilder.fill(1, 1, 2), 30)
            .withStdDevScale(modeScale::get)
            .withHighTagCountBoost(3, 0.35);

    var twoTags = estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW, List.of(1, 2), 2.0, 0.0);
    var threeTags =
        estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW, List.of(1, 2, 3), 2.0, 0.0);

    double baseTwo = filter.applyStdDevs(twoTags).stdDevs().get(0, 0);
    double boostedThree = filter.applyStdDevs(threeTags).stdDevs().get(0, 0);
    assertEquals(baseTwo * 0.35, boostedThree, 1e-9);

    // Post-bump snap mode (0.10x) tightens XY stddevs further
    modeScale.set(0.10);
    double snapTwo = filter.applyStdDevs(twoTags).stdDevs().get(0, 0);
    assertEquals(baseTwo * 0.10, snapTwo, 1e-9);
  }

  @Test
  void preEnableAlignmentBypassesPoseJumpRequiresMultiTagAndTightensHeading() {
    AtomicBoolean enabled = new AtomicBoolean(false);
    var filter =
        new VisionPoseFilter()
            .withMaxDistanceFromPose(() -> new Pose2d(1, 1, Rotation2d.ZERO), Meters.of(1.0))
            .withPreEnableAlignment(enabled::get, 2, Degrees.of(1.5));

    var farSingle = estimate(new Pose3d(6, 4, 0, Rotation3d.ZERO), NOW, List.of(1), 2.0, 0.05);
    var farMulti = estimate(new Pose3d(6, 4, 0, Rotation3d.ZERO), NOW, List.of(1, 2), 2.0, 0.0);

    // Disabled: single tag rejected (<2), multi-tag accepted even 5+ m away with tight heading
    assertTrue(filter.isPreEnableAlignmentActive());
    assertEquals(Optional.of("too few tags"), filter.check(farSingle, FIELD, NOW, 0.0));
    assertEquals(Optional.empty(), filter.check(farMulti, FIELD, NOW, 0.0));
    assertEquals(
        Math.toRadians(1.5), filter.applyStdDevs(farMulti).stdDevs().get(2, 0), 1e-9);

    // Enabled: pose-jump gate re-engages immediately
    enabled.set(true);
    assertFalse(filter.isPreEnableAlignmentActive());
    assertEquals(
        Optional.of("too far from the current pose"), filter.check(farMulti, FIELD, NOW, 0.0));
  }

  @Test
  void visionSimBlackoutDropoutAndFieldTagJitterAreDeterministic() {
    var simA = new VisionSim("FaultTestA", FIELD);
    var simB = new VisionSim("FaultTestB", FIELD);

    // 1. Deterministic >250 ms blackout window and clean recovery
    simA.injectBlackout(10.0, Seconds.of(0.30));
    assertTrue(simA.isBlackedOut(10.05));
    assertTrue(simA.shouldDropFrame(10.29));
    assertFalse(simA.isBlackedOut(10.31));
    assertFalse(simA.shouldDropFrame(10.31));

    // 2. Deterministic stochastic dropout matches bit-for-bit across same seed
    simA.clearFaults();
    simA.withDropoutProbability(0.25, 202701L);
    simB.withDropoutProbability(0.25, 202701L);
    int dropped = 0;
    for (int i = 0; i < 200; i++) {
      boolean dA = simA.shouldDropFrame(20.0 + i * 0.02);
      boolean dB = simB.shouldDropFrame(20.0 + i * 0.02);
      assertEquals(dA, dB);
      if (dA) {
        dropped++;
      }
    }
    assertTrue(dropped > 20 && dropped < 90, "Expected ~25% dropouts, got " + dropped);

    // 3. Deterministic +/-13 mm Welded/AndyMark field tag jitter preserves Z/rotation & matches seed
    simA.withFieldTagJitter(Millimeters.of(13), 202702L);
    simB.withFieldTagJitter(Millimeters.of(13), 202702L);
    var truth = new Pose3d(4.0, 3.0, 0.0, new Rotation3d(0.0, 0.0, 0.5));
    var jA = simA.applyFieldTagJitter(truth);
    var jB = simB.applyFieldTagJitter(truth);
    assertEquals(jA, jB);
    assertEquals(truth.getZ(), jA.getZ(), 1e-12);
    assertEquals(truth.getRotation(), jA.getRotation());
    assertTrue(jA.getTranslation().getDistance(truth.getTranslation()) > 0.0);
    assertTrue(jA.getTranslation().getDistance(truth.getTranslation()) < 0.15);
  }

  @Test
  void invalidFilterAndSimParametersThrowIllegalArgumentException() {
    var filter = new VisionPoseFilter();
    assertThrows(IllegalArgumentException.class, () -> filter.withMinSingleTagArea(-1.0));
    assertThrows(IllegalArgumentException.class, () -> filter.withMinSingleTagArea(101.0));
    assertThrows(IllegalArgumentException.class, () -> filter.withYawRateWindow(Seconds.of(0.0)));
    assertThrows(IllegalArgumentException.class, () -> filter.withStdDevScale(0.0));
    assertThrows(IllegalArgumentException.class, () -> filter.withHighTagCountBoost(1, 0.5));
    assertThrows(IllegalArgumentException.class, () -> filter.withHighTagCountBoost(3, -0.2));
    assertThrows(
        IllegalArgumentException.class,
        () -> filter.withStdDevs(VecBuilder.fill(1, 1, 1), VecBuilder.fill(1, 1, 1), 0.0));

    var sim = new VisionSim("InvalidParamTest", FIELD);
    assertThrows(IllegalArgumentException.class, () -> sim.withDropoutProbability(-0.1, 1L));
    assertThrows(IllegalArgumentException.class, () -> sim.withDropoutProbability(1.2, 1L));
    assertThrows(
        IllegalArgumentException.class, () -> sim.withFieldTagJitter(Meters.of(-0.01), 1L));
  }

  @Test
  void seededPropertyFuzzFilterProducesFinitePositiveStdDevsAndMonotonicDistanceScaling() {
    var rng = new SplittableRandom(20271125L);
    var filter =
        new VisionPoseFilter()
            .withMinSingleTagArea(0.05)
            .withYawRateWindow(Seconds.of(0.25))
            .withHighTagCountBoost(3, 0.35);

    for (int i = 0; i < 1_000; i++) {
      int tagCount = rng.nextInt(0, 5);
      var tagIds =
          switch (tagCount) {
            case 0 -> List.<Integer>of();
            case 1 -> List.of(1);
            case 2 -> List.of(1, 2);
            case 3 -> List.of(1, 2, 3);
            default -> List.of(1, 2, 3, 4);
          };
      double distNear = rng.nextDouble(0.5, 3.0);
      double distFar = distNear + rng.nextDouble(0.5, 4.0);
      boolean headingAssisted = rng.nextBoolean();
      var eNear =
          VisionPoseEstimate.unfiltered(
              "fuzz",
              new Pose3d(rng.nextDouble(1, 14), rng.nextDouble(1, 7), 0, Rotation3d.ZERO),
              NOW - 0.02,
              tagIds,
              distNear,
              tagCount == 1 ? rng.nextDouble(0.0, 0.5) : 0.0,
              headingAssisted);
      var eFar =
          VisionPoseEstimate.unfiltered(
              "fuzz",
              eNear.pose(),
              NOW - 0.02,
              tagIds,
              distFar,
              eNear.ambiguity(),
              headingAssisted);

      if (tagCount == 0) {
        assertTrue(filter.check(eNear, FIELD, NOW, 0.0, 1.0).isPresent());
        continue;
      }

      var wNear = filter.applyStdDevs(eNear).stdDevs();
      var wFar = filter.applyStdDevs(eFar).stdDevs();
      for (int row = 0; row < 3; row++) {
        assertTrue(Double.isFinite(wNear.get(row, 0)) && wNear.get(row, 0) > 0.0);
        assertTrue(Double.isFinite(wFar.get(row, 0)) && wFar.get(row, 0) > 0.0);
      }
      assertTrue(wFar.get(0, 0) > wNear.get(0, 0));
      assertTrue(wFar.get(1, 0) > wNear.get(1, 0));
    }
  }

  @Test
  void customChecksStillRunAfterNewBuiltInChecks() {
    var filter =
        new VisionPoseFilter()
            .withMinSingleTagArea(0.10)
            .withCustomCheck("our half only", e -> e.pose().getX() < 8.27);
    var smallTag = estimate(new Pose3d(10, 4, 0, Rotation3d.ZERO), NOW, List.of(1), 2.0, 0.05);
    var bigTagWrongHalf =
        estimate(new Pose3d(10, 4, 0, Rotation3d.ZERO), NOW, List.of(1), 2.0, 0.05);

    assertEquals(Optional.of("tags too small"), filter.check(smallTag, FIELD, NOW, 0.0, 0.05));
    assertEquals(
        Optional.of("our half only"), filter.check(bigTagWrongHalf, FIELD, NOW, 0.0, 0.50));
  }
}
