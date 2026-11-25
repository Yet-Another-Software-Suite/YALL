---
icon: video
---

# SimulatedCamera

In short: the pretend version of one camera, used when the robot program runs in simulation.

**Package:** `yall.sim`

```java
public class SimulatedCamera implements AutoCloseable
```

The simulation of one [`VisionCamera`](../yall/vision-camera.md), whatever its vendor: a PhotonVision camera simulation in a [`VisionSim`](vision-sim.md) world, and PhotonVision's pose estimator for its results. Cameras make one for themselves in simulation; get it with `camera.getSimulation()`, which is empty on a real robot.

```java
camera.getSimulation()
    .ifPresent(sim -> sim.getCameraSim().enableDrawWireframe(true));
```

***

## Making one

Vendor wrappers call this; robot code doesn't need to.

```java
static SimulatedCamera create(
    String name, VisionCameraConfig config, CameraModel defaultModel, PhotonCamera camera)
```

| Parameter | Description |
| --- | --- |
| `name` | Name of the camera. |
| `config` | The camera's config: its camera properties or model, camera offset, field, pose estimation mode, world and video streams. |
| `defaultModel` | [`CameraModel`](camera-model.md) to simulate when the config sets no camera properties. |
| `camera` | PhotonVision camera to simulate, or `null` to make a private one named `YALL-<name>`. [`PhotonVisionWrapper`](../cameras/photonvision-wrapper.md) passes its own camera, so code reading that camera sees simulated results too. |

Returns the simulation, or `null` when the robot program isn't in simulation or the config turned simulation off.

***

## Methods

| Method | Returns | Description |
| --- | --- | --- |
| `readFrames()` | `List<VisionFrame>` | Reads the simulated frames since the last call, oldest first. |
| `addHeadingData(double timestampSeconds, Rotation3d orientation)` | `void` | Gives the simulated pose estimator the robot's heading, for heading-assisted estimates. [`VisionCamera.setRobotOrientation`](../yall/vision-camera.md) calls this. |
| `setRobotToCamera(Transform3d robotToCamera)` | `void` | Moves the simulated camera on the robot. [`VisionCamera.setRobotToCamera`](../yall/vision-camera.md) calls this. |
| `setPipelineIndex(int index)` | `void` | Sets the simulated pipeline index. The simulation doesn't change with it. |
| `getPipelineIndex()` | `int` | The simulated pipeline index. |
| `getCameraSim()` | `PhotonCameraSim` | PhotonVision's simulated camera, for settings YALL doesn't cover. |
| `getVisionSim()` | [`VisionSim`](vision-sim.md) | The simulated world this camera is in. |
| `getPhotonPoseEstimator()` | `PhotonPoseEstimator` | PhotonVision's pose estimator for the simulated results. |
| `close()` | `void` | Removes the camera from its world and closes it. |

***

## Read more

* [VisionSim](vision-sim.md)
* [CameraModel](camera-model.md)
* [PhotonVisionFrames](../cameras/photonvision-frames.md)
