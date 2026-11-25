package yall.filter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.linalg.VecBuilder;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import org.wpilib.math.util.MathUtil;
import yall.results.VisionPoseEstimate;

/**
 * Counts of what a camera's pose filter accepted and rejected, and how spread out the accepted poses
 * are.
 *
 * <p>The spread is how to find a camera's standard deviations: hold the robot still where it sees
 * tags, {@link #reset()}, wait a few seconds, then read {@link #getStdDevs()}. {@link
 * #getSuggestedBaseStdDevs(double)} turns that into the base standard deviations for {@link
 * VisionPoseFilter#withStdDevs}.
 */
public class PoseFilterStats {

  private long accepted;
  private long rejected;
  private final Map<String, Long> rejectedByReason = new LinkedHashMap<>();

  private long samples;
  private double meanX;
  private double meanY;
  private double meanTheta;
  private double sumSqX;
  private double sumSqY;
  private double sumSqTheta;
  private double firstTheta;
  private double meanDistance;

  /** Creates empty stats. */
  public PoseFilterStats() {}

  /**
   * Counts an accepted estimate and adds its pose to the spread.
   *
   * @param estimate The accepted estimate.
   */
  public void recordAccepted(VisionPoseEstimate estimate) {
    accepted++;
    var pose = estimate.pose2d();
    double theta = pose.getRotation().getRadians();
    if (samples == 0) {
      firstTheta = theta;
    }
    // Measure heading around the first sample, so a spread across ±180° doesn't wrap.
    theta = MathUtil.angleModulus(theta - firstTheta);
    samples++;
    double dx = pose.getX() - meanX;
    meanX += dx / samples;
    sumSqX += dx * (pose.getX() - meanX);
    double dy = pose.getY() - meanY;
    meanY += dy / samples;
    sumSqY += dy * (pose.getY() - meanY);
    double dt = theta - meanTheta;
    meanTheta += dt / samples;
    sumSqTheta += dt * (theta - meanTheta);
    if (Double.isFinite(estimate.averageTagDistanceMeters())) {
      meanDistance += (estimate.averageTagDistanceMeters() - meanDistance) / samples;
    }
  }

  /**
   * Counts a rejected estimate.
   *
   * @param reason Why it was rejected: the name of the check that rejected it.
   */
  public void recordRejected(String reason) {
    rejected++;
    rejectedByReason.merge(reason, 1L, Long::sum);
  }

  /** Starts counting again. */
  public void reset() {
    accepted = 0;
    rejected = 0;
    rejectedByReason.clear();
    samples = 0;
    meanX = meanY = meanTheta = 0;
    sumSqX = sumSqY = sumSqTheta = 0;
    meanDistance = 0;
  }

  /**
   * How many estimates the filter accepted.
   *
   * @return The count.
   */
  public long getAcceptedCount() {
    return accepted;
  }

  /**
   * How many estimates the filter rejected.
   *
   * @return The count.
   */
  public long getRejectedCount() {
    return rejected;
  }

  /**
   * How many estimates one check rejected.
   *
   * @param reason The check's rejection reason, such as {@code "ambiguous"}, or a custom check's
   *     name.
   * @return The count.
   */
  public long getRejectedCount(String reason) {
    return rejectedByReason.getOrDefault(reason, 0L);
  }

  /**
   * How many estimates each check rejected.
   *
   * @return Counts by rejection reason, in the order the reasons first appeared.
   */
  public Map<String, Long> getRejectionCounts() {
    return Collections.unmodifiableMap(rejectedByReason);
  }

  /**
   * The share of estimates the filter accepted.
   *
   * @return 0 to 1, or {@link Double#NaN} before any estimate.
   */
  public double getAcceptanceRate() {
    long total = accepted + rejected;
    return total == 0 ? Double.NaN : (double) accepted / total;
  }

  /**
   * How many accepted poses the spread is measured from.
   *
   * @return The count.
   */
  public long getSampleCount() {
    return samples;
  }

  /**
   * The average accepted pose.
   *
   * @return The average, or empty before any accepted estimate.
   */
  public Optional<Pose2d> getMeanPose() {
    return samples == 0
        ? Optional.empty()
        : Optional.of(new Pose2d(meanX, meanY, Rotation2d.fromRadians(firstTheta + meanTheta)));
  }

  /**
   * How spread out the accepted poses are: the sample standard deviation of x (meters), y (meters)
   * and heading (radians). Measured with the robot still, this is the camera's real standard
   * deviation at the current distance.
   *
   * @return The standard deviations, or {@link Double#NaN}s with fewer than two samples.
   */
  public Matrix<N3, N1> getStdDevs() {
    if (samples < 2) {
      return VecBuilder.fill(Double.NaN, Double.NaN, Double.NaN);
    }
    return VecBuilder.fill(
        Math.sqrt(sumSqX / (samples - 1)),
        Math.sqrt(sumSqY / (samples - 1)),
        Math.sqrt(sumSqTheta / (samples - 1)));
  }

  /**
   * The average tag distance of the accepted poses.
   *
   * @return Meters, or {@link Double#NaN} before any accepted estimate.
   */
  public double getAverageTagDistanceMeters() {
    return samples == 0 ? Double.NaN : meanDistance;
  }

  /**
   * The base standard deviations that would give the measured spread at the measured tag distance,
   * for {@link VisionPoseFilter#withStdDevs}: {@code measured / (1 + distance² / distanceDivisor)}.
   *
   * @param distanceDivisor The filter's distance divisor (30 by default).
   * @return The base standard deviations, or {@link Double#NaN}s with fewer than two samples.
   */
  public Matrix<N3, N1> getSuggestedBaseStdDevs(double distanceDivisor) {
    double d = getAverageTagDistanceMeters();
    double scale = 1 + (Double.isFinite(d) ? d * d / distanceDivisor : 0);
    return getStdDevs().div(scale);
  }
}
