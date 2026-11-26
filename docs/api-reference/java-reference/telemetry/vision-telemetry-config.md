---
icon: tower-broadcast
---

# VisionTelemetryConfig

In short: what a camera sends to the dashboard and the log, and where.

**Package:** `yall.telemetry`

```java
public class VisionTelemetryConfig
```

What a camera publishes to NetworkTables and logs to a WPILib DataLog, and where. Pass it to [`VisionCameraConfig.withTelemetry`](../yall/vision-camera-config.md). By default a camera publishes every [`MID`](telemetry-verbosity.md) field to NetworkTables under `YALL/<camera name>`, and logs nothing.

```java
new VisionCameraConfig()
    .withTelemetry(new VisionTelemetryConfig()
        .withVerbosity(TelemetryVerbosity.LOW)
        .withField(VisionTelemetryField.FILTER_STATS, true)
        .withTablePath("Vision/front")
        .withDataLog());
```

***

## Constructor

`VisionTelemetryConfig()` makes a config that publishes every `MID` field under `YALL/<camera name>`.

***

## Methods

| Method | Returns | Description |
| --- | --- | --- |
| `withVerbosity(TelemetryVerbosity verbosity)` | `VisionTelemetryConfig` | Sets how much to publish. Fields turned on or off with `withField` keep their setting. |
| `withField(VisionTelemetryField field, boolean enabled)` | `VisionTelemetryConfig` | Turns one [field](vision-telemetry-field.md) on or off, whatever the verbosity. |
| `withTablePath(String path)` | `VisionTelemetryConfig` | Sets the table to publish under, such as `"Vision/front"`, with `/` between tables. Defaults to `YALL/<camera name>`. |
| `withNetworkTables(boolean enabled)` | `VisionTelemetryConfig` | Sets whether to publish to NetworkTables. Defaults to true. With it off, the fields only go to the DataLog, if `withDataLog` is on, and `stats/reset` isn't available. |
| `withDataLog()` | `VisionTelemetryConfig` | Logs the enabled fields to `DataLogManager`'s log, under the table path. Each value is logged when it changes. |
| `withDataLog(String path)` | `VisionTelemetryConfig` | Logs the enabled fields to `DataLogManager`'s log under a path of its own, such as `"Vision/front"`. |
| `getVerbosity()` | [`TelemetryVerbosity`](telemetry-verbosity.md) | How much to publish. |
| `isEnabled(VisionTelemetryField field)` | `boolean` | Whether a field is published: true if it is turned on, or if its verbosity is at or below the config's and it isn't turned off. |
| `getTablePath(String cameraName)` | `String` | The table to publish under, for a camera. |
| `isNetworkTablesEnabled()` | `boolean` | Whether to publish to NetworkTables. |
| `getDataLogPath(String cameraName)` | `Optional<String>` | The path to log under, for a camera, or empty if not logging to a DataLog. |

`DataLogManager` also logs NetworkTables by default, under `NT:/`. To log each value once, turn one of them off: `withNetworkTables(false)` here, or `DataLogManager.logNetworkTables(false)`.

To publish nothing, use [`VisionCameraConfig.withTelemetry(false)`](../yall/vision-camera-config.md).

***

## Read more

* [VisionTelemetryField](vision-telemetry-field.md)
* [PoseFilterStats](../filter/pose-filter-stats.md)
