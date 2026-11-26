package yall.telemetry;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * What a camera publishes to NetworkTables and logs to a WPILib DataLog, and where.
 *
 * <pre>{@code
 * new VisionCameraConfig()
 *     .withTelemetry(new VisionTelemetryConfig()
 *         .withVerbosity(TelemetryVerbosity.LOW)
 *         .withField(VisionTelemetryField.FILTER_STATS, true)
 *         .withDataLog());
 * }</pre>
 *
 * <p>By default a camera publishes every {@link TelemetryVerbosity#MID} field to NetworkTables
 * under {@code YALL/<camera name>}, and logs nothing to a DataLog. {@link #withDataLog()} logs the
 * same fields to {@link org.wpilib.system.DataLogManager}'s log, and {@link
 * #withNetworkTables(boolean)} turns NetworkTables off, such as at competition.
 */
public class VisionTelemetryConfig {

  private TelemetryVerbosity verbosity = TelemetryVerbosity.MID;
  private final Map<VisionTelemetryField, Boolean> overrides =
      new EnumMap<>(VisionTelemetryField.class);
  private Optional<String> tablePath = Optional.empty();
  private boolean networkTables = true;
  private boolean dataLog = false;
  private Optional<String> dataLogPath = Optional.empty();

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
   * Sets whether to publish to NetworkTables. Defaults to true. With it off, a camera's fields only
   * go to the DataLog, if {@link #withDataLog()} is on, and {@code stats/reset} isn't available.
   *
   * @param enabled true to publish to NetworkTables.
   * @return This config, for chaining.
   */
  public VisionTelemetryConfig withNetworkTables(boolean enabled) {
    networkTables = enabled;
    return this;
  }

  /**
   * Logs the enabled fields to {@link org.wpilib.system.DataLogManager}'s log, under the same path
   * as NetworkTables ({@code YALL/<camera name>} unless {@link #withTablePath} changes it). Each
   * value is logged when it changes.
   *
   * <p>{@code DataLogManager} also logs NetworkTables by default, under {@code NT:/}. To log each
   * value once, turn one of them off: {@link #withNetworkTables(boolean)} here, or {@code
   * DataLogManager.logNetworkTables(false)}.
   *
   * @return This config, for chaining.
   */
  public VisionTelemetryConfig withDataLog() {
    dataLog = true;
    return this;
  }

  /**
   * Logs the enabled fields to {@link org.wpilib.system.DataLogManager}'s log under a path of its
   * own, such as {@code "Vision/front"}. See {@link #withDataLog()}.
   *
   * @param path Log entry path, with {@code /} between names.
   * @return This config, for chaining.
   */
  public VisionTelemetryConfig withDataLog(String path) {
    dataLog = true;
    dataLogPath = Optional.of(path);
    return this;
  }

  /**
   * Whether to publish to NetworkTables.
   *
   * @return true to publish to NetworkTables.
   */
  public boolean isNetworkTablesEnabled() {
    return networkTables;
  }

  /**
   * The path to log under, if logging to a DataLog.
   *
   * @param cameraName The camera's name, for the default path.
   * @return The log path, or empty if not logging to a DataLog.
   */
  public Optional<String> getDataLogPath(String cameraName) {
    return dataLog ? Optional.of(dataLogPath.orElse(getTablePath(cameraName))) : Optional.empty();
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
