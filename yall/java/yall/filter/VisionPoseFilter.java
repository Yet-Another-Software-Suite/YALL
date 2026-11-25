package yall.filter;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.DegreesPerSecond;
import static org.wpilib.units.Units.Meters;
import static org.wpilib.units.Units.Seconds;

import java.util.Optional;
import java.util.OptionalDouble;
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
 * <p>Each check can be turned on with its {@code withX} method, which also sets its limits, and off
 * with its {@code withoutX} method. A new filter starts with these checks:
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
 *   <tr><td>{@link #withMaxAmbiguity(double) Ambiguity}</td><td>On, 0.2</td>
 *       <td>a single-tag estimate is too ambiguous</td></tr>
 *   <tr><td>{@link #withMinTagCount(int) Tag count}</td><td>On, 1 tag</td>
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
 * <p>Accepted estimates get standard deviations that grow with the square of the tag distance:
 * {@code base * (1 + distance² / distanceDivisor)}, with a smaller base for multi-tag estimates. See
 * {@link #withStdDevs(Matrix, Matrix, double)}.
 */
public class VisionPoseFilter {

  private OptionalDouble fieldBoundsMarginMeters = OptionalDouble.of(0.5);
  private OptionalDouble maxHeightErrorMeters = OptionalDouble.of(0.5);
  private OptionalDouble maxTiltDegrees = OptionalDouble.empty();
  private OptionalDouble maxAmbiguity = OptionalDouble.of(0.2);
  private int minTagCount = 1;
  private boolean tagCountEnabled = true;
  private OptionalDouble maxSingleTagDistanceMeters = OptionalDouble.of(4.0);
  private double maxMultiTagDistanceMeters = 8.0;
  private OptionalDouble maxYawRateDegreesPerSecond = OptionalDouble.of(720);
  private OptionalDouble maxAgeSeconds = OptionalDouble.of(0.5);
  private Optional<Supplier<Pose2d>> currentPose = Optional.empty();
  private double maxPoseJumpMeters = 1.0;

  private Matrix<N3, N1> singleTagStdDevs = VecBuilder.fill(4, 4, 8);
  private Matrix<N3, N1> multiTagStdDevs = VecBuilder.fill(0.5, 0.5, 1);
  private double distanceDivisor = 30;
  private double headingAssistedThetaStdDev = 1e6;

  /** Creates a filter with the default checks described in the class documentation. */
  public VisionPoseFilter() {}

  /**
   * Creates a filter that accepts every estimate. Turn checks on with the {@code withX} methods.
   *
   * @return A filter with every check off.
   */
  public static VisionPoseFilter acceptAll() {
    return new VisionPoseFilter()
        .withoutFieldBounds()
        .withoutMaxHeightError()
        .withoutMaxTilt()
        .withoutMaxAmbiguity()
        .withoutMinTagCount()
        .withoutMaxTagDistance()
        .withoutMaxYawRate()
        .withoutMaxAge()
        .withoutMaxDistanceFromPose();
  }

  /**
   * Rejects estimates that put the robot off the field.
   *
   * @param margin How far past the field edge to still accept, for measurement error.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withFieldBounds(Distance margin) {
    fieldBoundsMarginMeters = OptionalDouble.of(margin.in(Meters));
    return this;
  }

  /**
   * Stops checking that estimates are on the field.
   *
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withoutFieldBounds() {
    fieldBoundsMarginMeters = OptionalDouble.empty();
    return this;
  }

  /**
   * Rejects estimates that put the robot above or below the floor.
   *
   * @param maxHeightError Largest accepted distance between the estimated robot origin and the floor.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxHeightError(Distance maxHeightError) {
    maxHeightErrorMeters = OptionalDouble.of(maxHeightError.in(Meters));
    return this;
  }

  /**
   * Stops checking the estimated height.
   *
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withoutMaxHeightError() {
    maxHeightErrorMeters = OptionalDouble.empty();
    return this;
  }

  /**
   * Rejects estimates that roll or pitch the robot further than the limit. Leave this off if the
   * robot drives over ramps or bumps.
   *
   * @param maxTilt Largest accepted roll or pitch.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxTilt(Angle maxTilt) {
    maxTiltDegrees = OptionalDouble.of(maxTilt.in(Degrees));
    return this;
  }

  /**
   * Stops checking the estimated roll and pitch.
   *
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withoutMaxTilt() {
    maxTiltDegrees = OptionalDouble.empty();
    return this;
  }

  /**
   * Rejects single-tag estimates whose pose ambiguity is above the limit. Multi-tag estimates and
   * cameras that don't report ambiguity aren't checked.
   *
   * @param ambiguity Largest accepted ambiguity, 0 to 1.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxAmbiguity(double ambiguity) {
    maxAmbiguity = OptionalDouble.of(ambiguity);
    return this;
  }

  /**
   * Stops checking pose ambiguity.
   *
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withoutMaxAmbiguity() {
    maxAmbiguity = OptionalDouble.empty();
    return this;
  }

  /**
   * Rejects estimates that used fewer tags than the limit.
   *
   * @param tagCount Fewest tags accepted.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMinTagCount(int tagCount) {
    minTagCount = tagCount;
    tagCountEnabled = true;
    return this;
  }

  /**
   * Stops checking the tag count. Estimates that used no tags are still rejected.
   *
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withoutMinTagCount() {
    tagCountEnabled = false;
    return this;
  }

  /**
   * Rejects estimates whose tags are, on average, further away than the limit.
   *
   * @param maxSingleTagDistance Limit for estimates from one tag.
   * @param maxMultiTagDistance Limit for estimates from several tags.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxTagDistance(
      Distance maxSingleTagDistance, Distance maxMultiTagDistance) {
    maxSingleTagDistanceMeters = OptionalDouble.of(maxSingleTagDistance.in(Meters));
    maxMultiTagDistanceMeters = maxMultiTagDistance.in(Meters);
    return this;
  }

  /**
   * Stops checking tag distance.
   *
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withoutMaxTagDistance() {
    maxSingleTagDistanceMeters = OptionalDouble.empty();
    return this;
  }

  /**
   * Rejects estimates taken while the robot spun faster than the limit. Needs the yaw rate from
   * {@link yall.VisionCamera#setRobotOrientation}; without it, estimates aren't checked.
   *
   * @param maxYawRate Fastest accepted spin.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxYawRate(AngularVelocity maxYawRate) {
    maxYawRateDegreesPerSecond = OptionalDouble.of(maxYawRate.in(DegreesPerSecond));
    return this;
  }

  /**
   * Stops checking the yaw rate.
   *
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withoutMaxYawRate() {
    maxYawRateDegreesPerSecond = OptionalDouble.empty();
    return this;
  }

  /**
   * Rejects estimates from images older than the limit, and from images timestamped in the future.
   *
   * @param maxAge Oldest accepted image.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxAge(Time maxAge) {
    maxAgeSeconds = OptionalDouble.of(maxAge.in(Seconds));
    return this;
  }

  /**
   * Stops checking image age.
   *
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withoutMaxAge() {
    maxAgeSeconds = OptionalDouble.empty();
    return this;
  }

  /**
   * Rejects estimates further than the limit from the robot's current pose. Turn this on only once
   * the robot knows where it is, or vision can never correct a bad starting pose.
   *
   * @param currentPose Supplies the robot's current pose, usually from the drivetrain's pose
   *     estimator.
   * @param maxDistance Largest accepted distance from the current pose.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMaxDistanceFromPose(
      Supplier<Pose2d> currentPose, Distance maxDistance) {
    this.currentPose = Optional.of(currentPose);
    maxPoseJumpMeters = maxDistance.in(Meters);
    return this;
  }

  /**
   * Stops checking the distance from the current pose.
   *
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withoutMaxDistanceFromPose() {
    currentPose = Optional.empty();
    return this;
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
   * PhotonVision's trig solve). Their heading is the gyro's, so it should carry almost no weight.
   *
   * @param thetaStdDev Heading standard deviation in radians.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withHeadingAssistedThetaStdDev(double thetaStdDev) {
    headingAssistedThetaStdDev = thetaStdDev;
    return this;
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
    if (estimate.tagCount() == 0) {
      return Optional.of("no tags");
    }
    if (tagCountEnabled && estimate.tagCount() < minTagCount) {
      return Optional.of("too few tags");
    }
    if (maxAgeSeconds.isPresent()) {
      double age = nowSeconds - estimate.timestampSeconds();
      if (age > maxAgeSeconds.getAsDouble()) {
        return Optional.of("too old");
      }
      if (age < -0.05) {
        return Optional.of("timestamp in the future");
      }
    }
    if (fieldBoundsMarginMeters.isPresent() && field != null) {
      double margin = fieldBoundsMarginMeters.getAsDouble();
      if (pose.getX() < -margin
          || pose.getY() < -margin
          || pose.getX() > field.getFieldLength() + margin
          || pose.getY() > field.getFieldWidth() + margin) {
        return Optional.of("off the field");
      }
    }
    if (maxHeightErrorMeters.isPresent()
        && Math.abs(pose.getZ()) > maxHeightErrorMeters.getAsDouble()) {
      return Optional.of("off the floor");
    }
    if (maxTiltDegrees.isPresent()) {
      double max = maxTiltDegrees.getAsDouble();
      if (Math.abs(Math.toDegrees(pose.getRotation().getX())) > max
          || Math.abs(Math.toDegrees(pose.getRotation().getY())) > max) {
        return Optional.of("tilted");
      }
    }
    if (maxAmbiguity.isPresent()
        && estimate.tagCount() == 1
        && !Double.isNaN(estimate.ambiguity())
        && estimate.ambiguity() > maxAmbiguity.getAsDouble()) {
      return Optional.of("ambiguous");
    }
    if (maxSingleTagDistanceMeters.isPresent()) {
      double max =
          estimate.tagCount() == 1
              ? maxSingleTagDistanceMeters.getAsDouble()
              : maxMultiTagDistanceMeters;
      if (estimate.averageTagDistanceMeters() > max) {
        return Optional.of("tags too far");
      }
    }
    if (maxYawRateDegreesPerSecond.isPresent()
        && !Double.isNaN(yawRateDegreesPerSecond)
        && Math.abs(yawRateDegreesPerSecond) > maxYawRateDegreesPerSecond.getAsDouble()) {
      return Optional.of("spinning too fast");
    }
    if (currentPose.isPresent()) {
      Pose2d current = currentPose.get().get();
      if (current != null
          && current.getTranslation().getDistance(pose.toPose2d().getTranslation())
              > maxPoseJumpMeters) {
        return Optional.of("too far from the current pose");
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
