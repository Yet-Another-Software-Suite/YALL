---
icon: filter
---

# VisionPoseFilter

In short: throws out pose guesses that can't be right, and says how much to trust the rest.

**Package:** `yall.filter`

```java
public class VisionPoseFilter
```

Decides which vision pose estimates to trust, and how much. Every [`VisionCamera`](../yall/vision-camera.md) runs its estimates through the filter in its config. Each `withX` method sets one check and returns `this` for chaining. Pass `null` to turn the check off, and read it back with its `getX` method, which returns an `Optional`.

```java
new VisionCameraConfig()
    .withPoseFilter(new VisionPoseFilter()
        .withMaxAmbiguity(0.15)       // tighter than the default
        .withMaxTilt(Degrees.of(10))  // on; off by default
        .withMaxYawRate(null));       // off
```

***

## Constructors

| Constructor | Description |
| --- | --- |
| `VisionPoseFilter()` | A filter with the default checks below. |
| `static acceptAll()` | A filter with every check off. It accepts every estimate that used a tag. |

***

## Checks and defaults

Estimates that used no tags are always rejected. The checks run in this order, and the first to fail gives the rejection reason.

| Method | Default | Rejects an estimate when | Reason |
| --- | --- | --- | --- |
| `withMinTagCount(Integer tagCount)` | 1 | it used fewer tags | `"too few tags"` |
| `withMaxAge(Time maxAge)` | 0.5 s | its image is older than the limit, or more than 0.05 s in the future | `"too old"`, `"timestamp in the future"` |
| `withFieldBounds(Distance margin)` | 0.5 m | the robot would be more than the margin off the field | `"off the field"` |
| `withMaxHeightError(Distance maxHeightError)` | 0.5 m | the robot origin would be further than the limit above or below the floor | `"off the floor"` |
| `withMaxTilt(Angle maxTilt)` | Off | the robot would be rolled or pitched past the limit. Leave it off if the robot drives over ramps or bumps. | `"tilted"` |
| `withMaxAmbiguity(Double ambiguity)` | 0.2 | a single-tag estimate's ambiguity is above the limit. Multi-tag estimates, and cameras that don't report ambiguity, aren't checked. | `"ambiguous"` |
| `withMaxTagDistance(Distance maxSingleTagDistance, Distance maxMultiTagDistance)` | 4 m for one tag, 8 m for several | the average tag distance is above the limit. Each limit turns off on its own with `null`. | `"tags too far"` |
| `withMaxYawRate(AngularVelocity maxYawRate)` | 720 °/s | the robot spun faster than the limit. Needs the yaw rate from `setRobotOrientation`; without it, estimates aren't checked. | `"spinning too fast"` |
| `withMaxDistanceFromPose(Supplier<Pose2d> currentPose, Distance maxDistance)` | Off | the estimate is further than the limit from the robot's current pose. `null` for either argument turns it off. Turn it on only once the robot knows where it is. | `"too far from the current pose"` |

***

## Custom checks

Add checks of your own. They run after the built-in ones, in the order they were added.

| Method | Returns | Description |
| --- | --- | --- |
| `withCustomCheck(String name, Predicate<VisionPoseEstimate> accepts)` | `VisionPoseFilter` | Adds a check: `accepts` returns true to accept an estimate. The name is required: it is the rejection reason, and [`PoseFilterStats`](pose-filter-stats.md) counts the check's rejections under it. A check with the same name is replaced; `null` for `accepts` removes it. Throws `IllegalArgumentException` if the name is null or blank. |
| `getCustomChecks()` | `Map<String, Predicate<VisionPoseEstimate>>` | The custom checks by name, in the order they were added. |

```java
new VisionPoseFilter()
    .withCustomCheck("not in our half", estimate -> estimate.pose().getX() < 8.27);
```

***

## Standard deviations

Accepted estimates get standard deviations of `base * (1 + d² / distanceDivisor)`, where `d` is the average tag distance in meters. To measure your camera's real base, see [`PoseFilterStats.getSuggestedBaseStdDevs`](pose-filter-stats.md).

| Method | Default | Description |
| --- | --- | --- |
| `withStdDevs(Matrix<N3, N1> singleTagBase, Matrix<N3, N1> multiTagBase, double distanceDivisor)` | (4, 4, 8) for one tag, (0.5, 0.5, 1) for several, divisor 30 | Base standard deviations of x (m), y (m) and heading (rad). A smaller divisor makes distant tags count for less. |
| `withHeadingAssistedThetaStdDev(Angle thetaStdDev)` | 1e6 rad | Heading standard deviation for heading-assisted estimates (Limelight MegaTag2, PhotonVision's trig solve). Their heading is the gyro's, so it should carry almost no weight. |

***

## Getters

Each returns the check's setting, or empty if the check is off.

| Method | Returns |
| --- | --- |
| `getFieldBoundsMargin()` | `Optional<Distance>` |
| `getMaxHeightError()` | `Optional<Distance>` |
| `getMaxTilt()` | `Optional<Angle>` |
| `getMaxAmbiguity()` | `Optional<Double>` |
| `getMinTagCount()` | `Optional<Integer>` |
| `getMaxSingleTagDistance()` | `Optional<Distance>` |
| `getMaxMultiTagDistance()` | `Optional<Distance>` |
| `getMaxYawRate()` | `Optional<AngularVelocity>` |
| `getMaxAge()` | `Optional<Time>` |
| `getMaxDistanceFromPose()` | `Optional<Distance>` |
| `getDistanceDivisor()` | `double` (the standard deviation formula's divisor, not optional) |

***

## Applying the filter

Cameras call these; robot code rarely needs to.

| Method | Returns | Description |
| --- | --- | --- |
| `check(VisionPoseEstimate estimate, Field field, double nowSeconds, double yawRateDegreesPerSecond)` | `Optional<String>` | Why the estimate was rejected, or empty if it passes every enabled check. `field` is for the bounds check; `null` skips it. `nowSeconds` is the current `Timer.getMonotonicTimestamp()`. `yawRateDegreesPerSecond` is `NaN` if unknown. |
| `applyStdDevs(VisionPoseEstimate estimate)` | [`VisionPoseEstimate`](../results/vision-pose-estimate.md) | The estimate with its standard deviations filled in. |

***

## Read more

* [VisionPoseEstimate](../results/vision-pose-estimate.md)
* [PoseFilterStats](pose-filter-stats.md)
* [VisionCameraConfig](../yall/vision-camera-config.md)
