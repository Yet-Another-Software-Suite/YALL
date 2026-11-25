# How do I reduce thermal output while disabled?

The Limelight processes frames continuously by default, which generates heat even when the robot isn't moving. Throttle frame processing while disabled to cut thermal output:

```java
@Override
public void disabledInit() {
    limelight.getSettings().withThrottle(150).save();
}

@Override
public void autonomousInit() {
    limelight.getSettings().withThrottle(0).save();
}

@Override
public void teleopInit() {
    limelight.getSettings().withThrottle(0).save();
}
```

`withThrottle(n)` skips `n` frames between processed frames. The library's recommendation is **100-200** while disabled; outputs are not zeroed during skipped frames, so this is safe to toggle without side effects on stale readings. Set it back to `0` before you need fresh vision data.

## See Also

* LimelightSettings reference
* [Hardware Telemetry & IMU](../understanding/hardware-telemetry-and-imu.md): reading CPU temperature to confirm the throttle helped.
