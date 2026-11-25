---
icon: compass
---

# How do I choose a pose estimation mode?

A **pose estimation mode** is how a camera turns the AprilTags it sees into a robot position. There are two. The difference is whether the camera also uses your gyro's **heading** (which way the robot faces).

## Short answer

Use `HEADING_ASSISTED` on a Limelight or PhotonVision camera if your gyro is reliable, and call `setRobotOrientation` every loop. Otherwise keep the default, `TAGS_ONLY`.

```java
config.withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED);
```

## What each mode does

| | `TAGS_ONLY` (default) | `HEADING_ASSISTED` |
| --- | --- | --- |
| Limelight | MegaTag1 | MegaTag2 |
| PhotonVision | Coprocessor multi-tag result, falling back to the lowest-ambiguity tag | PnP distance trig solve, falling back to `TAGS_ONLY` |
| Synapse | Field-space camera pose | Not supported; same as `TAGS_ONLY` |
| Simulation (every vendor) | PhotonVision multi-tag, then lowest ambiguity | PhotonVision trig solve, then `TAGS_ONLY` |
| Needs the gyro every loop | No | **Yes** |
| Heading of the estimate | Solved from the tags | The gyro's |

## The tradeoff

* **`TAGS_ONLY`** needs nothing but the camera offset. Single-tag estimates can flip between two nearly equal solutions (ambiguity), which the pose filter's ambiguity check catches.
* **`HEADING_ASSISTED`** uses the robot's heading to pick the right solution, so it is steadier at range and at sharp angles. It is only as good as the heading you send. If you forget `setRobotOrientation` on some loops, estimates go stale without any error.

Because a heading-assisted estimate's heading is just the gyro's, YALL gives it a heading standard deviation of 1,000,000 radians: the pose estimator takes its position and ignores its heading. Change it with [`VisionPoseFilter.withHeadingAssistedThetaStdDev(Radians.of(...))`](https://yall.yassrobotics.com/api-reference/java-reference/filter/vision-pose-filter).

[`VisionPoseEstimate.headingAssisted()`](https://yall.yassrobotics.com/api-reference/java-reference/results/vision-pose-estimate) says which method produced an estimate. A heading-assisted PhotonVision camera, or any camera in simulation, falls back to `TAGS_ONLY` when the trig solve has no answer, and those estimates have `headingAssisted() == false`.

## Sending the gyro

In `Robot.robotPeriodic()` (Commands v2 or v3), every loop, before `update()`:

```java
camera.setRobotOrientation(gyro.getRotation3d(), DegreesPerSecond.of(gyroYawRateDegreesPerSecond));
camera.update();
```

The yaw rate also feeds the pose filter's yaw rate check. If you only have a heading, `camera.setRobotOrientation(gyro.getRotation2d())` works and skips that check.

On a Limelight, `setRobotOrientation` passes the yaw, pitch, roll and yaw rate to the camera for MegaTag2. On PhotonVision and in simulation, it adds heading data to PhotonVision's pose estimator.

## Read more

* [AprilTag Pose Estimation tutorial](../tutorials/apriltag-pose-estimation.md)
* [Pose Estimation & Filtering](../understanding/pose-estimation-and-filtering.md)
* [AprilTag Localization Tuning](../tuning/apriltag-and-megatag2-tuning.md): Limelight 4 IMU modes and assist alpha.
* Tuning a Limelight: [Robot Localization with MegaTag2](https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltag-robot-localization-megatag2).
* Tuning a PhotonVision camera: [MultiTag Localization](https://docs.photonvision.org/en/latest/docs/apriltag-pipelines/multitag.html) and [PhotonPoseEstimator](https://docs.photonvision.org/en/latest/docs/programming/photonlib/robot-pose-estimator.html).
