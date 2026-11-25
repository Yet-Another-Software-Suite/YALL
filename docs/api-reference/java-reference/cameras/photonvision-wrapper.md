---
icon: plug
---

# PhotonVisionWrapper

In short: makes a PhotonVision camera into a YALL [`VisionCamera`](../yall/vision-camera.md).

**Package:** `yall.cameras`

```java
public class PhotonVisionWrapper extends VisionCamera
```

A PhotonVision camera as a [`VisionCamera`](../yall/vision-camera.md). Needs PhotonLib, which YALL requires anyway.

```java
VisionCamera camera =
    new PhotonVisionWrapper(
        new PhotonCamera("front"),
        new VisionCameraConfig().withCameraModel(CameraModel.LUMA_P1));
```

***

## Constructor

```java
PhotonVisionWrapper(PhotonCamera camera, VisionCameraConfig config)
```

| Parameter | Description |
| --- | --- |
| `camera` | The PhotonVision camera. |
| `config` | The camera's config. |

Throws `IllegalArgumentException` if the config has a vendor config: PhotonVision takes none. Use `getCamera()` for PhotonVision settings.

***

## Methods

| Method | Returns | Description |
| --- | --- | --- |
| `getCamera()` | `PhotonCamera` | The PhotonVision camera. |

Every other method comes from [`VisionCamera`](../yall/vision-camera.md).

***

## Behavior

* **Results:** read with `PhotonCamera.getAllUnreadResults()`, which hands each result out only once. Let the wrapper read them; read results from the wrapper, not from `getCamera()`.
* **Pose estimates:** from PhotonVision's `PhotonPoseEstimator`: the coprocessor's multi-tag result, then the lowest-ambiguity tag. With `HEADING_ASSISTED`, the PnP distance trig solve comes first.
* **Simulation:** PhotonVision's simulation publishes to this same `PhotonCamera`, so code that uses `getCamera()` directly sees simulated results too.
* **Pipeline and connection:** `PhotonCamera.setPipelineIndex`, `getPipelineIndex` and `isConnected`.
* **Default camera model:** [`CameraModel.ARDUCAM_OV9281`](../sim/camera-model.md).

***

## Read more

* [VisionCamera](../yall/vision-camera.md)
* [LimelightWrapper](limelight-wrapper.md)
* [SynapseWrapper](synapse-wrapper.md)
