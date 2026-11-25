---
icon: volume-high
---

# TelemetryVerbosity

In short: how much a camera sends to the dashboard.

**Package:** `yall.telemetry`

```java
public enum TelemetryVerbosity
```

How much a camera publishes to NetworkTables. Each level includes the ones below it. Set it with [`VisionTelemetryConfig.withVerbosity`](vision-telemetry-config.md).

| Value | Publishes |
| --- | --- |
| `LOW` | Connection, targets in view and accepted poses: what a driver needs. |
| `MID` | Also rejected poses, why they were rejected and latency. The default. |
| `HIGH` | Also the pose filter stats and every target's ID, yaw, pitch and area. |

[`VisionTelemetryField`](vision-telemetry-field.md) lists which fields each level turns on.

***

## Read more

* [VisionTelemetryField](vision-telemetry-field.md)
* [VisionTelemetryConfig](vision-telemetry-config.md)
