# Overview

**Root package:** `yall`

```java
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.PhotonVisionWrapper;
```

## Package Layout

| Package | Contents |
| --- | --- |
| `yall` | [`VisionCamera`](yall/vision-camera.md), [`VisionCameraConfig`](yall/vision-camera-config.md), [`VisionSystem`](yall/vision-system.md), [`PoseEstimationMode`](yall/pose-estimation-mode.md) |
| `yall.cameras` | The vendor wrappers: [`PhotonVisionWrapper`](cameras/photonvision-wrapper.md), [`LimelightWrapper`](cameras/limelight-wrapper.md), [`SynapseWrapper`](cameras/synapse-wrapper.md) |
| `yall.filter` | [`VisionPoseFilter`](filter/vision-pose-filter.md) |
| `yall.results` | [`VisionResult`](results/vision-result.md), [`VisionTarget`](results/vision-target.md), [`VisionPoseEstimate`](results/vision-pose-estimate.md) |
| `yall.sim` | [`VisionSim`](sim/vision-sim.md), [`CameraModel`](sim/camera-model.md) |

## Where to Start

Every use of YALL begins with one wrapped camera per physical camera:

```java
VisionCamera camera =
    new PhotonVisionWrapper(  // or LimelightWrapper / SynapseWrapper
        new PhotonCamera("front"),
        new VisionCameraConfig()
            .withCameraModel(CameraModel.ARDUCAM_OV9281)
            .withRobotToCamera(Inches.of(12), Inches.zero(), Inches.of(10),
                               Degrees.zero(), Degrees.of(-15), Degrees.zero()));
```

From there:

* [`update()`](yall/vision-camera.md#updating) reads new frames once per loop.
* [`getTargets()`](yall/vision-camera.md#targets) and [`getBestTarget()`](yall/vision-camera.md#targets) return [`VisionTarget`](results/vision-target.md)s.
* [`getEstimatedPose()`](yall/vision-camera.md#pose-estimates) and [`addVisionMeasurements(poseEstimator)`](yall/vision-camera.md#pose-estimates) give [`VisionPoseEstimate`](results/vision-pose-estimate.md)s that passed the [`VisionPoseFilter`](filter/vision-pose-filter.md).
* [`VisionSystem`](yall/vision-system.md) does the same for several cameras.
* [`VisionSim.getDefault().update(truePose)`](sim/vision-sim.md) runs the simulation.
* [`getCamera()`](yall/vision-camera.md#pass-through) returns the vendor's own camera object.

## Reference

* yall
  * [VisionCamera](yall/vision-camera.md)
  * [VisionCameraConfig](yall/vision-camera-config.md)
  * [VisionSystem](yall/vision-system.md)
  * [PoseEstimationMode](yall/pose-estimation-mode.md)
* yall.cameras
  * [PhotonVisionWrapper](cameras/photonvision-wrapper.md)
  * [LimelightWrapper](cameras/limelight-wrapper.md)
  * [SynapseWrapper](cameras/synapse-wrapper.md)
* yall.filter
  * [VisionPoseFilter](filter/vision-pose-filter.md)
* yall.results
  * [VisionResult](results/vision-result.md)
  * [VisionTarget](results/vision-target.md)
  * [VisionPoseEstimate](results/vision-pose-estimate.md)
* yall.sim
  * [VisionSim](sim/vision-sim.md)
  * [CameraModel](sim/camera-model.md)
