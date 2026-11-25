package yall.telemetry;

/** One group of values a camera can publish, and the lowest verbosity that publishes it. */
public enum VisionTelemetryField {
  /** {@code connected}: whether the camera is sending data. */
  CONNECTED(TelemetryVerbosity.LOW),
  /** {@code hasTargets}: whether the newest frame has targets. */
  HAS_TARGETS(TelemetryVerbosity.LOW),
  /** {@code targetCount}: how many targets the newest frame has. */
  TARGET_COUNT(TelemetryVerbosity.LOW),
  /** {@code acceptedPoses}: the pose estimates the filter accepted in the last update. */
  ACCEPTED_POSES(TelemetryVerbosity.LOW),
  /** {@code rejectedPoses}: the pose estimates the filter rejected in the last update. */
  REJECTED_POSES(TelemetryVerbosity.MID),
  /** {@code rejectionReason}: why the filter last rejected an estimate. */
  REJECTION_REASON(TelemetryVerbosity.MID),
  /** {@code latencyMillis}: the newest frame's latency. */
  LATENCY(TelemetryVerbosity.MID),
  /**
   * {@code stats/...}: pose filter counts (accepted, rejected, rejected by each check, acceptance
   * rate) and the spread of the accepted poses (mean, standard deviations, sample count). Set {@code
   * stats/reset} to true to start counting again.
   */
  FILTER_STATS(TelemetryVerbosity.MID),
  /** {@code targets/...}: every target's ID, yaw, pitch and area. */
  TARGETS(TelemetryVerbosity.HIGH);

  private final TelemetryVerbosity verbosity;

  VisionTelemetryField(TelemetryVerbosity verbosity) {
    this.verbosity = verbosity;
  }

  /**
   * The lowest verbosity that publishes this field.
   *
   * @return The verbosity.
   */
  public TelemetryVerbosity getVerbosity() {
    return verbosity;
  }
}
