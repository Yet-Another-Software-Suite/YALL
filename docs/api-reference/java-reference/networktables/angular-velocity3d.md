# AngularVelocity3d

**Package:** `limelight.networktables`

Immutable 3-axis angular velocity struct. Each axis is exposed under both its Cartesian name (`x`/`y`/`z`) and its rotational name (`roll`/`pitch`/`yaw`) — they're the same field.

***

## Constructor

```java
AngularVelocity3d(AngularVelocity x, AngularVelocity y, AngularVelocity z)
```

`x`/`roll`, `y`/`pitch`, `z`/`yaw`.

***

## Fields

| Field        | Type              | Description                        |
| ------------ | ----------------- | ---------------------------------- |
| `x`, `roll`  | `AngularVelocity` | Angular velocity about the X-axis. |
| `y`, `pitch` | `AngularVelocity` | Angular velocity about the Y-axis. |
| `z`, `yaw`   | `AngularVelocity` | Angular velocity about the Z-axis. |

## See Also

* [Orientation3d](orientation3d.md)
* [AngularAcceleration3d](angular-acceleration3d.md)
