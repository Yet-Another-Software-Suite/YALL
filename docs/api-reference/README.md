---
icon: book-open
---

# Overview

Yet Another Limelight Library (YALL) gives PhotonVision, Limelight and Synapse cameras one Java API for FRC robots. A camera object returns real results on the robot and simulated results in simulation, so the same code runs in both. Every camera is simulated with PhotonVision's camera simulation, whatever its vendor.

YALL builds against WPILib 2027 (alpha 8). This reference covers every public class. For tutorials, how-to guides and explanations, see the Documentation section of this site.

## Features

* **[`VisionCamera`](java-reference/yall/vision-camera.md)**: one camera API for every vendor, through [`PhotonVisionWrapper`](java-reference/cameras/photonvision-wrapper.md), [`LimelightWrapper`](java-reference/cameras/limelight-wrapper.md) and [`SynapseWrapper`](java-reference/cameras/synapse-wrapper.md)
* **[`VisionCameraConfig`](java-reference/yall/vision-camera-config.md)**: camera position, pose estimation mode, pose filter, camera model, simulation, vendor configs and telemetry
* **[`VisionPoseFilter`](java-reference/filter/vision-pose-filter.md)**: pose estimate checks you can each tune or turn off, and standard deviations scaled by tag distance and count
* **[`VisionSystem`](java-reference/yall/vision-system.md)**: several cameras feeding a WPILib pose estimator, or fused into one pose
* **[`VisionSim`](java-reference/sim/vision-sim.md) and [`CameraModel`](java-reference/sim/camera-model.md)**: PhotonVision simulation for every camera, with presets from each camera's published specs
* **Results**: [`VisionResult`](java-reference/results/vision-result.md), [`VisionTarget`](java-reference/results/vision-target.md) and [`VisionPoseEstimate`](java-reference/results/vision-pose-estimate.md), the same for every vendor

## Quick Navigation

* [Java API](java-reference/README.md)
* [Python API](python-reference/README.md) (the older Limelight-only library)

## Supported Hardware

| Vendor | Library | Wrapper | Camera models |
| --- | --- | --- | --- |
| PhotonVision | PhotonLib (required) | `PhotonVisionWrapper` | `LUMA_P1`, `LUMA_P2`, `THRIFTYCAM`, `THRIFTIEST_CAM`, `ARDUCAM_OV9281` |
| Limelight | LimelightLib 2 | `LimelightWrapper` | `LIMELIGHT_2`, `LIMELIGHT_2_PLUS`, `LIMELIGHT_3`, `LIMELIGHT_3A`, `LIMELIGHT_3G`, `LIMELIGHT_4` |
| Synapse | SynapseLib | `SynapseWrapper` | any USB camera, for example `THRIFTYCAM`, `THRIFTIEST_CAM`, `ARDUCAM_OV9281` |

YALL requires PhotonLib and WPILib's CameraServer and AprilTag vendordeps, which PhotonVision's simulation needs. LimelightLib 2 and SynapseLib are needed only for the cameras you construct. Any other camera can be simulated with PhotonVision camera properties.
