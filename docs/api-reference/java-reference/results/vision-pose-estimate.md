---
icon: location-dot
---

# VisionPoseEstimate

In short: a camera's guess at where the robot is, with when the picture was taken and how much to trust it.

**Package:** `yall.results`

```java
public record VisionPoseEstimate(
    String cameraName,
    Pose3d pose,
    double timestampSeconds,
    List<Integer> tagIds,
    double averageTagDistanceMeters,
    double ambiguity,
    boolean headingAssisted,
    Matrix<N3, N1> stdDevs)
```

A robot pose estimated from AprilTags, ready for a WPILib pose estimator. [`VisionCamera`](../yall/vision-camera.md) returns them from `getEstimatedPose()` and `getEstimatedPoses()`, after the [`VisionPoseFilter`](../filter/vision-pose-filter.md) has accepted them and filled in their standard deviations.

```java
camera.getEstimatedPose().ifPresent(estimate ->
    poseEstimator.addVisionMeasurement(
        estimate.pose2d(), estimate.timestampSeconds(), estimate.stdDevs()));
```

***

## Components

| Component | Type | Description |
| --- | --- | --- |
| `cameraName` | `String` | Name of the camera that produced the estimate, or `"fused"` for one from [`VisionSystem.getFusedPose()`](../yall/vision-system.md). |
| `pose` | `Pose3d` | Estimated robot pose on the field, in WPILib blue-origin field coordinates. |
| `timestampSeconds` | `double` | When the image was captured, on the `Timer.getMonotonicTimestamp()` timebase. |
| `tagIds` | `List<Integer>` | IDs of the AprilTags used. The constructor copies the list. |
| `averageTagDistanceMeters` | `double` | Average distance from the camera to the tags used. |
| `ambiguity` | `double` | Pose ambiguity of a single-tag estimate, 0 to 1. 0 for multi-tag estimates, and `NaN` if the camera doesn't report it. |
| `headingAssisted` | `boolean` | Whether the estimate used the robot heading from `setRobotOrientation`, so its rotation is the gyro's rather than the camera's. |
| `stdDevs` | `Matrix<N3, N1>` | Standard deviations of x (meters), y (meters) and heading (radians) for `PoseEstimator.addVisionMeasurement`. |

***

## Methods

| Method | Returns | Description |
| --- | --- | --- |
| `static unfiltered(String cameraName, Pose3d pose, double timestampSeconds, List<Integer> tagIds, double averageTagDistanceMeters, double ambiguity, boolean headingAssisted)` | `VisionPoseEstimate` | An estimate whose standard deviations the filter hasn't filled in yet: they are zero. Wrappers build their estimates with it. |
| `tagCount()` | `int` | Number of AprilTags used. |
| `pose2d()` | `Pose2d` | The estimated pose on the floor. |
| `withStdDevs(Matrix<N3, N1> stdDevs)` | `VisionPoseEstimate` | A copy with other standard deviations. |

***

## Read more

* [VisionPoseFilter](../filter/vision-pose-filter.md)
* [VisionSystem](../yall/vision-system.md)
