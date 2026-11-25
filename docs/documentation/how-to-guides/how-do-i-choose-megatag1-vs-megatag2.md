# How do I choose between MegaTag1 and MegaTag2?

## Short answer

Use **MegaTag2** unless you have a specific reason not to. It's smoother, more accurate at range and oblique angles, and is what `LimelightPoseEstimator.getPoseEstimate()` and `BotPose.BLUE`/`RED` recommend over their non-`_MEGATAG2` counterparts.

## The tradeoff

|                                               | MegaTag1                      | MegaTag2                                                                   |
| --------------------------------------------- | ----------------------------- | -------------------------------------------------------------------------- |
| Data required                                 | Visual only (single AprilTag) | AprilTag + robot heading, submitted every loop                             |
| Accuracy at range / oblique angles            | Lower                         | Higher                                                                     |
| Setup complexity                              | None beyond camera offset     | Must call `withRobotOrientation` every periodic loop before reading a pose |
| Fails silently if orientation isn't submitted | N/A                           | **Yes**: stale/inaccurate poses with no error                              |

## Using MegaTag1

```java
LimelightPoseEstimator estimator = limelight.createPoseEstimator(EstimationMode.MEGATAG1);
Optional<PoseEstimate> estimate = estimator.getPoseEstimate();
```

No per-loop orientation submission needed.

## Using MegaTag2

```java
LimelightPoseEstimator estimator = limelight.createPoseEstimator(EstimationMode.MEGATAG2);

// Every periodic loop, BEFORE reading a pose estimate:
limelight.getSettings()
         .withRobotOrientation(new Orientation3d(gyro.getRotation3d(),
                                                  new AngularVelocity3d(DegreesPerSecond.of(0),
                                                                        DegreesPerSecond.of(0),
                                                                        DegreesPerSecond.of(0))))
         .save();

Optional<PoseEstimate> estimate = estimator.getPoseEstimate();
```

If your gyro doesn't report angular velocity per-axis, passing zeros for pitch/roll rate is fine, since only yaw rate meaningfully affects MegaTag2's fusion, and even that is optional in most drivetrains.

## See Also

* [AprilTag Pose Estimation tutorial](../tutorials/apriltag-pose-estimation.md)
* [Pose Estimation & Ambiguity](../understanding/pose-estimation-and-ambiguity.md)
* [AprilTag & MegaTag2 Localization Tuning](../tuning/apriltag-and-megatag2-tuning.md): the full `ImuMode` breakdown and assist alpha tuning.
* LimelightSettings.ImuMode: for Limelight 4's internal IMU fusion options.
