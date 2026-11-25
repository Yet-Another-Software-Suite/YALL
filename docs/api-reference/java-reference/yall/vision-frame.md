---
icon: film
---

# VisionFrame

In short: one picture from a camera, before the pose filter sees it. Camera wrappers make these.

**Package:** `yall`

```java
public record VisionFrame(
    VisionResult result,
    Optional<VisionPoseEstimate> estimate,
    Optional<String> vendorRejection,
    Optional<Matrix<N3, N1>> vendorStdDevs)
```

One frame read from a camera, before the pose filter. Vendor wrappers return them from `readFrames()`; [`VisionCamera`](vision-camera.md) filters their estimates. Robot code reads [`VisionResult`](../results/vision-result.md)s and [`VisionPoseEstimate`](../results/vision-pose-estimate.md)s instead.

***

## Components

| Component | Type | Description |
| --- | --- | --- |
| `result` | [`VisionResult`](../results/vision-result.md) | The frame's targets. |
| `estimate` | `Optional<VisionPoseEstimate>` | The robot pose estimated from the frame, if any. |
| `vendorRejection` | `Optional<String>` | Why the vendor's own filter rejected the estimate, if it did. It replaces the pose filter's checks, and the stats count it under this reason. |
| `vendorStdDevs` | `Optional<Matrix<N3, N1>>` | Standard deviations from the vendor, used instead of the filter's. |

***

## Constructors

| Constructor | Description |
| --- | --- |
| `VisionFrame(VisionResult result, Optional<VisionPoseEstimate> estimate, Optional<String> vendorRejection, Optional<Matrix<N3, N1>> vendorStdDevs)` | A frame with every part. |
| `VisionFrame(VisionResult result, Optional<VisionPoseEstimate> estimate)` | A frame with no vendor filtering. |

***

## Read more

* [VisionCamera: writing a wrapper](vision-camera.md#writing-a-wrapper)
* [PhotonVisionFrames](../cameras/photonvision-frames.md)
