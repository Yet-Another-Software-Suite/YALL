---
icon: chart-column
---

# PoseFilterStats

In short: counts what the pose filter kept and threw out, and measures how much the kept guesses wobble.

**Package:** `yall.filter`

```java
public class PoseFilterStats
```

Counts of what a camera's [`VisionPoseFilter`](vision-pose-filter.md) accepted and rejected, and how spread out the accepted poses are. Every [`VisionCamera`](../yall/vision-camera.md) keeps one: `camera.getFilterStats()`, reset with `camera.resetFilterStats()`.

The spread is how to find a camera's standard deviations: hold the robot still where it sees tags, reset, wait a few seconds, then read `getStdDevs()`. `getSuggestedBaseStdDevs(divisor)` turns that into the base for [`VisionPoseFilter.withStdDevs`](vision-pose-filter.md#standard-deviations). See [Measuring Standard Deviations](https://yall.yassrobotics.com/documentation/tutorials/measuring-standard-deviations).

***

## Counts

| Method | Returns | Description |
| --- | --- | --- |
| `getAcceptedCount()` | `long` | How many estimates the filter accepted. |
| `getRejectedCount()` | `long` | How many estimates the filter rejected. |
| `getRejectedCount(String reason)` | `long` | How many one check rejected, by its rejection reason, such as `"ambiguous"`, or a custom check's name. |
| `getRejectionCounts()` | `Map<String, Long>` | Counts by rejection reason, in the order the reasons first appeared. |
| `getAcceptanceRate()` | `double` | The share accepted, 0 to 1. `NaN` before any estimate. |

***

## Spread of the accepted poses

| Method | Returns | Description |
| --- | --- | --- |
| `getSampleCount()` | `long` | How many accepted poses the spread is measured from. |
| `getMeanPose()` | `Optional<Pose2d>` | The average accepted pose. Empty before any. |
| `getStdDevs()` | `Matrix<N3, N1>` | Sample standard deviation of x (m), y (m) and heading (rad). Heading is measured around the first sample, so a spread across ±180° doesn't wrap. `NaN`s with fewer than two samples. |
| `getAverageTagDistanceMeters()` | `double` | Average tag distance of the accepted poses. `NaN` before any. |
| `getSuggestedBaseStdDevs(double distanceDivisor)` | `Matrix<N3, N1>` | The base standard deviations that give the measured spread at the measured distance: `stdDevs / (1 + distance² / distanceDivisor)`. Pass the filter's `getDistanceDivisor()` (30 by default). |

***

## Recording

[`VisionCamera`](../yall/vision-camera.md) calls these in `update()`; robot code doesn't need to.

| Method | Description |
| --- | --- |
| `recordAccepted(VisionPoseEstimate estimate)` | Counts an accepted estimate and adds its pose to the spread. |
| `recordRejected(String reason)` | Counts a rejected estimate under its reason. |
| `reset()` | Starts counting again. |

***

## NetworkTables

With the [`FILTER_STATS`](../telemetry/vision-telemetry-field.md) telemetry field on (it is on at `HIGH` verbosity, or turn it on by itself with `withField(VisionTelemetryField.FILTER_STATS, true)`), each camera publishes its stats under `YALL/<camera name>/stats/`:

| Key | Type | Description |
| --- | --- | --- |
| `accepted` | `int` | Accepted count. |
| `rejected` | `int` | Rejected count. |
| `rejectedBy/<reason>` | `int` | Rejected count for each check, built-in or custom. |
| `acceptanceRate` | `double` | Share accepted, 0 to 1. |
| `sampleCount` | `int` | Accepted poses in the spread. |
| `meanPose` | `Pose2d` | Average accepted pose. |
| `stdDevs` | `double[3]` | x (m), y (m), heading (rad). |
| `suggestedBaseStdDevs` | `double[3]` | `stdDevs / (1 + distance² / divisor)`, with the camera's filter divisor. |
| `averageTagDistanceMeters` | `double` | Average tag distance. |
| `reset` | `boolean` | Set it to true from a dashboard to reset the stats. The camera sets it back to false. |

***

## Read more

* [VisionPoseFilter](vision-pose-filter.md)
* [VisionTelemetryConfig](../telemetry/vision-telemetry-config.md)
