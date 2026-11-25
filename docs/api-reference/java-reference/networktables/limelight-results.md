# LimelightResults

**Package:** `limelight.networktables`

Java model of the full `limelight.results` JSON payload — every field the Limelight can publish for a single frame, across every pipeline type. Retrieved via `limelight.getLatestResults()` or `limelight.getData().getResults()`, both of which return `Optional<LimelightResults>` (empty if the LL GUI's "Send JSON over NT?" option is off).

{% hint style="warning" %}
Parsing this object deserializes a full JSON payload with Jackson every call. For a tight periodic loop where you only need tag/detection geometry, prefer [`LimelightData.getRawFiducials()`](limelight-data.md) / `getRawDetections()`, or the direct NT reads on [`LimelightTargetData`](limelight-target-data.md).
{% endhint %}

***

## Key Fields

| Field                                                                                          | Type                    | Description                                                                                           |
| ---------------------------------------------------------------------------------------------- | ----------------------- | ----------------------------------------------------------------------------------------------------- |
| `valid`                                                                                        | `boolean`               | `true` if any valid target is present (`v`).                                                          |
| `error`                                                                                        | `String`                | Error message, if any.                                                                                |
| `tx`, `ty`                                                                                     | `double`                | Crosshair-relative horizontal/vertical offset in degrees.                                             |
| `txnc`, `tync`                                                                                 | `double`                | Principal-pixel-relative horizontal/vertical offset in degrees.                                       |
| `ta`                                                                                           | `double`                | Undistorted, normalized target area (0-100).                                                          |
| `pipelineID`, `pipeline_type`                                                                  | `double`, `String`      | Active pipeline index and type string.                                                                |
| `latency_pipeline`, `latency_capture`                                                          | `double`                | Tracking loop latency and capture latency, in milliseconds.                                           |
| `timestamp_LIMELIGHT_publish`, `timestamp_RIOFPGA_capture`, `timestamp_NT`, `timestamp_System` | `double`                | Timestamps from various clocks — Limelight boot, RIO FPGA, NT server, and system wall clock.          |
| `botpose`, `botpose_wpired`, `botpose_wpiblue`                                                 | `double[6]`             | MegaTag1 pose in raw/red/blue coordinate frames: `[x, y, z, roll, pitch, yaw]`.                       |
| `botpose_mt2`, `botpose_mt2_blue`                                                              | `double[6]`             | MegaTag2 pose, raw and blue-origin.                                                                   |
| `botpose_tagcount`, `botpose_span`, `botpose_avgdist`, `botpose_avgarea`                       | `double`                | Aggregate stats over the tags used to compute `botpose`.                                              |
| `stdev_mt1`, `stdev_mt2`                                                                       | `double[6]`             | Standard deviations for MegaTag1/MegaTag2 pose components.                                            |
| `camerapose_robotspace`                                                                        | `double[6]`             | Camera pose relative to the robot.                                                                    |
| `targets_Retro`                                                                                | `RetroreflectiveTape[]` | Retroreflective pipeline results. See [RetroreflectiveTape](../target-types/retroreflective-tape.md). |
| `targets_Fiducials`                                                                            | `AprilTagFiducial[]`    | AprilTag pipeline results. See [AprilTagFiducial](../target-types/apriltag-fiducial.md).              |
| `targets_Classifier`                                                                           | `NeuralClassifier[]`    | Neural classifier results. See [NeuralClassifier](../target-types/neural-classifier.md).              |
| `targets_Detector`                                                                             | `NeuralDetector[]`      | Neural detector results. See [NeuralDetector](../target-types/neural-detector.md).                    |
| `targets_Barcode`                                                                              | `Barcode[]`             | Barcode pipeline results. See [Barcode](../target-types/barcode.md).                                  |
| `hardware`                                                                                     | `HardwareReport`        | System hardware telemetry (2026+). See [HardwareReport](../results-and-telemetry/hardware-report.md). |
| `imuResults`                                                                                   | `IMUResults`            | IMU orientation/acceleration data. See [IMUResults](../results-and-telemetry/imu-results.md).         |
| `rewindStats`                                                                                  | `RewindStats`           | Rewind buffer stats (LL4 only). See [RewindStats](../results-and-telemetry/rewind-stats.md).          |
| `targetDistance`                                                                               | `double`                | 3D distance from camera to target/POI, in meters (`tdist`).                                           |
| `PythonOut`                                                                                    | `double[8]`             | Output from a custom Python SnapScript pipeline.                                                      |

***

## Methods

| Method                                          | Returns  | Description                                                  |
| ----------------------------------------------- | -------- | ------------------------------------------------------------ |
| `getBotPose3d()`                                | `Pose3d` | MegaTag1 `botpose` as a `Pose3d`.                            |
| `getBotPose3d(DriverStation.Alliance alliance)` | `Pose3d` | MegaTag1 pose in the given alliance's coordinate frame.      |
| `getBotPose2d()`                                | `Pose2d` | MegaTag1 `botpose` as a `Pose2d`.                            |
| `getBotPose2d(DriverStation.Alliance alliance)` | `Pose2d` | MegaTag1 pose (2D) in the given alliance's coordinate frame. |
| `getMT2Pose3d()`                                | `Pose3d` | MegaTag2 `botpose_orb` as a `Pose3d`.                        |

{% hint style="info" %}
Prefer [`LimelightPoseEstimator`](limelight-pose-estimator.md) / [`PoseEstimate`](pose-estimate.md) for pose consumption in a periodic loop — they read the NT double-array entries directly (cheaper than JSON parsing) and include per-tag ambiguity data for filtering.
{% endhint %}

## See Also

* [LimelightData](limelight-data.md)
* [PoseEstimate](pose-estimate.md)
