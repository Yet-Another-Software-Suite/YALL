# AprilTag & MegaTag2 Localization Tuning

This page covers the setup steps that make MegaTag2 robot localization actually accurate, on top of the basic wiring already covered in the [AprilTag Pose Estimation tutorial](../tutorials/apriltag-pose-estimation.md) and [How do I choose between MegaTag1 and MegaTag2?](../how-to-guides/how-do-i-choose-megatag1-vs-megatag2.md)

## Why MegaTag beats single-tag pose

A single AprilTag's pose solve is sensitive to noise and near-symmetric ambiguity. MegaTag fuses keypoints from every visible tag into one combined solve, which is far more stable: the more tags and the more spread out they are, the better constrained the result:

<figure><img src="../.gitbook/assets/megatag-diagram.png" alt=""><figcaption><p>MegaTag fuses keypoints across all visible tags. Real-world results are even better than this simplified planar diagram suggests.</p></figcaption></figure>

This is exactly why [Pose Estimation & Ambiguity](../understanding/pose-estimation-and-ambiguity.md) recommends filtering `addVisionMeasurement` calls on `tagCount` and `tagSpan`, not just ambiguity: more, more-spread-out tags is the mechanism that makes MegaTag reliable in the first place.

## Prerequisites checklist

1. **Camera offset**: `limelight.getSettings().withCameraOffset(...)` set once, accurately, in robot space. Every localization result is only as good as this offset.
2. **Field map**: upload the correct `.fmap` for the season's field to the Limelight.
3. **Blue-corner origin**: MegaTag2 (2024+) always uses the blue-corner field origin; a mismatched origin convention silently rotates/flips every pose.
4. **Robot orientation every loop**: `withRobotOrientation(...)` must be called every periodic loop, _before_ reading a pose estimate. This is the single most common MegaTag2 mistake. Skip it and you get stale or inaccurate poses with no error thrown.

## Tuning the internal IMU (Limelight 4)

`LimelightSettings.ImuMode` controls how the Limelight 4's internal IMU interacts with your externally-submitted orientation:

| `ImuMode`                   | Behavior                                                                                 | When to use                                                                                                            |
| --------------------------- | ---------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `ExternalImu`               | Internal IMU ignored entirely; MT2 uses only what you submit via `withRobotOrientation`. | Simplest option; fine if your external gyro is trustworthy every loop.                                                 |
| `SyncInternalImu`           | Internal IMU stays synced to your external submissions.                                  | Good default while disabled/pre-match; "seeds" the internal IMU.                                                       |
| `InternalImu`               | Internal IMU drives MT2; external submissions ignored.                                   | Rarely, only if you have no reliable external gyro.                                                                    |
| `InternalImuMT1Assist`      | Internal IMU drives MT2, continuously corrected by filtered MT1 (vision-only) yaw.       | Enabled, vision-heavy matches; gentle correction without needing a gyro.                                               |
| `InternalImuExternalAssist` | Internal IMU drives MT2, continuously corrected by your external submissions.            | Enabled: a good default once your gyro is confirmed working, more robust to a single bad gyro read than `ExternalImu`. |

A common pattern: seed with `SyncInternalImu` (or `ExternalImu`) while disabled, then switch to `InternalImuMT1Assist`/`InternalImuExternalAssist` once enabled:

```java
@Override
public void disabledInit() {
    limelight.getSettings().withImuMode(ImuMode.SyncInternalImu).save();
}

@Override
public void teleopInit() {
    limelight.getSettings().withImuMode(ImuMode.InternalImuExternalAssist).save();
}
```

### Complementary filter alpha

`withImuAssistAlpha(alpha)` (default `0.001`) controls how fast the internal IMU converges toward whichever assist source is active:

* **Lower**: the internal IMU stays stable but corrects drift slowly.
* **Higher**: corrects drift faster, at the cost of reacting more to noise in the assist source, which matters more during rapid turns.

Start at the default and only raise it if you observe heading drift the assist source isn't correcting fast enough.

## Common setup mistakes

* Forgetting `withRobotOrientation(...)` on some loop iterations (e.g. an early `return` before the settings call).
* No `.fmap` uploaded, or an outdated one from a previous season.
* Mounting a Limelight 4 in portrait orientation: its internal IMU requires landscape mounting.
* Assuming the field origin convention from a pre-2024 season still applies.

## See Also

* [Tuning Workflow Overview](tuning-workflow-overview.md)
* [ChArUco Camera Calibration](charuco-camera-calibration.md): accurate intrinsics are a prerequisite for accurate MegaTag poses.
* [How do I choose between MegaTag1 and MegaTag2?](../how-to-guides/how-do-i-choose-megatag1-vs-megatag2.md)
* [Pose Estimation & Ambiguity](../understanding/pose-estimation-and-ambiguity.md)
