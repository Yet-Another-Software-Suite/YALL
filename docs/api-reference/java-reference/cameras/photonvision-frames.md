---
icon: right-left
---

# PhotonVisionFrames

In short: turns PhotonVision's results into YALL's.

**Package:** `yall.cameras`

```java
public final class PhotonVisionFrames
```

Turns PhotonVision results into YALL frames. [`PhotonVisionWrapper`](photonvision-wrapper.md) uses it on real robots, and every simulated [`VisionCamera`](../yall/vision-camera.md) uses it for every simulated camera, whatever its vendor. Robot code doesn't need it; it is for wrappers built on PhotonVision.

***

## Methods

| Method | Returns | Description |
| --- | --- | --- |
| `static read(String cameraName, PhotonCamera camera, PhotonPoseEstimator poseEstimator, PoseEstimationMode mode)` | `List<VisionFrame>` | Reads a PhotonVision camera's unread results and turns them into [`VisionFrame`](../yall/vision-frame.md)s, oldest first. |
| `static toVisionResult(String cameraName, PhotonPipelineResult result)` | [`VisionResult`](../results/vision-result.md) | Turns one PhotonVision result into a YALL result. |
| `static estimatePose(String cameraName, PhotonPipelineResult result, PhotonPoseEstimator poseEstimator, PoseEstimationMode mode)` | `Optional<VisionPoseEstimate>` | Estimates the robot pose from one result. Empty if the result has no tags. |

***

## How results are turned into YALL's

* **Targets:** yaw, pitch and area pass straight through. A target that isn't a detected object gets class ID -1 and confidence 0 (PhotonVision reports -1). Ambiguity is `NaN` for anything but an AprilTag. AprilTags keep their best camera-to-tag transform.
* **Pose:** with [`PoseEstimationMode.HEADING_ASSISTED`](../yall/pose-estimation-mode.md), the PnP distance trig solve is tried first. Then the coprocessor's multi-tag result, then the lowest-ambiguity tag. The estimate is heading-assisted only if the trig solve made it.
* **Tag distance:** the average length of the camera-to-tag transforms of the tags used.
* **Ambiguity:** the tag's ambiguity for one tag, 0 for several.

***

## Read more

* [PhotonVisionWrapper](photonvision-wrapper.md)
