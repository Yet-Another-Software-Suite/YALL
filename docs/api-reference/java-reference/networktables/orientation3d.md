# Orientation3d

**Package:** `limelight.networktables`

Bundles a robot's 3D rotation with its angular velocity, in the shape MegaTag2 expects via [`LimelightSettings.withRobotOrientation`](limelight-settings.md).

***

## Constructors

```java
Orientation3d(Rotation3d orientation, AngularVelocity yaw, AngularVelocity pitch, AngularVelocity roll)
Orientation3d(Rotation3d orientation, AngularVelocity3d angularVelocity)
```

***

## Fields

| Field             | Type                | Description                     |
| ----------------- | ------------------- | ------------------------------- |
| `orientation`     | `Rotation3d`        | Current robot rotation.         |
| `angularVelocity` | `AngularVelocity3d` | Current robot angular velocity. |

## Usage

```java
limelight.getSettings()
         .withRobotOrientation(new Orientation3d(
             gyro.getRotation3d(),
             new AngularVelocity3d(
                 DegreesPerSecond.of(gyro.getPitchVelocity()),
                 DegreesPerSecond.of(gyro.getRollVelocity()),
                 DegreesPerSecond.of(gyro.getYawVelocity()))))
         .save();
```

## See Also

* [AngularVelocity3d](angular-velocity3d.md)
* [LimelightSettings#withRobotOrientation](limelight-settings.md)
