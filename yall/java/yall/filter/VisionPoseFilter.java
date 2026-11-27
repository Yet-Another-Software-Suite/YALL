package yall.filter;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.DegreesPerSecond;
import static org.wpilib.units.Units.Meters;
import static org.wpilib.units.Units.Radians;
import static org.wpilib.units.Units.Seconds;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
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
  private static final int YAW_RATE_RING_CAPACITY = 64;

  private Optional<Double> minSingleTagAreaPercent = Optional.empty();
  private Optional<Time> yawRateWindowHalf = Optional.empty();
  private final Map<String, Predicate<VisionPoseEstimate>> customChecks = new LinkedHashMap<>();

  private Matrix<N3, N1> singleTagStdDevs = VecBuilder.fill(4, 4, 8);
  private Matrix<N3, N1> multiTagStdDevs = VecBuilder.fill(0.5, 0.5, 1);
  private double distanceDivisor = 30;
  private double headingAssistedThetaStdDev = 1e6;
  private DoubleSupplier xyStdDevScaleSupplier = () -> 1.0;
  private Optional<Function<VisionPoseEstimate, Matrix<N3, N1>>> customStdDevCalculator =
      Optional.empty();
  private Optional<Integer> highTagBoostMinTags = Optional.empty();
  private double highTagBoostXyScale = 1.0;
  private Optional<BooleanSupplier> robotEnabledSupplier = Optional.empty();
  private int preEnableMinTagCount = 2;
  private double preEnableThetaStdDevRad = Math.toRadians(2.0);

  private final double[] yawTimestamps = new double[YAW_RATE_RING_CAPACITY];
  private final double[] yawAbsRatesDegPerSec = new double[YAW_RATE_RING_CAPACITY];
  private int yawRingHead = 0;
  private int yawRingSize = 0;

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
        .withMaxDistanceFromPose(null, null)
        .withMinSingleTagArea(null)
        .withYawRateWindow(null)
        .withPreEnableAlignment(null, null, null);
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
    Objects.requireNonNull(singleTagBase, "singleTagBase must not be null");
    Objects.requireNonNull(multiTagBase, "multiTagBase must not be null");
    if (!Double.isFinite(distanceDivisor) || distanceDivisor <= 0.0) {
      throw new IllegalArgumentException("distanceDivisor must be finite and positive");
    }
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
   * Rejects single-tag estimates whose tag covers less than the given percentage of the image.
   * Multi-tag estimates and cameras that don't report target area aren't checked.
   *
   * @param minAreaPercent Smallest accepted image area percentage (0 to 100). null turns the check
   *     off.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withMinSingleTagArea(Double minAreaPercent) {
    if (minAreaPercent != null
        && (!Double.isFinite(minAreaPercent) || minAreaPercent < 0.0 || minAreaPercent > 100.0)) {
      throw new IllegalArgumentException("minAreaPercent must be finite and in [0, 100]");
    }
    this.minSingleTagAreaPercent = Optional.ofNullable(minAreaPercent);
    return this;
  }

  /**
   * Checks the peak yaw rate over a symmetric window ({@code timestamp +/- halfWindow}) rather than
   * only the instantaneous yaw rate when the frame arrived.
   *
   * @param halfWindow Half-width of the yaw-rate window around each image timestamp. null turns
   *     windowed checking off.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withYawRateWindow(Time halfWindow) {
    if (halfWindow != null) {
      double sec = halfWindow.in(Seconds);
      if (!Double.isFinite(sec) || sec <= 0.0) {
        throw new IllegalArgumentException("halfWindow must be finite and positive");
      }
    }
    this.yawRateWindowHalf = Optional.ofNullable(halfWindow);
    return this;
  }

  /**
   * Records a timestamped robot yaw-rate sample for {@link #withYawRateWindow(Time)}. Called
   * automatically by {@link yall.VisionCamera#setRobotOrientation}.
   *
   * @param timestampSeconds Monotonic timestamp in seconds.
   * @param yawRateDegreesPerSecond Robot yaw rate in degrees per second.
   */
  public void recordYawRate(double timestampSeconds, double yawRateDegreesPerSecond) {
    if (!Double.isFinite(timestampSeconds) || !Double.isFinite(yawRateDegreesPerSecond)) {
      return;
    }
    yawTimestamps[yawRingHead] = timestampSeconds;
    yawAbsRatesDegPerSec[yawRingHead] = Math.abs(yawRateDegreesPerSecond);
    yawRingHead = (yawRingHead + 1) % YAW_RATE_RING_CAPACITY;
    if (yawRingSize < YAW_RATE_RING_CAPACITY) {
      yawRingSize++;
    }
  }

  /**
   * Applies a constant multiplier to accepted X/Y standard deviations (for example, tightening
   * trust during alignment or right after traversing a bump).
   *
   * @param xyScale Positive multiplier applied to X and Y standard deviations.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withStdDevScale(double xyScale) {
    if (!Double.isFinite(xyScale) || xyScale <= 0.0) {
      throw new IllegalArgumentException("xyScale must be finite and positive");
    }
    this.xyStdDevScaleSupplier = () -> xyScale;
    return this;
  }

  /**
   * Applies a dynamic multiplier to accepted X/Y standard deviations (for example, switching
   * between normal driving, close-in alignment, and post-bump relocalization snap modes).
   *
   * @param xyScaleSupplier Supplies a positive multiplier each time standard deviations are
   *     computed. null resets the scale to 1.0.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withStdDevScale(DoubleSupplier xyScaleSupplier) {
    this.xyStdDevScaleSupplier = xyScaleSupplier != null ? xyScaleSupplier : () -> 1.0;
    return this;
  }

  /**
   * Supplies a custom standard-deviation calculator that replaces the built-in {@code base * (1 +
   * d² / distanceDivisor)} model for accepted estimates (for example, {@code d^1.5 / tagCount} or
   * ambiguity-weighted covariance). If the calculator returns {@code null} or any non-finite /
   * non-positive entry, the filter safely falls back to its built-in formula.
   *
   * @param calculator Function computing {@code [x, y, theta]} standard deviations for an accepted
   *     estimate, or {@code null} to restore the built-in model.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withStdDevCalculator(
      Function<VisionPoseEstimate, Matrix<N3, N1>> calculator) {
    this.customStdDevCalculator = Optional.ofNullable(calculator);
    return this;
  }

  /**
   * Applies {@link PoseFilterStats#getSuggestedBaseStdDevs(double)} as the multi-tag base standard
   * deviations once at least {@code minSamples} accepted poses have been collected, clamping each
   * axis into {@code [minBase, maxBase]} so noisy calibration runs cannot produce extreme weights.
   *
   * @param stats Collected pose-filter statistics from a stationary calibration run.
   * @param minSamples Minimum accepted sample count required before applying (must be {@code >= 2}).
   * @param minBase Per-axis lower clamp {@code [x, y, theta]} (all finite and positive).
   * @param maxBase Per-axis upper clamp {@code [x, y, theta]} (all finite and {@code >= minBase}).
   * @return {@code true} if the base standard deviations were updated, {@code false} if not enough
   *     valid samples were available.
   */
  public boolean applySuggestedBaseStdDevs(
      PoseFilterStats stats,
      long minSamples,
      Matrix<N3, N1> minBase,
      Matrix<N3, N1> maxBase) {
    Objects.requireNonNull(stats, "stats must not be null");
    Objects.requireNonNull(minBase, "minBase must not be null");
    Objects.requireNonNull(maxBase, "maxBase must not be null");
    if (minSamples < 2) {
      throw new IllegalArgumentException("minSamples must be >= 2");
    }
    for (int i = 0; i < 3; i++) {
      double lo = minBase.get(i, 0);
      double hi = maxBase.get(i, 0);
      if (!Double.isFinite(lo) || !Double.isFinite(hi) || lo <= 0.0 || hi < lo) {
        throw new IllegalArgumentException("minBase/maxBase must be finite, positive, and ordered");
      }
    }
    if (stats.getAcceptedCount() < minSamples) {
      return false;
    }
    Matrix<N3, N1> suggested = stats.getSuggestedBaseStdDevs(distanceDivisor);
    double[] clamped = new double[3];
    for (int i = 0; i < 3; i++) {
      double v = suggested.get(i, 0);
      if (!Double.isFinite(v) || v <= 0.0) {
        return false;
      }
      clamped[i] = Math.max(minBase.get(i, 0), Math.min(maxBase.get(i, 0), v));
    }
    this.multiTagStdDevs = VecBuilder.fill(clamped[0], clamped[1], clamped[2]);
    return true;
  }

  /**
   * Applies an extra X/Y standard deviation multiplier when an estimate sees at least {@code
   * minTags} AprilTags (for example, trusting a 3+ tag shooter-camera solve more tightly).
   *
   * @param minTags Fewest tags required for the boost (must be {@code >= 2}). null turns the boost
   *     off.
   * @param xyScale Positive multiplier applied to X and Y standard deviations (e.g. {@code 0.35}).
   *     null turns the boost off.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withHighTagCountBoost(Integer minTags, Double xyScale) {
    if (minTags == null || xyScale == null) {
      this.highTagBoostMinTags = Optional.empty();
      this.highTagBoostXyScale = 1.0;
      return this;
    }
    if (minTags < 2) {
      throw new IllegalArgumentException("minTags must be >= 2");
    }
    if (!Double.isFinite(xyScale) || xyScale <= 0.0) {
      throw new IllegalArgumentException("xyScale must be finite and positive");
    }
    this.highTagBoostMinTags = Optional.of(minTags);
    this.highTagBoostXyScale = xyScale;
    return this;
  }

  /**
   * Enables pre-enable (disabled) field alignment: while {@code isEnabled} returns {@code false},
   * the pose-jump gate is bypassed so vision can snap to the field from an unknown starting pose,
   * at least {@code minPreEnableTags} tags are required, and heading uses {@code
   * preEnableThetaStdDev} so multi-tag solves can align the gyro before the match starts.
   *
   * @param isEnabled Supplies whether the robot is currently enabled (e.g. {@code
   *     DriverStation::isEnabled}). null turns pre-enable alignment off.
   * @param minPreEnableTags Fewest tags required while disabled (must be {@code >= 1}). null
   *     defaults to 2.
   * @param preEnableThetaStdDev Heading standard deviation while disabled. null defaults to 2 deg.
   * @return This filter, for chaining.
   */
  public VisionPoseFilter withPreEnableAlignment(
      BooleanSupplier isEnabled, Integer minPreEnableTags, Angle preEnableThetaStdDev) {
    if (isEnabled == null) {
      this.robotEnabledSupplier = Optional.empty();
      return this;
    }
    int tags = minPreEnableTags != null ? minPreEnableTags : 2;
    if (tags < 1) {
      throw new IllegalArgumentException("minPreEnableTags must be >= 1");
    }
    double thetaRad =
        preEnableThetaStdDev != null ? preEnableThetaStdDev.in(Radians) : Math.toRadians(2.0);
    if (!Double.isFinite(thetaRad) || thetaRad <= 0.0) {
      throw new IllegalArgumentException("preEnableThetaStdDev must be finite and positive");
    }
    this.robotEnabledSupplier = Optional.of(isEnabled);
    this.preEnableMinTagCount = tags;
    this.preEnableThetaStdDevRad = thetaRad;
    return this;
  }

  /**
   * The smallest accepted single-tag image area percentage.
   *
   * @return The limit, or empty if the check is off.
   */
  public Optional<Double> getMinSingleTagArea() {
    return minSingleTagAreaPercent;
  }

  /**
   * The yaw-rate window half-width.
   *
   * @return The window half-width, or empty if windowed checking is off.
   */
  public Optional<Time> getYawRateWindow() {
    return yawRateWindowHalf;
  }

  /**
   * The minimum tag count required to trigger the high-tag-count trust boost.
   *
   * @return The minimum tag count, or empty if the boost is off.
   */
  public Optional<Integer> getHighTagCountBoostMinTags() {
    return highTagBoostMinTags;
  }

  /**
   * Whether pre-enable alignment mode is currently active (configured and robot disabled).
   *
   * @return true if pre-enable alignment is active right now.
   */
  public boolean isPreEnableAlignmentActive() {
    return robotEnabledSupplier.isPresent() && !robotEnabledSupplier.get().getAsBoolean();
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
    return check(estimate, field, nowSeconds, yawRateDegreesPerSecond, Double.NaN);
  }

  /**
   * Checks an estimate with optional average AprilTag image area.
   *
   * @param estimate Estimate to check.
   * @param field Field the robot is on, for the bounds check. null skips that check.
   * @param nowSeconds Current {@link org.wpilib.system.Timer#getMonotonicTimestamp()}.
   * @param yawRateDegreesPerSecond Robot yaw rate when the image was taken, or {@link Double#NaN}
   *     if unknown.
   * @param averageTagAreaPercent Average image area percentage of the AprilTags in the frame, or
   *     {@link Double#NaN} if unknown.
   * @return Why the estimate was rejected, or empty if it passes every enabled check.
   */
  public Optional<String> check(
      VisionPoseEstimate estimate,
      Field field,
      double nowSeconds,
      double yawRateDegreesPerSecond,
      double averageTagAreaPercent) {
    var pose = estimate.pose();
    int tags = estimate.tagCount();
    boolean preEnable = isPreEnableAlignmentActive();
    if (tags == 0) {
      return Optional.of("no tags");
    }
    if (preEnable) {
      if (tags < preEnableMinTagCount) {
        return Optional.of("too few tags");
      }
    } else if (minTagCount.isPresent() && tags < minTagCount.get()) {
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
    if (minSingleTagAreaPercent.isPresent()
        && tags == 1
        && !Double.isNaN(averageTagAreaPercent)
        && averageTagAreaPercent < minSingleTagAreaPercent.get()) {
      return Optional.of("tags too small");
    }
    var maxDistance = tags == 1 ? maxSingleTagDistance : maxMultiTagDistance;
    if (maxDistance.isPresent()
        && estimate.averageTagDistanceMeters() > maxDistance.get().in(Meters)) {
      return Optional.of("tags too far");
    }
    if (maxYawRate.isPresent()) {
      double effectiveYawRate =
          effectiveAbsYawRate(estimate.timestampSeconds(), yawRateDegreesPerSecond);
      if (!Double.isNaN(effectiveYawRate)
          && effectiveYawRate > maxYawRate.get().in(DegreesPerSecond)) {
        return Optional.of("spinning too fast");
      }
    }
    if (!preEnable && currentPose.isPresent()) {
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

  private double effectiveAbsYawRate(double captureTimestampSec, double instantYawRateDegPerSec) {
    double peak =
        Double.isNaN(instantYawRateDegPerSec) ? Double.NaN : Math.abs(instantYawRateDegPerSec);
    if (yawRateWindowHalf.isEmpty() || yawRingSize == 0) {
      return peak;
    }
    double halfSec = yawRateWindowHalf.get().in(Seconds);
    double minT = captureTimestampSec - halfSec;
    double maxT = captureTimestampSec + halfSec;
    for (int i = 0; i < yawRingSize; i++) {
      double t = yawTimestamps[i];
      if (t >= minT && t <= maxT) {
        double r = yawAbsRatesDegPerSec[i];
        peak = Double.isNaN(peak) ? r : Math.max(peak, r);
      }
    }
    return peak;
  }

  /**
   * Computes the standard deviations for an estimate.
   *
   * @param estimate Estimate to weigh.
   * @return The estimate with {@link VisionPoseEstimate#stdDevs()} filled in.
   */
  public VisionPoseEstimate applyStdDevs(VisionPoseEstimate estimate) {
    if (customStdDevCalculator.isPresent()) {
      Matrix<N3, N1> custom = customStdDevCalculator.get().apply(estimate);
      if (isValidStdDevs(custom)) {
        return estimate.withStdDevs(custom);
      }
    }
    var base = estimate.tagCount() > 1 ? multiTagStdDevs : singleTagStdDevs;
    double distance = estimate.averageTagDistanceMeters();
    double scale = 1 + (Double.isFinite(distance) ? distance * distance / distanceDivisor : 0);

    double dynamicXyScale = xyStdDevScaleSupplier.getAsDouble();
    if (!Double.isFinite(dynamicXyScale) || dynamicXyScale <= 0.0) {
      dynamicXyScale = 1.0;
    }
    if (highTagBoostMinTags.isPresent() && estimate.tagCount() >= highTagBoostMinTags.get()) {
      dynamicXyScale *= highTagBoostXyScale;
    }

    double xyScale = scale * dynamicXyScale;
    double theta;
    if (isPreEnableAlignmentActive()) {
      theta = preEnableThetaStdDevRad;
    } else if (estimate.headingAssisted()) {
      theta = headingAssistedThetaStdDev;
    } else {
      theta = base.get(2, 0) * scale;
    }
    return estimate.withStdDevs(
        VecBuilder.fill(base.get(0, 0) * xyScale, base.get(1, 0) * xyScale, theta));
  }

  private static boolean isValidStdDevs(Matrix<N3, N1> stdDevs) {
    if (stdDevs == null) {
      return false;
    }
    for (int i = 0; i < 3; i++) {
      double v = stdDevs.get(i, 0);
      if (!Double.isFinite(v) || v <= 0.0) {
        return false;
      }
    }
    return true;
  }
}
