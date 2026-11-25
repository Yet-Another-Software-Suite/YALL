# Pose Estimation & Ambiguity

## Where a pose estimate comes from

A `PoseEstimate` is decoded from one `botpose*` NetworkTables double-array entry, and which one depends on the `BotPose` value used (`BLUE`, `BLUE_MEGATAG2`, `RED`, `RED_MEGATAG2`). `LimelightPoseEstimator.getPoseEstimate()` picks the blue-origin entry matching its `EstimationMode`; `getAlliancePoseEstimate()` picks the entry matching `DriverStation.getAlliance()`.

Each `BotPose` value caches one `PoseEstimate` object per camera it's queried with, so repeated calls in the same loop don't re-allocate, but each call to `getPoseEstimate()` does re-read NetworkTables and re-decode the array.

## Why you must filter before fusing

Not every pose estimate is trustworthy. A single distant or oblique-angle tag can produce a pose that's technically "present" but wildly inaccurate. `PoseEstimate` exposes exactly the fields you need to gate on:

| Field                                                                    | What it tells you                                                                                                                        |
| ------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `hasData`                                                                | Whether any tag contributed to this estimate at all.                                                                                     |
| `tagCount`                                                               | More tags generally means a more constrained, trustworthy solve.                                                                         |
| `avgTagDist`                                                             | Farther tags have more angular pose error per pixel of detection noise.                                                                  |
| `avgTagArea`                                                             | A proxy for distance/detection quality: larger is closer/clearer.                                                                        |
| `tagSpan`                                                                | Wider-spread tags better constrain rotation than tags clustered together.                                                                |
| `getMinTagAmbiguity()` / `getMaxTagAmbiguity()` / `getAvgTagAmbiguity()` | Per-tag pose ambiguity (0-1) from SolvePnP. Near-symmetric tag views produce multiple plausible poses, which shows up as high ambiguity. |

The pattern used throughout YALL's examples:

```java
if (poseEstimate.avgTagDist < 4 && poseEstimate.tagCount > 0 && poseEstimate.getMinTagAmbiguity() < 0.3) {
    poseEstimator.addVisionMeasurement(poseEstimate.pose.toPose2d(), poseEstimate.timestampSeconds);
}
```

Tune the thresholds (`4` meters, `0.3` ambiguity) to your field and camera; they aren't universal constants, just a reasonable starting point.

## Timestamps matter

`PoseEstimate.timestampSeconds` is the NT server timestamp adjusted for total pipeline latency, not "now." Always pass it (not `Timer.getFPGATimestamp()`) to `addVisionMeasurement`, so your pose estimator correctly accounts for how stale the vision reading actually is relative to your odometry.

## MegaTag1 vs. MegaTag2 fields

Every field above works identically regardless of which `EstimationMode` produced the estimate; `isMegaTag2` just records which one it was. The meaningful difference between the two modes is accuracy and setup requirements, covered in [How do I choose between MegaTag1 and MegaTag2?](../how-to-guides/how-do-i-choose-megatag1-vs-megatag2.md)

## See Also

* [AprilTag Pose Estimation tutorial](../tutorials/apriltag-pose-estimation.md)
* [AprilTag & MegaTag2 Localization Tuning](../tuning/apriltag-and-megatag2-tuning.md): why more, more-spread-out tags produce a more trustworthy estimate.
* [ChArUco Camera Calibration](../tuning/charuco-camera-calibration.md): accurate intrinsics are what these fields are measuring the quality of in the first place.
* PoseEstimate reference
* RawFiducial reference
