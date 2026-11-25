# SynapseWrapper

**Package:** `yall.cameras`

```java
public class SynapseWrapper extends VisionCamera
```

A Synapse camera as a [`VisionCamera`](../yall/vision-camera.md). Needs the SynapseLib vendordep.

```java
VisionCamera camera =
    new SynapseWrapper(
        new SynapseCamera("side"),
        new VisionCameraConfig().withCameraModel(CameraModel.ARDUCAM_OV9281));
```

***

## Constructor

```java
SynapseWrapper(SynapseCamera camera, VisionCameraConfig config)
```

| Parameter | Description |
| --- | --- |
| `camera` | The Synapse camera. |
| `config` | The camera's config. |

Throws `IllegalArgumentException` if the config has a vendor config: Synapse takes none. Use `getCamera()` for Synapse settings. The constructor sets the Synapse camera's robot-to-camera offset from the config.

***

## Methods

| Method | Returns | Description |
| --- | --- | --- |
| `getCamera()` | `SynapseCamera` | The Synapse camera. |
| `close()` | `void` | Closes the wrapper's results subscriber, then everything [`VisionCamera.close()`](../yall/vision-camera.md#pass-through) closes. |

Every other method comes from [`VisionCamera`](../yall/vision-camera.md).

***

## Behavior

* **Results:** read from the camera's `data/results` topic with the wrapper's own subscriber, so every frame is read once with its arrival time. `ApriltagPipeline` and `ColorPipeline` results are read; others are skipped.
* **Angles:** Synapse reports target positions in pixels. The wrapper turns them into yaw (positive right) and pitch (positive up) with the config's camera properties, so set them to match the real camera with `withCameraModel` or `withCameraProperties`.
* **Missing values:** Synapse doesn't report target area or AprilTag ambiguity, so those are `NaN`, and the pose filter's ambiguity check is skipped.
* **Timestamps:** the arrival time minus Synapse's capture and process latencies.
* **Pose estimates:** Synapse's field-space camera pose, moved back by the robot-to-camera transform. Tag distances come from the field's tag poses. `HEADING_ASSISTED` works like `TAGS_ONLY`.
* **Pipeline and connection:** `SynapseCamera.setPipeline` and `getPipeline`; connected while the results topic exists.
* **Default camera model:** [`CameraModel.ARDUCAM_OV9281`](../sim/camera-model.md).

***

## See Also

* [VisionCamera](../yall/vision-camera.md)
* [CameraModel](../sim/camera-model.md)
* [PhotonVisionWrapper](photonvision-wrapper.md)
