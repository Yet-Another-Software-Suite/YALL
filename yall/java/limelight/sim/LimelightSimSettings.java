package limelight.sim;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;

/**
 * Configurable properties describing a simulated {@link limelight.Limelight} camera, used by {@link LimelightSim}.
 * <p>
 * The defaults approximate a Limelight 3 running the AprilTag pipeline at 1280x960 and require no configuration to
 * get a usable simulation running. Construct one with {@code new LimelightSimSettings()} and tweak only the fields
 * that matter for your camera with the {@code withXXX} methods.
 */
public class LimelightSimSettings
{

  /**
   * Horizontal sensor resolution in pixels.
   */
  double   resolutionWidth              = 1280;
  /**
   * Vertical sensor resolution in pixels.
   */
  double   resolutionHeight             = 960;
  /**
   * Horizontal field of view of the camera.
   */
  Rotation2d horizontalFOV              = Rotation2d.fromDegrees(63.3);
  /**
   * Vertical field of view of the camera.
   */
  Rotation2d verticalFOV                = Rotation2d.fromDegrees(49.7);
  /**
   * Physical size of the AprilTag (black square, edge-to-edge) in meters. Defaults to the 6.5in tags used in modern
   * FRC games.
   */
  double   tagSizeMeters                = Units.inchesToMeters(6.5);
  /**
   * Maximum distance a tag may be detected from, in meters.
   */
  double   maxDetectionRangeMeters      = 6.0;
  /**
   * Maximum angle of incidence between the camera's line of sight and a tag's face before the tag is considered too
   * edge-on to reliably detect, in degrees.
   */
  double   maxTagIncidenceAngleDegrees  = 80;
  /**
   * Average pipeline processing latency ("tl"), in milliseconds.
   */
  double   avgPipelineLatencyMs         = 40;
  /**
   * Standard deviation of the pipeline processing latency, in milliseconds.
   */
  double   latencyStdDevMs              = 8;
  /**
   * Average capture latency ("cl"), in milliseconds.
   */
  double   avgCaptureLatencyMs          = 10;
  /**
   * Standard deviation applied to tx/ty readings, in degrees.
   */
  double   angleNoiseStdDevDegrees      = 0.15;
  /**
   * Baseline standard deviation applied to the translation component of botpose readings, in meters, for a single tag
   * seen at 1 meter. Scaled down as more tags are seen and up with average tag distance.
   */
  double   translationNoiseStdDevMeters = 0.03;
  /**
   * Baseline standard deviation applied to the rotation component of botpose readings, in degrees, for a single tag.
   * Scaled down as more tags are seen.
   */
  double   rotationNoiseStdDevDegrees   = 1.5;
  /**
   * Seed used for the deterministic noise {@link java.util.Random} source.
   */
  long     randomSeed                   = 4152026L;
  /**
   * Pipeline type string reported by "getpipetype".
   */
  String   pipelineType                 = "pipe_fiducial";

  /**
   * Construct {@link LimelightSimSettings} with defaults approximating a Limelight 3.
   */
  public LimelightSimSettings()
  {
  }

  /**
   * Set the simulated sensor resolution.
   *
   * @param widthPixels  Horizontal resolution in pixels.
   * @param heightPixels Vertical resolution in pixels.
   * @return {@link LimelightSimSettings} for chaining.
   */
  public LimelightSimSettings withResolution(double widthPixels, double heightPixels)
  {
    resolutionWidth = widthPixels;
    resolutionHeight = heightPixels;
    return this;
  }

  /**
   * Set the simulated camera's field of view.
   *
   * @param horizontalDegrees Horizontal field of view in degrees.
   * @param verticalDegrees   Vertical field of view in degrees.
   * @return {@link LimelightSimSettings} for chaining.
   */
  public LimelightSimSettings withFOV(double horizontalDegrees, double verticalDegrees)
  {
    horizontalFOV = Rotation2d.fromDegrees(horizontalDegrees);
    verticalFOV = Rotation2d.fromDegrees(verticalDegrees);
    return this;
  }

  /**
   * Set the physical size of the AprilTags being detected.
   *
   * @param sizeMeters Edge-to-edge size of the tag's black square, in meters.
   * @return {@link LimelightSimSettings} for chaining.
   */
  public LimelightSimSettings withTagSize(double sizeMeters)
  {
    tagSizeMeters = sizeMeters;
    return this;
  }

  /**
   * Set the maximum distance a tag may be detected from.
   *
   * @param rangeMeters Maximum detection range, in meters.
   * @return {@link LimelightSimSettings} for chaining.
   */
  public LimelightSimSettings withMaxDetectionRange(double rangeMeters)
  {
    maxDetectionRangeMeters = rangeMeters;
    return this;
  }

  /**
   * Set the maximum angle of incidence a tag may be viewed at before it is considered too edge-on to detect.
   *
   * @param degrees Maximum incidence angle, in degrees.
   * @return {@link LimelightSimSettings} for chaining.
   */
  public LimelightSimSettings withMaxTagIncidenceAngle(double degrees)
  {
    maxTagIncidenceAngleDegrees = degrees;
    return this;
  }

  /**
   * Set the simulated pipeline processing latency ("tl").
   *
   * @param avgMs    Average latency, in milliseconds.
   * @param stdDevMs Standard deviation of the latency, in milliseconds.
   * @return {@link LimelightSimSettings} for chaining.
   */
  public LimelightSimSettings withPipelineLatency(double avgMs, double stdDevMs)
  {
    avgPipelineLatencyMs = avgMs;
    latencyStdDevMs = stdDevMs;
    return this;
  }

  /**
   * Set the simulated capture latency ("cl").
   *
   * @param avgMs Average capture latency, in milliseconds.
   * @return {@link LimelightSimSettings} for chaining.
   */
  public LimelightSimSettings withCaptureLatency(double avgMs)
  {
    avgCaptureLatencyMs = avgMs;
    return this;
  }

  /**
   * Set the noise applied to tx/ty/txnc/tync readings.
   *
   * @param stdDevDegrees Standard deviation of the angular noise, in degrees.
   * @return {@link LimelightSimSettings} for chaining.
   */
  public LimelightSimSettings withAngleNoise(double stdDevDegrees)
  {
    angleNoiseStdDevDegrees = stdDevDegrees;
    return this;
  }

  /**
   * Set the noise applied to botpose translation and rotation readings, for a single tag seen at 1 meter.
   *
   * @param translationStdDevMeters Standard deviation of the translation noise, in meters.
   * @param rotationStdDevDegrees   Standard deviation of the rotation noise, in degrees.
   * @return {@link LimelightSimSettings} for chaining.
   */
  public LimelightSimSettings withPoseNoise(double translationStdDevMeters, double rotationStdDevDegrees)
  {
    translationNoiseStdDevMeters = translationStdDevMeters;
    rotationNoiseStdDevDegrees = rotationStdDevDegrees;
    return this;
  }

  /**
   * Set the seed used for the deterministic noise source.
   *
   * @param seed Random seed.
   * @return {@link LimelightSimSettings} for chaining.
   */
  public LimelightSimSettings withRandomSeed(long seed)
  {
    randomSeed = seed;
    return this;
  }

  /**
   * Set the pipeline type string reported by "getpipetype".
   *
   * @param type Pipeline type, e.g. "pipe_fiducial".
   * @return {@link LimelightSimSettings} for chaining.
   */
  public LimelightSimSettings withPipelineType(String type)
  {
    pipelineType = type;
    return this;
  }

  /**
   * Create {@link LimelightSimSettings} with all noise and latency zeroed out, useful for deterministic testing.
   *
   * @return {@link LimelightSimSettings} with no noise or latency.
   */
  public static LimelightSimSettings perfect()
  {
    return new LimelightSimSettings().withPipelineLatency(0, 0)
                                     .withCaptureLatency(0)
                                     .withAngleNoise(0)
                                     .withPoseNoise(0, 0);
  }

}
