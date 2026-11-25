package yall.telemetry;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * What a camera publishes to NetworkTables, and where.
 *
 * <pre>{@code
 * new VisionCameraConfig()
 *     .withTelemetry(new VisionTelemetryConfig()
 *         .withVerbosity(TelemetryVerbosity.LOW)
 *         .withField(VisionTelemetryField.FILTER_STATS, true));
 * }</pre>
 *
 * <p>By default a camera publishes every {@link TelemetryVerbosity#MID} field under {@code
 * YALL/<camera name>}.
 */
public class VisionTelemetryConfig {

  private TelemetryVerbosity verbosity = TelemetryVerbosity.MID;
  private final Map<VisionTelemetryField, Boolean> overrides =
      new EnumMap<>(VisionTelemetryField.class);
  private Optional<String> tablePath = Optional.empty();

  /** Creates a config that publishes every {@link TelemetryVerbosity#MID} field. */
  public VisionTelemetryConfig() {}

  /**
   * Sets how much to publish. Fields turned on or off with {@link #withField} keep their setting.
   *
   * @param verbosity The verbosity.
   * @return This config, for chaining.
   */
  public VisionTelemetryConfig withVerbosity(TelemetryVerbosity verbosity) {
    this.verbosity = verbosity;
    return this;
  }

  /**
   * Turns one field on or off, whatever the verbosity.
   *
   * @param field The field.
   * @param enabled true to publish it.
   * @return This config, for chaining.
   */
  public VisionTelemetryConfig withField(VisionTelemetryField field, boolean enabled) {
    overrides.put(field, enabled);
    return this;
  }

  /**
   * Sets the NetworkTables table to publish under, such as {@code "Vision/front"}. Defaults to
   * {@code YALL/<camera name>}.
   *
   * @param path Table path, with {@code /} between tables.
   * @return This config, for chaining.
   */
  public VisionTelemetryConfig withTablePath(String path) {
    tablePath = Optional.of(path);
    return this;
  }

  /**
   * How much to publish.
   *
   * @return The verbosity.
   */
  public TelemetryVerbosity getVerbosity() {
    return verbosity;
  }

  /**
   * Whether a field is published.
   *
   * @param field The field.
   * @return true if it is turned on, or is at or below the verbosity and not turned off.
   */
  public boolean isEnabled(VisionTelemetryField field) {
    Boolean override = overrides.get(field);
    return override != null ? override : field.getVerbosity().ordinal() <= verbosity.ordinal();
  }

  /**
   * The table to publish under.
   *
   * @param cameraName The camera's name, for the default path.
   * @return The table path.
   */
  public String getTablePath(String cameraName) {
    return tablePath.orElse("YALL/" + cameraName);
  }
}
