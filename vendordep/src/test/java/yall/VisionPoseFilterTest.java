package yall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.DegreesPerSecond;
import static org.wpilib.units.Units.Meters;
import static org.wpilib.units.Units.Seconds;

import java.util.List;
import java.util.Optional;
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

class VisionPoseFilterTest {

  private static final Field FIELD = Field.loadField(Fields.DEFAULT_FIELD);
  private static final double NOW = 100;

  private static VisionPoseEstimate estimate(
      Pose3d pose, double timestamp, List<Integer> tags, double distance, double ambiguity) {
    return VisionPoseEstimate.unfiltered("test", pose, timestamp, tags, distance, ambiguity, false);
  }

  private static VisionPoseEstimate good() {
    return estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW - 0.05, List.of(1), 2, 0.05);
  }

  private static Optional<String> check(VisionPoseFilter filter, VisionPoseEstimate e) {
    return filter.check(e, FIELD, NOW, Double.NaN);
  }

  @Test
  void defaultsAcceptAGoodEstimate() {
    assertEquals(Optional.empty(), check(new VisionPoseFilter(), good()));
  }

  @Test
  void rejectsEstimatesWithNoTagsEvenWithEveryCheckOff() {
    var e = estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW, List.of(), 2, 0);
    assertTrue(check(VisionPoseFilter.acceptAll(), e).isPresent());
  }

  @Test
  void fieldBoundsCanBeTurnedOffAndResized() {
    var off = estimate(new Pose3d(-1, 4, 0, Rotation3d.ZERO), NOW, List.of(1), 2, 0.05);
    assertEquals(Optional.of("off the field"), check(new VisionPoseFilter(), off));
    assertEquals(Optional.empty(), check(new VisionPoseFilter().withoutFieldBounds(), off));
    assertEquals(
        Optional.empty(), check(new VisionPoseFilter().withFieldBounds(Meters.of(1.5)), off));
  }

  @Test
  void heightCanBeTurnedOffAndResized() {
    var high = estimate(new Pose3d(5, 4, 0.8, Rotation3d.ZERO), NOW, List.of(1), 2, 0.05);
    assertEquals(Optional.of("off the floor"), check(new VisionPoseFilter(), high));
    assertEquals(Optional.empty(), check(new VisionPoseFilter().withoutMaxHeightError(), high));
    assertEquals(
        Optional.empty(), check(new VisionPoseFilter().withMaxHeightError(Meters.of(1)), high));
  }

  @Test
  void tiltIsOffByDefault() {
    var tilted =
        estimate(
            new Pose3d(5, 4, 0, new Rotation3d(0, Math.toRadians(20), 0)), NOW, List.of(1), 2, 0);
    assertEquals(Optional.empty(), check(new VisionPoseFilter(), tilted));
    assertEquals(
        Optional.of("tilted"), check(new VisionPoseFilter().withMaxTilt(Degrees.of(10)), tilted));
  }

  @Test
  void ambiguityAppliesOnlyToKnownSingleTagAmbiguity() {
    var ambiguous = estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW, List.of(1), 2, 0.5);
    assertEquals(Optional.of("ambiguous"), check(new VisionPoseFilter(), ambiguous));
    assertEquals(Optional.empty(), check(new VisionPoseFilter().withMaxAmbiguity(0.6), ambiguous));
    assertEquals(Optional.empty(), check(new VisionPoseFilter().withoutMaxAmbiguity(), ambiguous));
    var unknown = estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW, List.of(1), 2, Double.NaN);
    assertEquals(Optional.empty(), check(new VisionPoseFilter(), unknown));
  }

  @Test
  void tagCountCanBeRaised() {
    assertEquals(
        Optional.of("too few tags"), check(new VisionPoseFilter().withMinTagCount(2), good()));
    assertEquals(
        Optional.empty(),
        check(new VisionPoseFilter().withMinTagCount(2).withoutMinTagCount(), good()));
  }

  @Test
  void tagDistanceHasSingleAndMultiTagLimits() {
    var far = estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW, List.of(1), 5, 0.05);
    var farMulti = estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW, List.of(1, 2), 5, 0);
    assertEquals(Optional.of("tags too far"), check(new VisionPoseFilter(), far));
    assertEquals(Optional.empty(), check(new VisionPoseFilter(), farMulti));
    assertEquals(Optional.empty(), check(new VisionPoseFilter().withoutMaxTagDistance(), far));
    assertEquals(
        Optional.of("tags too far"),
        check(new VisionPoseFilter().withMaxTagDistance(Meters.of(4), Meters.of(3)), farMulti));
  }

  @Test
  void yawRateNeedsARate() {
    var filter = new VisionPoseFilter();
    assertEquals(Optional.of("spinning too fast"), filter.check(good(), FIELD, NOW, 800));
    assertEquals(Optional.empty(), filter.check(good(), FIELD, NOW, Double.NaN));
    assertEquals(
        Optional.empty(),
        filter.withMaxYawRate(DegreesPerSecond.of(900)).check(good(), FIELD, NOW, 800));
    assertEquals(Optional.empty(), filter.withoutMaxYawRate().check(good(), FIELD, NOW, 2000));
  }

  @Test
  void ageRejectsOldAndFutureImages() {
    var old = estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW - 1, List.of(1), 2, 0.05);
    var future = estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW + 1, List.of(1), 2, 0.05);
    assertEquals(Optional.of("too old"), check(new VisionPoseFilter(), old));
    assertEquals(Optional.of("timestamp in the future"), check(new VisionPoseFilter(), future));
    assertEquals(Optional.empty(), check(new VisionPoseFilter().withMaxAge(Seconds.of(2)), old));
    assertEquals(Optional.empty(), check(new VisionPoseFilter().withoutMaxAge(), old));
  }

  @Test
  void poseJumpIsOffByDefault() {
    var filter = new VisionPoseFilter();
    assertEquals(Optional.empty(), check(filter, good()));
    filter.withMaxDistanceFromPose(
        () -> new Pose2d(1, 1, Rotation2d.ZERO), Meters.of(1));
    assertEquals(Optional.of("too far from the current pose"), check(filter, good()));
    assertEquals(Optional.empty(), check(filter.withoutMaxDistanceFromPose(), good()));
  }

  @Test
  void stdDevsGrowWithDistanceAndShrinkWithTags() {
    var filter =
        new VisionPoseFilter().withStdDevs(VecBuilder.fill(4, 4, 8), VecBuilder.fill(1, 1, 2), 30);
    var near = filter.applyStdDevs(good());
    var far =
        filter.applyStdDevs(
            estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW, List.of(1), 3, 0.05));
    var multi =
        filter.applyStdDevs(
            estimate(new Pose3d(5, 4, 0, Rotation3d.ZERO), NOW, List.of(1, 2), 2, 0));
    assertEquals(4 * (1 + 4 / 30.0), near.stdDevs().get(0, 0), 1e-9);
    assertTrue(far.stdDevs().get(0, 0) > near.stdDevs().get(0, 0));
    assertEquals(1 * (1 + 4 / 30.0), multi.stdDevs().get(0, 0), 1e-9);
  }

  @Test
  void headingAssistedEstimatesBarelyTrustTheirHeading() {
    var e = VisionPoseEstimate.unfiltered("test", new Pose3d(), NOW, List.of(1), 2, 0, true);
    var weighed = new VisionPoseFilter().withHeadingAssistedThetaStdDev(123).applyStdDevs(e);
    assertEquals(123, weighed.stdDevs().get(2, 0));
  }
}
