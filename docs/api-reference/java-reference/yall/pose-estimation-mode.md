# PoseEstimationMode

**Package:** `yall`

```java
public enum PoseEstimationMode
```

How a camera turns the AprilTags it sees into a robot pose. Set it with [`VisionCameraConfig.withPoseEstimationMode`](vision-camera-config.md#pose-estimation).

***

## Values

| Value | Description |
| --- | --- |
| `TAGS_ONLY` | Solve the robot pose from the tags alone: Limelight MegaTag1, PhotonVision's multi-tag result with a lowest-ambiguity fallback, and Synapse's field-space camera pose. The default. |
| `HEADING_ASSISTED` | Use the robot heading from [`VisionCamera.setRobotOrientation`](vision-camera.md#updating) to steady the estimate: Limelight MegaTag2 and PhotonVision's PnP distance trig solve. Synapse doesn't support it and falls back to `TAGS_ONLY`. Call `setRobotOrientation` every loop. |

***

## What each camera does

| Camera | `TAGS_ONLY` | `HEADING_ASSISTED` |
| --- | --- | --- |
| Limelight | MegaTag1, blue origin | MegaTag2, blue origin |
| PhotonVision | Coprocessor multi-tag, then lowest-ambiguity tag | PnP distance trig solve, then the `TAGS_ONLY` methods |
| Synapse | Field-space camera pose, moved back by the robot-to-camera transform | Same as `TAGS_ONLY` |
| Any camera in simulation | The PhotonVision methods above | The PhotonVision methods above |

Heading-assisted estimates have [`VisionPoseEstimate.headingAssisted()`](../results/vision-pose-estimate.md) set. Their heading is the gyro's, so the pose filter gives them a very large heading standard deviation (see [`withHeadingAssistedThetaStdDev`](../filter/vision-pose-filter.md#standard-deviations)).

***

## See Also

* [VisionCameraConfig](vision-camera-config.md)
* [VisionPoseEstimate](../results/vision-pose-estimate.md)
