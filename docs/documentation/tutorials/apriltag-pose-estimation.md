# AprilTag Pose Estimation

This tutorial feeds a camera's AprilTag pose estimates into a WPILib pose estimator, so odometry stays corrected over a match.

## 1. Give the camera its offset

Pose estimates are only as good as the camera's position on the robot:

```java
VisionCameraConfig config = new VisionCameraConfig()
    .withRobotToCamera(Inches.of(12), Inches.zero(), Inches.of(10),
                       Degrees.zero(), Degrees.of(-15), Degrees.zero());
```

## 2. Choose how tags become a pose

```java
config.withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED);
```

| Mode | Limelight | PhotonVision | Synapse |
| --- | --- | --- | --- |
| `TAGS_ONLY` (default) | MegaTag1 | Multi-tag result, falling back to the lowest-ambiguity tag | Field-space camera pose |
| `HEADING_ASSISTED` | MegaTag2 | PnP distance trig solve, falling back to `TAGS_ONLY` | Same as `TAGS_ONLY` |

`HEADING_ASSISTED` uses the gyro heading to steady the estimate, so it needs `setRobotOrientation` every loop. See [How do I choose a pose estimation mode?](../how-to-guides/how-do-i-choose-a-pose-estimation-mode.md)

## 3. Send the gyro every loop

```java
camera.setRobotOrientation(gyro.getRotation3d(), DegreesPerSecond.of(gyroYawRateDegreesPerSecond));
```

Call it before `update()`. With only a heading, use `camera.setRobotOrientation(gyro.getRotation2d())`; the pose filter's yaw rate check is then skipped.

## 4. Add the estimates to your pose estimator

```java
@Override
public void periodic() {
  poseEstimator.update(gyro.getRotation2d(), getModulePositions());

  camera.setRobotOrientation(gyro.getRotation3d(), DegreesPerSecond.of(gyroYawRateDegreesPerSecond));
  camera.update();
  camera.addVisionMeasurements(poseEstimator);
}
```

`addVisionMeasurements` passes every estimate that passed the filter, oldest first, with its capture timestamp and standard deviations. It works with any WPILib pose estimator (`SwerveDrivePoseEstimator`, `DifferentialDrivePoseEstimator`, ...).

For a drivetrain with its own vision method, pass each estimate yourself:

```java
camera.forEachEstimatedPose(estimate ->
    drivetrain.addVisionMeasurement(estimate.pose2d(), estimate.timestampSeconds(), estimate.stdDevs()));
```

## 5. Check what the filter rejects

Every estimate goes through the camera's `VisionPoseFilter` first. The defaults reject estimates that are off the field, off the floor, too ambiguous, from tags too far away, taken while spinning faster than 720 °/s, or older than 0.5 s.

```java
List<VisionPoseEstimate> accepted = camera.getEstimatedPoses();
List<VisionPoseEstimate> rejected = camera.getRejectedPoses();
String why = camera.getLastRejectionReason(); // for example "tags too far"
```

The same lists are published to NetworkTables under `YALL/<camera name>` as `acceptedPoses`, `rejectedPoses` and `rejectionReason`. Plot them on a field to see the filter at work. To change a check, see [How do I tune the pose filter?](../how-to-guides/how-do-i-tune-the-pose-filter.md)

## The estimate itself

`getEstimatedPose()` returns the newest accepted estimate from the last `update()`:

```java
camera.getEstimatedPose().ifPresent(estimate -> {
  Pose2d pose = estimate.pose2d();
  double timestamp = estimate.timestampSeconds();       // capture time
  int tags = estimate.tagCount();
  double distance = estimate.averageTagDistanceMeters();
  Matrix<N3, N1> stdDevs = estimate.stdDevs();          // x, y (m), heading (rad)
});
```

It is empty when the last `update()` produced no accepted estimate, so don't treat empty as "no tags in view".

## See Also

* [Pose Estimation & Filtering](../understanding/pose-estimation-and-filtering.md)
* [How do I use multiple cameras?](../how-to-guides/how-do-i-use-multiple-cameras.md)
* [AprilTag & MegaTag2 Localization Tuning](../tuning/apriltag-and-megatag2-tuning.md)
