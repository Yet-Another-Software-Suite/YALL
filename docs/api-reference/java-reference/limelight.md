# Limelight

**Package:** `limelight`

`Limelight` is the entry point for the library — one instance per physical camera, bound to that camera's NetworkTables table (`/<name>`). Construction verifies the table exists (except in simulation) and creates the associated `LimelightData` and `LimelightSettings` objects.

***

## Constructor

```java
Limelight(String name)
```

| Parameter | Description                                                                 |
| --------- | --------------------------------------------------------------------------- |
| `name`    | NetworkTables name of the camera, e.g. `"limelight"` or `"limelight-front"` |

***

## Fields

| Field           | Type     | Description                     |
| --------------- | -------- | ------------------------------- |
| `limelightName` | `String` | Name passed to the constructor. |

***

## Methods

| Method                                        | Returns                      | Description                                                                                                                                                        |
| --------------------------------------------- | ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `getSettings()`                               | `LimelightSettings`          | Chainable object for `.withXXXX()` configuration calls. See [LimelightSettings](networktables/limelight-settings.md).                                              |
| `getData()`                                   | `LimelightData`              | Read-only NetworkTables data: raw fiducials, raw detections, barcodes, Python I/O. See [LimelightData](networktables/limelight-data.md).                           |
| `createPoseEstimator(EstimationMode)`         | `LimelightPoseEstimator`     | Creates a MegaTag1 or MegaTag2 pose estimator bound to this camera. See [LimelightPoseEstimator](networktables/limelight-pose-estimator.md).                       |
| `getLatestResults()`                          | `Optional<LimelightResults>` | Parses the latest `limelight.results` JSON blob. Empty if the Limelight isn't publishing JSON over NT. See [LimelightResults](networktables/limelight-results.md). |
| `getNTTable()`                                | `NetworkTable`               | The underlying `NetworkTable` for this camera.                                                                                                                     |
| `flush()`                                     | `void`                       | Flushes the default `NetworkTableInstance` immediately.                                                                                                            |
| `rewindCapture(double durationSeconds)`       | `void`                       | Triggers a rewind buffer capture (max 165s, rate-limited on the Limelight). Requires supported hardware.                                                           |
| `snapshot(String snapshotname)`               | `void`                       | Asynchronously requests an HTTP snapshot capture (`capturesnapshot`) named `snapshotname`.                                                                         |
| `static isAvailable(String limelightName)`    | `boolean`                    | Polls NetworkTables (up to 2s) for a `getpipe` key under the given table name. Raises a WPILib `Alert` and returns `false` if not found.                           |
| `static setupPortForwardingUSB(int usbIndex)` | `void`                       | Forwards ports `5800-5809 + usbIndex*10` to `172.29.<usbIndex>.1:5800-5809` for USB-connected LL3A/3G web UI access.                                               |

***

## Notes

* `isAvailable` is meant to run once, at robot construction — not in a periodic loop. Use `getData().targetData.getTargetStatus()` for per-iteration 2D target validity, or `PoseEstimate.hasData` for 3D pose validity.
* `snapshot()` runs the HTTP request on a separate thread via `CompletableFuture` so it never blocks the robot loop.

## See Also

* [LimelightSettings](networktables/limelight-settings.md)
* [LimelightPoseEstimator](networktables/limelight-pose-estimator.md)
* [LimelightData](networktables/limelight-data.md)
