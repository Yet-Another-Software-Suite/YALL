# How do I mount a Limelight on a moving mechanism?

`withCameraOffset` isn't limited to a fixed, one-time value. Call it every periodic loop with a pose computed from your mechanism's current position when the camera itself moves relative to robot center.

## Example: camera mounted on a rotating turret

Adapted from YALL's `TurretSubsystem` example ([LocalizationExamples](https://github.com/BroncBotz3481/YALL/tree/main/LocalizationExamples)):

```java
@Override
public void periodic() {
    limelight.getSettings()
             .withRobotOrientation(new Orientation3d(new Rotation3d(swerveDrive.getOdometryHeading()),
                                                      new AngularVelocity3d(DegreesPerSecond.of(0),
                                                                            DegreesPerSecond.of(0),
                                                                            DegreesPerSecond.of(0))))
             .withCameraOffset(cameraOffsetFromRobotCenter.rotateAround(
                     turretPivotCenterFromCamera,
                     new Rotation3d(0, 0, turret.getAngle().in(Radians))))
             .save();
}
```

`cameraOffsetFromRobotCenter` is the camera's pose when the turret sits at its zero angle. `rotateAround` re-derives the camera's _current_ offset every loop by rotating that fixed pose around the turret's pivot point by the turret's live angle. Without this, MegaTag pose estimates silently assume the camera never moved and drift as the turret rotates.

## Checklist

* Measure `cameraOffsetFromRobotCenter` and the turret pivot point once, physically, at turret-zero.
* Call `withCameraOffset` with the _recomputed_ pose every loop, not just once at startup.
* Still submit `withRobotOrientation` every loop for MegaTag2; the two settings are independent (robot heading vs. camera-to-robot offset).

## See Also

* [AprilTag Pose Estimation tutorial](../tutorials/apriltag-pose-estimation.md)
* [How do I choose between MegaTag1 and MegaTag2?](how-do-i-choose-megatag1-vs-megatag2.md)
