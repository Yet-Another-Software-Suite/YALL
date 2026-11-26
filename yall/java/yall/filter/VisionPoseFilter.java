package yall.filter;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.DegreesPerSecond;
import static org.wpilib.units.Units.Meters;
import static org.wpilib.units.Units.Radians;
import static org.wpilib.units.Units.Seconds;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.wpilib.fields.Field;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.linalg.VecBuilder;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.AngularVelocity;
import org.wpilib.units.measure.Distance;
import org.wpilib.units.measure.Time;
import yall.results.VisionPoseEstimate;

/**
 * Decides which vision pose estimates to trust, and how much.
 *
 * <p>Each check is set with its {@code withX} method. Pass {@code null} to turn the check off:
 *
 * <pre>{@code
 * new VisionPoseFilter()
 *     .withMaxAmbiguity(0.15)       // tighter than the default
 *     .withMaxTilt(Degrees.of(10))  // on; off by default
 *     .withMaxYawRate(null);        // off
 * }</pre>
 *
 * <p>A new filter starts with these checks:
 *
 * <table>
 *   <caption>Checks and their defaults</caption>
 *   <tr><th>Check</th><th>Default</th><th>Rejects an estimate when</th></tr>
 *   <tr><td>{@link #withFieldBounds(Distance) Field bounds}</td><td>On, 0.5 m margin</td>
 *       <td>the robot would be off the field</td></tr>
 *   <tr><td>{@link #withMaxHeightError(Distance) Height}</td><td>On, 0.5 m</td>
 *       <td>the robot would be floating above or sunk below the floor</td></tr>
 *   <tr><td>{@link #withMaxTilt(Angle) Tilt}</td><td>Off</td>
 *       <td>the robot would be rolled or pitched further than the limit</td></tr>
 *   <tr><td>{@link #withMaxAmbiguity(Double) Ambiguity}</td><td>On, 0.2</td>
 *       <td>a single-tag estimate is too ambiguous</td></tr>
 *   <tr><td>{@link #withMinTagCount(Integer) Tag count}</td><td>On, 1 tag</td>
 *       <td>too few tags were used</td></tr>
 *   <tr><td>{@link #withMaxTagDistance(Distance, Distance) Tag distance}</td>
 *       <td>On, 4 m for one tag, 8 m for several</td><td>the tags are too far away</td></tr>
 *   <tr><td>{@link #withMaxYawRate(AngularVelocity) Yaw rate}</td><td>On, 720 °/s</td>
 *       <td>the robot was spinning too fast, which blurs the image (needs {@link
 *       yall.VisionCamera#setRobotOrientation})</td></tr>
 *   <tr><td>{@link #withMaxAge(Time) Age}</td><td>On, 0.5 s</td>
 *       <td>the image is too old, or claims to be from the future</td></tr>
 *   <tr><td>{@link #withMaxDistanceFromPose(Supplier, Distance) Pose jump}</td><td>Off</td>
 *       <td>the estimate is too far from the robot's current pose</td></tr>
 * </table>
 *
 * <p>Add your own checks with {@link #withCustomCheck(String, Predicate)}. Each has a name, which is
 * the rejection reason and the name its rejections are counted under in {@link PoseFilterStats}.
 *
 * <p>Estimates that used no tags are always rejected.
 *
 * <p>Accepted estimates get standard deviations that grow with the square of the tag distance:
 * {@code base * (1 + distance² / distanceDivisor)}, with a smaller base for multi-tag estimates. See
 * {@link #withStdDevs(Matrix, Matrix, double)}.
 */
public class VisionPoseFilter {

  private Optional<Distance> fieldBoundsMargin = Optional.of(Meters.of(0.5));
  private Optional<Distance> maxHeightError = Optional.of(Meters.of(0.5));
  private Optional<Angle> maxTilt = Optional.empty();
  private Optional<Double> maxAmbiguity = Optional.of(0.2);
  private Optional<Integer> minTagCount = Optional.of(1);
  private Optional<Distance> maxSingleTagDistance = Optional.of(Meters.of(4));
  private Optional<Distance> maxMultiTagDistance = Optional.of(Meters.of(8));
  private Optional<AngularVelocity> maxYawRate = Optional.of(DegreesPerSecond.of(720));
  private Optional<Time> maxAge = Optional.of(Seconds.of(0.5));
  private Optional<Supplier<Pose2d>> currentPose = Optional.empty();
  private Optional<Distance> maxDistanceFromPose = Optional.empty();
  private final Map<String, Predicate<VisionPoseEstimate>> customChecks = new LinkedHashMap<>();

  private Matrix<N3, N1> singleTagStdDevs = VecBuilder.fill(4, 4, 8);
  private Matrix<N3, N1> multiTagStdDevs = VecBuilder.fill(0.5, 0.5, 1);
  private double distanceDivisor = 30;
  private double headingAssistedThetaStdDev = 1e6;

  /** Creates a filter with the default checks described in the class documentation. */
  public VisionPoseFilter() {}

  /**
   * Creates a filter that accepts every estimate that used a tag. Turn checks on with the {@code
   * withX} methods.
   *
   * @return A filter with every check off.
   */
  public static VisionPoseFilter acceptAll() {
    return new VisionPoseFilter()
        .withFieldBounds(null)
        .withMaxHeightError(null)
        .withMaxTilt(null)
        .withMaxAmbiguity(null)
        .withMinTagCount(null)
        .withMaxTagDistance(null, null)
        .withMaxYawRate(null)
        .withMaxAge(null)
        .withMaxDistanceFromPose(null, null);
  }

  /**
   * Rejects estimates that put the robot off the field.
   *
   * @param margin How far past the field edge to still accept, for measurement error. null turns
   *     the check off.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withFieldBounds(Distance margin) {
    fieldBoundsMargin = Optional.ofNullable(margin);
    return this;
  }

  /**
   * Rejects estimates that put the robot above or below the floor.
   *
   * @param maxHeightError Largest accepted distance between the estimated robot origin and the
   *     floor. null turns the check off.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxHeightError(Distance maxHeightError) {
    this.maxHeightError = Optional.ofNullable(maxHeightError);
    return this;
  }

  /**
   * Rejects estimates that roll or pitch the robot further than the limit. Leave this off if the
   * robot drives over ramps or bumps.
   *
   * @param maxTilt Largest accepted roll or pitch. null turns the check off.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxTilt(Angle maxTilt) {
    this.maxTilt = Optional.ofNullable(maxTilt);
    return this;
  }

  /**
   * Rejects single-tag estimates whose pose ambiguity is above the limit. Multi-tag estimates and
   * cameras that don't report ambiguity aren't checked.
   *
   * @param ambiguity Largest accepted ambiguity, 0 to 1. null turns the check off.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxAmbiguity(Double ambiguity) {
    maxAmbiguity = Optional.ofNullable(ambiguity);
    return this;
  }

  /**
   * Rejects estimates that used fewer tags than the limit. Estimates that used no tags are always
   * rejected.
   *
   * @param tagCount Fewest tags accepted. null turns the check off.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMinTagCount(Integer tagCount) {
    minTagCount = Optional.ofNullable(tagCount);
    return this;
  }

  /**
   * Rejects estimates whose tags are, on average, further away than the limit.
   *
   * @param maxSingleTagDistance Limit for estimates from one tag. null turns this limit off.
   * @param maxMultiTagDistance Limit for estimates from several tags. null turns this limit off.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxTagDistance(
      Distance maxSingleTagDistance, Distance maxMultiTagDistance) {
    this.maxSingleTagDistance = Optional.ofNullable(maxSingleTagDistance);
    this.maxMultiTagDistance = Optional.ofNullable(maxMultiTagDistance);
    return this;
  }

  /**
   * Rejects estimates taken while the robot spun faster than the limit. Needs the yaw rate from
   * {@link yall.VisionCamera#setRobotOrientation}; without it, estimates aren't checked.
   *
   * @param maxYawRate Fastest accepted spin. null turns the check off.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxYawRate(AngularVelocity maxYawRate) {
    this.maxYawRate = Optional.ofNullable(maxYawRate);
    return this;
  }

  /**
   * Rejects estimates from images older than the limit, and from images timestamped in the future.
   *
   * @param maxAge Oldest accepted image. null turns the check off.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxAge(Time maxAge) {
    this.maxAge = Optional.ofNullable(maxAge);
    return this;
  }

  /**
   * Rejects estimates further than the limit from the robot's current pose. Turn this on only once
   * the robot knows where it is, or vision can never correct a bad starting pose.
   *
   * @param currentPose Supplies the robot's current pose, usually from the drivetrain's pose
   *     estimator. null turns the check off.
   * @param maxDistance Largest accepted distance from the current pose. null turns the check off.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxDistanceFromPose(
      Supplier<Pose2d> currentPose, Distance maxDistance) {
    boolean on = currentPose != null && maxDistance != null;
    this.currentPose = on ? Optional.of(currentPose) : Optional.empty();
    maxDistanceFromPose = on ? Optional.of(maxDistance) : Optional.empty();
    return this;
  }

  /**
   * Adds a check of your own, run after the built-in ones. Its name is the rejection reason, and
   * {@link PoseFilterStats} counts its rejections under that name.
   *
   * <pre>{@code
   * filter.withCustomCheck("not in our half", e -> e.pose().getX() < 8.27);
   * }</pre>
   *
   * @param name Name of the check, shown as the rejection reason. Must not be empty. A check with
   *     the same name is replaced.
   * @param accepts Returns true to accept an estimate. null removes the check.
   * @return This filter, for chaining.
   * @throws IllegalArgumentException if the name is null or blank.
   */
  public VisionPoseFilter withCustomCheck(String name, Predicate<VisionPoseEstimate> accepts) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("A custom check needs a name, for its rejection stats");
    }
    if (accepts == null) {
      customChecks.remove(name);
    } else {
      customChecks.put(name, accepts);
    }
    return this;
  }

  /**
   * The custom checks, by name.
   *
   * @return The checks, in the order they were added.
   */
  public Map<String, Predicate<VisionPoseEstimate>> getCustomChecks() {
    return Collections.unmodifiableMap(customChecks);
  }

  /**
   * The divisor for the squared tag distance in the standard deviation formula.
   *
   * @return The divisor.
   */
  public double getDistanceDivisor() {
    return distanceDivisor;
  }

  /**
   * Sets how standard deviations are computed for accepted estimates: {@code base * (1 + d² /
   * distanceDivisor)}, where d is the average tag distance in meters. A smaller divisor makes
   * distant tags count for less.
   *
   * @param singleTagBase Base standard deviations of x (m), y (m) and heading (rad) for one tag.
   * @param multiTagBase Base standard deviations for several tags.
   * @param distanceDivisor Divisor for the squared tag distance.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withStdDevs(
      Matrix<N3, N1> singleTagBase, Matrix<N3, N1> multiTagBase, double distanceDivisor) {
    singleTagStdDevs = singleTagBase;
    multiTagStdDevs = multiTagBase;
    this.distanceDivisor = distanceDivisor;
    return this;
  }

  /**
   * Sets the heading standard deviation for heading-assisted estimates (Limelight MegaTag2,
   * PhotonVision's trig solve). Their heading is the one given to {@code setRobotOrientation}, so it
   * should carry almost no weight.
   *
   * @param thetaStdDev Heading standard deviation.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withHeadingAssistedThetaStdDev(Angle thetaStdDev) {
    headingAssistedThetaStdDev = thetaStdDev.in(Radians);
    return this;
  }

  /**
   * The field bounds margin.
   *
   * @return The margin, or empty if the check is off.
   */
  public Optional<Distance> getFieldBoundsMargin() {
    return fieldBoundsMargin;
  }

  /**
   * The largest accepted height error.
   *
   * @return The limit, or empty if the check is off.
   */
  public Optional<Distance> getMaxHeightError() {
    return maxHeightError;
  }

  /**
   * The largest accepted roll or pitch.
   *
   * @return The limit, or empty if the check is off.
   */
  public Optional<Angle> getMaxTilt() {
    return maxTilt;
  }

  /**
   * The largest accepted single-tag ambiguity.
   *
   * @return The limit, or empty if the check is off.
   */
  public Optional<Double> getMaxAmbiguity() {
    return maxAmbiguity;
  }

  /**
   * The fewest tags accepted.
   *
   * @return The limit, or empty if the check is off.
   */
  public Optional<Integer> getMinTagCount() {
    return minTagCount;
  }

  /**
   * The largest accepted average tag distance for a single-tag estimate.
   *
   * @return The limit, or empty if it is off.
   */
  public Optional<Distance> getMaxSingleTagDistance() {
    return maxSingleTagDistance;
  }

  /**
   * The largest accepted average tag distance for a multi-tag estimate.
   *
   * @return The limit, or empty if it is off.
   */
  public Optional<Distance> getMaxMultiTagDistance() {
    return maxMultiTagDistance;
  }

  /**
   * The fastest accepted spin.
   *
   * @return The limit, or empty if the check is off.
   */
  public Optional<AngularVelocity> getMaxYawRate() {
    return maxYawRate;
  }

  /**
   * The oldest accepted image.
   *
   * @return The limit, or empty if the check is off.
   */
  public Optional<Time> getMaxAge() {
    return maxAge;
  }

  /**
   * The largest accepted distance from the current pose.
   *
   * @return The limit, or empty if the check is off.
   */
  public Optional<Distance> getMaxDistanceFromPose() {
    return maxDistanceFromPose;
  }

  /**
   * Checks an estimate.
   *
   * @param estimate Estimate to check.
   * @param field Field the robot is on, for the bounds check. null skips that check.
   * @param nowSeconds Current {@link org.wpilib.system.Timer#getMonotonicTimestamp()}.
   * @param yawRateDegreesPerSecond Robot yaw rate when the image was taken, or {@link Double#NaN}
   *     if unknown.
   * @return Why the estimate was rejected, or empty if it passes every enabled check.
   */
  public Optional<String> check(
      VisionPoseEstimate estimate, Field field, double nowSeconds, double yawRateDegreesPerSecond) {
    var pose = estimate.pose();
    int tags = estimate.tagCount();
    if (tags == 0) {
      return Optional.of("no tags");
    }
    if (minTagCount.isPresent() && tags < minTagCount.get()) {
      return Optional.of("too few tags");
    }
    if (maxAge.isPresent()) {
      double age = nowSeconds - estimate.timestampSeconds();
      if (age > maxAge.get().in(Seconds)) {
        return Optional.of("too old");
      }
      if (age < -0.05) {
        return Optional.of("timestamp in the future");
      }
    }
    if (fieldBoundsMargin.isPresent() && field != null) {
      double margin = fieldBoundsMargin.get().in(Meters);
      if (pose.getX() < -margin
          || pose.getY() < -margin
          || pose.getX() > field.getFieldLength() + margin
          || pose.getY() > field.getFieldWidth() + margin) {
        return Optional.of("off the field");
      }
    }
    if (maxHeightError.isPresent() && Math.abs(pose.getZ()) > maxHeightError.get().in(Meters)) {
      return Optional.of("off the floor");
    }
    if (maxTilt.isPresent()) {
      double max = maxTilt.get().in(Degrees);
      if (Math.abs(Math.toDegrees(pose.getRotation().getX())) > max
          || Math.abs(Math.toDegrees(pose.getRotation().getY())) > max) {
        return Optional.of("tilted");
      }
    }
    if (maxAmbiguity.isPresent()
        && tags == 1
        && !Double.isNaN(estimate.ambiguity())
        && estimate.ambiguity() > maxAmbiguity.get()) {
      return Optional.of("ambiguous");
    }
    var maxDistance = tags == 1 ? maxSingleTagDistance : maxMultiTagDistance;
    if (maxDistance.isPresent()
        && estimate.averageTagDistanceMeters() > maxDistance.get().in(Meters)) {
      return Optional.of("tags too far");
    }
    if (maxYawRate.isPresent()
        && !Double.isNaN(yawRateDegreesPerSecond)
        && Math.abs(yawRateDegreesPerSecond) > maxYawRate.get().in(DegreesPerSecond)) {
      return Optional.of("spinning too fast");
    }
    if (currentPose.isPresent()) {
      Pose2d current = currentPose.get().get();
      if (current != null
          && current.getTranslation().getDistance(pose.toPose2d().getTranslation())
              > maxDistanceFromPose.get().in(Meters)) {
        return Optional.of("too far from the current pose");
      }
    }
    for (var check : customChecks.entrySet()) {
      if (!check.getValue().test(estimate)) {
        return Optional.of(check.getKey());
      }
    }
    return Optional.empty();
  }

  /**
   * Computes the standard deviations for an estimate.
   *
   * @param estimate Estimate to weigh.
   * @return The estimate with {@link VisionPoseEstimate#stdDevs()} filled in.
   */
  public VisionPoseEstimate applyStdDevs(VisionPoseEstimate estimate) {
    var base = estimate.tagCount() > 1 ? multiTagStdDevs : singleTagStdDevs;
    double distance = estimate.averageTagDistanceMeters();
    double scale = 1 + (Double.isFinite(distance) ? distance * distance / distanceDivisor : 0);
    double theta = estimate.headingAssisted() ? headingAssistedThetaStdDev : base.get(2, 0) * scale;
    return estimate.withStdDevs(
        VecBuilder.fill(base.get(0, 0) * scale, base.get(1, 0) * scale, theta));
  }
}
