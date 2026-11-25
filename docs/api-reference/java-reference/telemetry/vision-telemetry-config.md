---
icon: tower-broadcast
---

# VisionTelemetryConfig

In short: what a camera sends to the dashboard, and where.

**Package:** `yall.telemetry`

```java
public class VisionTelemetryConfig
```

What a camera publishes to NetworkTables, and where. Pass it to [`VisionCameraConfig.withTelemetry`](../yall/vision-camera-config.md). By default a camera publishes every [`MID`](telemetry-verbosity.md) field under `YALL/<camera name>`.

```java
new VisionCameraConfig()
    .withTelemetry(new VisionTelemetryConfig()
        .withVerbosity(TelemetryVerbosity.LOW)
        .withField(VisionTelemetryField.FILTER_STATS, true)
        .withTablePath("Vision/front"));
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
| `getVerbosity()` | [`TelemetryVerbosity`](telemetry-verbosity.md) | How much to publish. |
| `isEnabled(VisionTelemetryField field)` | `boolean` | Whether a field is published: true if it is turned on, or if its verbosity is at or below the config's and it isn't turned off. |
| `getTablePath(String cameraName)` | `String` | The table to publish under, for a camera. |

To publish nothing, use [`VisionCameraConfig.withTelemetry(false)`](../yall/vision-camera-config.md).

***

## Read more

* [VisionTelemetryField](vision-telemetry-field.md)
* [PoseFilterStats](../filter/pose-filter-stats.md)
