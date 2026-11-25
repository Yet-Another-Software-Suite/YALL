# How do I mount a camera on a moving mechanism?

Call `setRobotToCamera` every loop with the camera's current position. It updates everything that uses the offset: PhotonVision's pose estimator, the simulated camera, a Limelight's camera pose, and Synapse's robot pose.

## Example: camera on a turret

```java
// Measured once, with the turret at its zero angle.
private final Transform3d robotToTurretPivot =
    new Transform3d(Inches.of(-4), Inches.zero(), Inches.of(18), Rotation3d.ZERO);
private final Transform3d turretPivotToCamera =
    new Transform3d(Inches.of(6), Inches.zero(), Inches.of(2),
                    new Rotation3d(Degrees.zero(), Degrees.of(-20), Degrees.zero()));

@Override
public void periodic() {
  Transform3d turretRotation =
      new Transform3d(Translation3d.ZERO, new Rotation3d(Degrees.zero(), Degrees.zero(), turret.getAngle()));
  camera.setRobotToCamera(robotToTurretPivot.plus(turretRotation).plus(turretPivotToCamera));
  camera.update();
}
```

The transforms chain from the robot to the turret pivot, through the turret's current angle, to the camera.

For a camera that moves to a few fixed positions, the measured form works too:

```java
camera.setRobotToCamera(Inches.of(10), Inches.zero(), Inches.of(30),
                        Degrees.zero(), Degrees.of(-10), Degrees.zero());
```

## Checklist

* Measure the pivot and the camera at the mechanism's zero position.
* Set the offset every loop, before `update()`, not just once at startup.
* Images take time to process, so a fast-moving mechanism makes the newest offset slightly wrong for the frame being read. Tighten the pose filter, or ignore estimates while the mechanism is moving fast.
* Keep calling `setRobotOrientation` for `HEADING_ASSISTED`. The robot's heading and the camera's offset are separate.

## See Also

* [AprilTag Pose Estimation tutorial](../tutorials/apriltag-pose-estimation.md)
* [Basic Setup](../tutorials/basic-setup.md#where-the-camera-is)
