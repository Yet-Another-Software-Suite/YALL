# PoseEstimate

**Package:** `limelight.networktables`

A single 3D pose estimate read from one Limelight `botpose*` NetworkTables entry, along with the AprilTag observations that produced it. Instances come from [`LimelightPoseEstimator`](limelight-pose-estimator.md) or `BotPose.get(limelight)` — you generally don't construct these directly.

***

## Fields

| Field              | Type              | Description                                                     |
| ------------------ | ----------------- | --------------------------------------------------------------- |
| `pose`             | `Pose3d`          | Estimated robot pose.                                           |
| `timestampSeconds` | `double`          | NT server timestamp, adjusted for total latency, in seconds.    |
| `latency`          | `double`          | Total pipeline latency in milliseconds.                         |
| `tagCount`         | `int`             | Number of AprilTags used in this estimate.                      |
| `tagSpan`          | `double`          | Maximum distance between visible tags, in meters.               |
| `avgTagDist`       | `double`          | Average camera-to-tag distance, in meters.                      |
| `avgTagArea`       | `double`          | Average tag area as a percentage of the image.                  |
| `rawFiducials`     | `RawFiducial[]`   | Per-tag detail for every tag used in the estimate.              |
| `hasData`          | `boolean`         | `true` if the estimate contains at least one observed fiducial. |
| `isMegaTag2`       | `boolean` (final) | Whether this estimate came from the MegaTag2 pipeline.          |

***

## Methods

| Method                 | Returns                  | Description                                                                                      |
| ---------------------- | ------------------------ | ------------------------------------------------------------------------------------------------ |
| `getPoseEstimate()`    | `Optional<PoseEstimate>` | Re-reads the bound NT entry and repopulates this object's fields. Empty if no data is published. |
| `refresh()`            | `PoseEstimate`           | Calls `getPoseEstimate()` and returns `this` for chaining.                                       |
| `getMinTagAmbiguity()` | `double`                 | Minimum `ambiguity` across `rawFiducials`. Returns `1` if `hasData` is `false`.                  |
| `getMaxTagAmbiguity()` | `double`                 | Maximum `ambiguity` across `rawFiducials`. Returns `1` if `hasData` is `false`.                  |
| `getAvgTagAmbiguity()` | `double`                 | Average `ambiguity` across `rawFiducials`. Returns `1` if `hasData` is `false`.                  |

***

## Filtering Before Fusing

`avgTagDist`, `tagCount`, and the ambiguity helpers are the standard gate before calling `addVisionMeasurement` on a WPILib pose estimator — reject stale or unreliable reads instead of trusting every frame:

```java
visionEstimate.ifPresent(estimate -> {
    if (estimate.avgTagDist < 4 && estimate.tagCount > 0 && estimate.getMinTagAmbiguity() < 0.3) {
        poseEstimator.addVisionMeasurement(estimate.pose.toPose2d(), estimate.timestampSeconds);
    }
});
```

## See Also

* [LimelightPoseEstimator](limelight-pose-estimator.md)
* [RawFiducial](../results-and-telemetry/raw-fiducial.md)
