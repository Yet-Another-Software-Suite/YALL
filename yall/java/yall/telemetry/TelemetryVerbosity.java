package yall.telemetry;

/** How much a camera publishes to NetworkTables. Each level includes the ones below it. */
public enum TelemetryVerbosity {
  /** Connection, targets in view and accepted poses: what a driver needs. */
  LOW,
  /** Also rejected poses, why they were rejected and latency. */
  MID,
  /** Also the pose filter stats and every target's ID, yaw, pitch and area. */
  HIGH
}
