# IMUResults

**Package:** `limelight.results`

Onboard IMU data from the Limelight 4, populated from the JSON `imu` object in [`LimelightResults`](../networktables/limelight-results.md).

***

## Fields

| Field        | Type        | Description                                                                                                                       |
| ------------ | ----------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `data`       | `double[9]` | Raw IMU array: `[roll, pitch, yaw, roll_rate, pitch_rate, yaw_rate, accel_x, accel_y, accel_z]` (degrees, degrees/s, degrees/s²). |
| `quaternion` | `double[4]` | Orientation quaternion `[w, x, y, z]`.                                                                                            |
| `yaw`        | `double`    | Final fused yaw, in degrees.                                                                                                      |

***

## Methods

| Method          | Returns                                      | Description                                                                                                                                                                                        |
| --------------- | -------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Quaternion()`  | `Rotation3d`                                 | `quaternion` decoded as a `Rotation3d`.                                                                                                                                                            |
| `getIMUData()`  | `Pair<Orientation3d, AngularAcceleration3d>` | `data` decoded into an [`Orientation3d`](../networktables/orientation3d.md) (orientation + angular velocity) paired with an [`AngularAcceleration3d`](../networktables/angular-acceleration3d.md). |
| `getFusedYaw()` | `Angle`                                      | `yaw` as a WPILib `Angle`.                                                                                                                                                                         |

## See Also

* [Orientation3d](../networktables/orientation3d.md)
* [LimelightSettings.ImuMode](../networktables/limelight-settings.md)
