# AngularAcceleration3d

**Package:** `limelight.networktables`

Immutable 3-axis angular acceleration struct, following the same `x`/`roll`, `y`/`pitch`, `z`/`yaw` aliasing as [`AngularVelocity3d`](angular-velocity3d.md). Produced by [`IMUResults.getIMUData()`](../results-and-telemetry/imu-results.md).

***

## Constructor

```java
AngularAcceleration3d(AngularAcceleration x, AngularAcceleration y, AngularAcceleration z)
```

***

## Fields

| Field        | Type                  | Description                            |
| ------------ | --------------------- | -------------------------------------- |
| `x`, `roll`  | `AngularAcceleration` | Angular acceleration about the X-axis. |
| `y`, `pitch` | `AngularAcceleration` | Angular acceleration about the Y-axis. |
| `z`, `yaw`   | `AngularAcceleration` | Angular acceleration about the Z-axis. |

## See Also

* [AngularVelocity3d](angular-velocity3d.md)
* [IMUResults](../results-and-telemetry/imu-results.md)
