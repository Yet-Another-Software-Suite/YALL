---
icon: lightbulb
---

# LimelightWrapper

In short: makes a Limelight into a YALL [`VisionCamera`](../yall/vision-camera.md).

**Package:** `yall.cameras`

```java
public class LimelightWrapper extends VisionCamera
```

A Limelight, through LimelightLib 2 (`com.limelightvision`), as a [`VisionCamera`](../yall/vision-camera.md). Needs the LimelightLib 2 vendordep.

```java
VisionCamera camera =
    new LimelightWrapper(
        new Limelight("limelight-front"),
        new VisionCameraConfig()
            .withCameraModel(CameraModel.LIMELIGHT_4)
            .withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED)); // MegaTag2
```

For a Limelight on a SystemCore USB port, use LimelightLib's names: `new Limelight(Limelight.SYSTEMCORE_USB0)`.

***

## Constructor

```java
LimelightWrapper(Limelight limelight, VisionCameraConfig config)
```

| Parameter | Description |
| --- | --- |
| `limelight` | The Limelight. |
| `config` | The camera's config. |

The config's robot-to-camera transform, if set, overrides the camera pose from the Limelight web interface. The config's pipeline index, if set, is applied.

***

## Methods

| Method | Returns | Description |
| --- | --- | --- |
| `getCamera()` | `Limelight` | The Limelight, for settings YALL doesn't cover (LEDs, IMU mode, throttle, tag ID filters, snapshots, rewind). |

Every other method comes from [`VisionCamera`](../yall/vision-camera.md).

***

## Vendor configs

`withVendorConfig(...)` accepts:

| Type | Effect |
| --- | --- |
| `PoseEstimateConfig` | Applied to both MegaTag1 and MegaTag2. Limelight's own rejection and standard deviations replace YALL's [`VisionPoseFilter`](../filter/vision-pose-filter.md) for this camera. |
| `PipelineConfiguration` | Applied as a pipeline override. |

Any other type throws `IllegalArgumentException`.

***

## Behavior

* **Results:** read with `Limelight.readResultsQueue()`, so every frame is read once. Don't read any of the Limelight's queues elsewhere.
* **Targets:** fiducial, detector and retroreflective targets, in that order. Yaw is Limelight's per-target `tx` (positive right). Pitch is the negative of its per-target `ty`, which is positive down, so pitch is positive up as in PhotonVision. Fiducials include their camera-to-tag transform and ambiguity.
* **Timestamps:** the NetworkTables receive time minus capture and targeting latency.
* **Pose estimates:** MegaTag1 (`TAGS_ONLY`) or MegaTag2 (`HEADING_ASSISTED`), in WPILib blue-origin coordinates. A single-tag estimate carries that tag's ambiguity.
* **Robot orientation:** `setRobotOrientation` sends yaw, yaw rate, pitch and roll to the Limelight and flushes NetworkTables.
* **Pipeline and connection:** `Limelight.setPipelineIndex`, `getCurrentPipelineIndex` and `isConnected`.
* **Default camera model:** [`CameraModel.LIMELIGHT_4`](../sim/camera-model.md). Set the model for other Limelights.

***

## Read more

* [VisionCamera](../yall/vision-camera.md)
* [PoseEstimationMode](../yall/pose-estimation-mode.md)
* [PhotonVisionWrapper](photonvision-wrapper.md)
