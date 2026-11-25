# Hardware Telemetry & IMU

Newer Limelight firmware (2026+) publishes system health and, on the Limelight 4, onboard IMU data as part of the JSON results frame. All of it lives under `LimelightResults` and is only available through [`getLatestResults()`](results-json-vs-raw-networktables.md), since there's no raw-NT equivalent.

## HardwareReport

`LimelightResults.hardware` reports CPU usage, disk space, RAM usage, and CPU temperature:

```java
limelight.getLatestResults().ifPresent(result -> {
    if (result.hardware != null && result.hardware.getTemp().gt(Celsius.of(70))) {
        // consider throttling, see How do I reduce thermal output while disabled?
    }
});
```

If a Hailo neural accelerator module is installed (Limelight 4 only, sold separately), its stats are nested at `result.hardware.hailoStats` as a `HailoStats`: presence, temperature, power draw, and throttle state.

## IMUResults (Limelight 4)

`LimelightResults.imuResults` exposes the onboard IMU's fused yaw and full 9-axis data (orientation + angular velocity + acceleration):

```java
limelight.getLatestResults().ifPresent(result -> {
    if (result.imuResults != null) {
        Pair<Orientation3d, AngularAcceleration3d> imu = result.imuResults.getIMUData();
        Angle fusedYaw = result.imuResults.getFusedYaw();
    }
});
```

This is a separate concept from `LimelightSettings.withRobotOrientation`. That method **submits** your robot's gyro data _to_ the Limelight for MegaTag2 fusion, while `IMUResults` **reads back** the Limelight 4's own internal IMU, which is useful for diagnostics or as a fallback orientation source, not as a replacement for your robot's primary gyro. See `LimelightSettings.ImuMode` for how the two interact when both an internal and external IMU are available.

## RewindStats (Limelight 4)

`LimelightResults.rewindStats` reports rolling-buffer status: usage fraction, frame count, and whether a flush to disk is in progress. See [How do I take snapshots and rewind captures?](../how-to-guides/how-do-i-take-snapshots-and-rewind-captures.md) for triggering captures.

## See Also

* [How do I reduce thermal output while disabled?](../how-to-guides/how-do-i-reduce-thermal-output-while-disabled.md)
* HardwareReport reference
* IMUResults reference
