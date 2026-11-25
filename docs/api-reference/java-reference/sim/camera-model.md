---
icon: camera-retro
---

# CameraModel

In short: real cameras' published specs, so the simulation sees what your camera would.

**Package:** `yall.sim`

```java
public enum CameraModel
```

Published specs of common FRC vision cameras, for simulating them. Pass one to [`VisionCameraConfig.withCameraModel`](../yall/vision-camera-config.md#camera-properties-and-simulation), so the simulation sees what the real camera would.

```java
new VisionCameraConfig().withCameraModel(CameraModel.LIMELIGHT_3G);
```

***

## Values

| Value | Resolution | Field of view (H × V) | fps | Latency estimate |
| --- | --- | --- | --- | --- |
| `LIMELIGHT_2` | 1280x960 | 62.5° × 48.9° | 6 | 30 ms |
| `LIMELIGHT_2_PLUS` | 1280x960 | 62.5° × 48.9° | 7 | 30 ms |
| `LIMELIGHT_3` | 1280x960 | 62.5° × 48.9° | 20 | 25 ms |
| `LIMELIGHT_3A` | 1280x960 | 54.5° × 42° | 20 | 25 ms |
| `LIMELIGHT_3G` | 1280x800 | 82° × 56.2° | 20 | 25 ms |
| `LIMELIGHT_4` | 1280x800 | 82° × 56.2° | 40 | 20 ms |
| `LUMA_P1` | 1280x800 | 80° × 56° | 60 | 20 ms |
| `LUMA_P2` | 1280x800 | 80° × 56° | 60 | 20 ms |
| `THRIFTYCAM` | 1600x1304 | 60.7° × 51° | 70 | 30 ms |
| `THRIFTIEST_CAM` | 1280x800 | 82° × 56° | 120 | 30 ms |
| `ARDUCAM_OV9281` | 1280x800 | 70° × 47.3° | 100 | 30 ms |
| `ELP_FISHEYE` | 1280x960 | 115° × 99.4° (center of a 180° fisheye) | 20 | 40 ms |

* **Frame rates:** smart cameras (Limelight, Luma) run at the AprilTag frame rate their vendor publishes. USB cameras (ThriftyCam, Thriftiest Cam, Arducam) run at their full-resolution capture rate. A coprocessor may track AprilTags slower; lower the rate with `withCameraProperties` if yours does.
* **Limelight 3G:** Limelight lists its AprilTag rate at 1280x960, which its 1280x800 sensor can't produce, so it is simulated at 1280x800.
* **Luma P1 and P2:** OV9281 cameras running PhotonVision; the P2 has the same camera and performance as the P1. The rate is at 1280x800 with decimate 2.
* **ThriftyCam:** its stock 80° lens is specified as 71° × 48° on a different image size. On the ThriftyCam's OmniVision OG02B1B sensor, the lens's 4.1 mm focal length and 3 µm pixels give 60.7° × 51°, which this simulates. 70 fps needs firmware 5.10 or newer.
* **Arducam OV9281 (B0332):** Arducam gives only the 70° horizontal field of view, so the vertical one comes from the 16:10 sensor. Arducam's manual says 120 fps and resellers say 100, so this uses 100.
* **ELP fisheye (AndyMark am-5170, ELP-USB8MP02G-L180):** a Sony IMX179 color rolling-shutter camera with a 180° fisheye lens. ELP lists 1280x960 at 20 fps (3264x2448 at 15 fps). PhotonVision simulates a pinhole lens, which can't see 180°, so this model keeps the fisheye's magnification at the center: 180° across 1280 pixels is a focal length of about 407 pixels, and a pinhole lens with that focal length sees 115° × 99.4°. Tags near the center look the right size; tags at the edges of the real view aren't simulated.
* **Latency:** no vendor publishes a fixed latency, so every model uses an estimate, with a 5 ms standard deviation. Limelights report their real latency at runtime.

Sources: the Limelight quick-start pages and hardware comparison at https://docs.limelightvision.io/docs/docs-limelight/getting-started/, the Luma P1 docs at https://docs.luma.vision/p1/ and the Luma P2 page at https://luma.vision/products/luma-p2, The Thrifty Bot's docs at https://docs.thethriftybot.com/thriftycam and https://docs.thethriftybot.com/thriftiest-cam, Arducam's B0332 quick start guide, AndyMark's am-5170 page at https://andymark.com/products/elp-fisheye-lens-camera, and ELP's ELP-USB8MP02G-L180 listing.

***

## Methods

| Method | Returns | Description |
| --- | --- | --- |
| `getWidthPixels()` | `int` | Image width simulated. |
| `getHeightPixels()` | `int` | Image height simulated. |
| `getHorizontalFovDegrees()` | `double` | Horizontal field of view, in degrees. |
| `getVerticalFovDegrees()` | `double` | Vertical field of view, in degrees. |
| `getFps()` | `double` | Frames per second simulated. |
| `getAverageLatencyMillis()` | `double` | Average latency simulated, an estimate. |
| `getSimCameraProperties()` | `SimCameraProperties` | New PhotonVision camera properties for this model: a distortion-free lens with the published horizontal and vertical field of view, 0.25 ± 0.08 px of pixel noise, the frame rate, and the estimated latency ± 5 ms. |

Start from a model and change what differs:

```java
var properties = CameraModel.ARDUCAM_OV9281.getSimCameraProperties();
properties.setFPS(30); // what your coprocessor tracks AprilTags at
config.withCameraProperties(properties);
```

***

## Wrapper defaults

Without a model or camera properties in the config, each wrapper simulates:

| Wrapper | Default model |
| --- | --- |
| [`LimelightWrapper`](../cameras/limelight-wrapper.md) | `LIMELIGHT_4` |
| [`PhotonVisionWrapper`](../cameras/photonvision-wrapper.md) | `ARDUCAM_OV9281` |
| [`SynapseWrapper`](../cameras/synapse-wrapper.md) | `ARDUCAM_OV9281` |

***

## Read more

* [VisionSim](vision-sim.md)
* [VisionCameraConfig](../yall/vision-camera-config.md)
