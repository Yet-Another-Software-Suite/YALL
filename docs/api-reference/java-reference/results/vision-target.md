---
icon: bullseye
---

# VisionTarget

In short: one thing the camera saw, like an AprilTag or a game piece, and where it is in the picture.

**Package:** `yall.results`

```java
public record VisionTarget(
    double yawDegrees,
    double pitchDegrees,
    double areaPercent,
    int fiducialId,
    int classId,
    double confidence,
    double ambiguity,
    Optional<Transform3d> cameraToTarget)
```

One target a camera sees, in the same form for every camera vendor.

Angles follow PhotonVision's convention: yaw is positive to the right of the crosshair, pitch is positive above it. Limelight's per-target `tx` is already positive right. Its per-target `ty` is positive down, so [`LimelightWrapper`](../cameras/limelight-wrapper.md) negates it.

***

## Components

| Component | Type | Description |
| --- | --- | --- |
| `yawDegrees` | `double` | Horizontal angle from the crosshair to the target, positive right. |
| `pitchDegrees` | `double` | Vertical angle from the crosshair to the target, positive up. |
| `areaPercent` | `double` | Share of the image the target covers, 0 to 100. `NaN` if the camera doesn't report it (Synapse). |
| `fiducialId` | `int` | AprilTag ID, or -1 if the target isn't an AprilTag. |
| `classId` | `int` | Object detection class ID, or -1 if the target isn't a detected object. |
| `confidence` | `double` | Object detection confidence, 0 to 1. 0 if the target isn't a detected object. |
| `ambiguity` | `double` | AprilTag pose ambiguity, 0 to 1. `NaN` if the camera doesn't report it. |
| `cameraToTarget` | `Optional<Transform3d>` | Transform from the camera to the target, if the camera solved it (PhotonVision and Limelight AprilTags). |

***

## Methods

| Method | Returns | Description |
| --- | --- | --- |
| `isAprilTag()` | `boolean` | Whether `fiducialId()` is a tag ID. |
| `isDetectedObject()` | `boolean` | Whether `classId()` is a class ID. |

```java
camera.getBestTarget().ifPresent(target -> {
  double turn = -kP * target.yawDegrees(); // target to the right: turn right
});
```

***

## Read more

* [VisionResult](vision-result.md)
* [VisionPoseEstimate](vision-pose-estimate.md)
