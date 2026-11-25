# VisionResult

**Package:** `yall.results`

```java
public record VisionResult(
    String cameraName, double timestampSeconds, double latencyMillis, List<VisionTarget> targets)
```

One processed camera frame, in the same form for every camera vendor. [`VisionCamera`](../yall/vision-camera.md) returns them from `getLatestResult()` and `getUnreadResults()`.

***

## Components

| Component | Type | Description |
| --- | --- | --- |
| `cameraName` | `String` | Name of the camera that produced the frame. |
| `timestampSeconds` | `double` | When the image was captured, on the `Timer.getMonotonicTimestamp()` timebase that WPILib's pose estimators use. |
| `latencyMillis` | `double` | Time from capture to the result reaching the robot, in milliseconds. |
| `targets` | `List<VisionTarget>` | Targets in the frame, best first. The constructor copies the list, so the result can't change after it is made. |

***

## Methods

| Method | Returns | Description |
| --- | --- | --- |
| `hasTargets()` | `boolean` | Whether the frame has any targets. |
| `getBestTarget()` | `Optional<VisionTarget>` | The first target, as ranked by the camera. Empty if there are none. |
| `getAprilTags()` | `List<VisionTarget>` | Targets with a fiducial ID. |
| `getDetectedObjects()` | `List<VisionTarget>` | Targets with an object detection class ID. |

```java
camera.getLatestResult().ifPresent(result -> {
  for (VisionTarget piece : result.getDetectedObjects()) {
    // piece.classId(), piece.confidence(), piece.yawDegrees()
  }
});
```

***

## See Also

* [VisionTarget](vision-target.md)
* [VisionCamera](../yall/vision-camera.md)
