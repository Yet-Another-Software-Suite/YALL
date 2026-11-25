---
icon: chart-line
---

# Telemetry

**Telemetry** is data the robot program sends out while it runs, so you can watch it on a dashboard. YALL sends what each camera sees and what the pose filter decided.

## What YALL publishes

Each camera publishes to NetworkTables under `YALL/<camera name>`, updated by every `update()`:

| Topic | Type | Meaning |
| --- | --- | --- |
| `acceptedPoses` | `Pose3d[]` | Estimates from the last `update()` that passed the pose filter |
| `rejectedPoses` | `Pose3d[]` | Estimates from the last `update()` the filter rejected |
| `rejectionReason` | string | Why the filter last rejected an estimate, for example `tags too far` |
| `hasTargets` | boolean | Whether the newest frame has targets |
| `targetCount` | integer | Targets in the newest frame |
| `latencyMillis` | double | The newest frame's latency |
| `connected` | boolean | Whether the camera is sending data |

[`VisionSystem`](https://yall.yassrobotics.com/api-reference/java-reference/yall/vision-system) publishes `YALL/fusedPose` (`Pose3d`) when it has one.

Turn a camera's telemetry off with `config.withTelemetry(false)`.

## Seeing the filter work

Add `acceptedPoses` and `rejectedPoses` to a field view in AdvantageScope or Elastic, next to your pose estimator's pose. Accepted poses should sit on the robot. Rejected poses that also sit on the robot mean a check is too strict; check `rejectionReason` for which one.

## In simulation

To see the simulated camera's picture, turn on its video stream: see [Simulating Cameras](../tutorials/simulating-cameras.md#see-what-the-camera-sees).


PhotonVision publishes a debug field for each simulated world, `VisionSystemSim-YALL/Sim Field` for the default one, with the robot, the cameras and the targets they see. [`VisionSim.getDefault().getDebugField()`](https://yall.yassrobotics.com/api-reference/java-reference/sim/vision-sim) returns the same `Field2d`.

## Vendor telemetry

Vendor libraries publish their own telemetry too:

* **Limelight**: LimelightLib 2 publishes under `limelight_telemetry/<name>` (turn it off with `limelight.withTelemetry(false)`). Hardware and IMU readings are on the `Limelight` object: `getHardwareData()` (`cpuTempCelsius`, `cpuUsagePercent`, `ramUsagePercent`, ...) and `getIMUData()` (`robotYawDegrees`, `rollDegrees`, `pitchDegrees`, `gyroZDegreesPerSecond`, ...).
* **PhotonVision**: under `photonvision/<camera name>`.
* **Synapse**: under `Synapse/<camera name>`.

## Read more

* [How do I tune the pose filter?](../how-to-guides/how-do-i-tune-the-pose-filter.md)
* [How do I use vendor-specific settings?](../how-to-guides/how-do-i-use-vendor-specific-settings.md)
