---
icon: list-check
---

# VisionTelemetryField

In short: one group of values a camera can send, and the lowest verbosity that sends it.

**Package:** `yall.telemetry`

```java
public enum VisionTelemetryField
```

Turn single fields on or off with [`VisionTelemetryConfig.withField`](vision-telemetry-config.md). Keys are under the camera's table, `YALL/<camera name>` by default.

| Value | Verbosity | NetworkTables keys |
| --- | --- | --- |
| `CONNECTED` | `LOW` | `connected` (`boolean`): whether the camera is sending data. |
| `HAS_TARGETS` | `LOW` | `hasTargets` (`boolean`): whether the newest frame has targets. |
| `TARGET_COUNT` | `LOW` | `targetCount` (`int`): targets in the newest frame. |
| `ACCEPTED_POSES` | `LOW` | `acceptedPoses` (`Pose3d[]`): estimates the filter accepted in the last update. |
| `REJECTED_POSES` | `MID` | `rejectedPoses` (`Pose3d[]`): estimates the filter rejected in the last update. |
| `REJECTION_REASON` | `MID` | `rejectionReason` (`String`): why the filter last rejected an estimate. |
| `LATENCY` | `MID` | `latencyMillis` (`double`): the newest frame's latency. |
| `FILTER_STATS` | `MID` | `stats/...`: the [`PoseFilterStats`](../filter/pose-filter-stats.md#networktables). |
| `TARGETS` | `HIGH` | `targets/fiducialIds` (`long[]`), `targets/yawDegrees`, `targets/pitchDegrees`, `targets/areaPercent` (`double[]`): every target in the newest frame. |

| Method | Returns | Description |
| --- | --- | --- |
| `getVerbosity()` | [`TelemetryVerbosity`](telemetry-verbosity.md) | The lowest verbosity that publishes this field. |

***

## Read more

* [VisionTelemetryConfig](vision-telemetry-config.md)
* [TelemetryVerbosity](telemetry-verbosity.md)
