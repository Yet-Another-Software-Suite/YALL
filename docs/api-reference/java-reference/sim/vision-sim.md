---
icon: computer
---

# VisionSim

In short: the pretend field every simulated camera sees.

**Package:** `yall.sim`

```java
public class VisionSim
```

The simulated world every simulated [`VisionCamera`](../yall/vision-camera.md) sees, whatever its vendor. It is PhotonVision's `VisionSystemSim`: each camera projects the field's AprilTags (and any extra targets) through a simulated lens, with noise and latency.

Cameras join `getDefault()` unless their config names another world with [`withSimulation(VisionSim)`](../yall/vision-camera-config.md#camera-properties-and-simulation). Update the world once per loop with the robot's true pose:

```java
// In Robot.simulationPeriodic(): TimedRobot (Commands v2) and OpModeRobot (Commands v3) both have it.
public void simulationPeriodic() {
  VisionSim.getDefault().update(drivetrainSim.getPose());
}
```

[`VisionSystem.updateSimulation`](../yall/vision-system.md) does this for every world its cameras are in.

***

## Constructor

```java
VisionSim(String name, Field field)
```

| Parameter | Description |
| --- | --- |
| `name` | Name of the world, used for PhotonVision's debug field in NetworkTables. |
| `field` | Field whose AprilTags the cameras see. |

***

## Methods

| Method | Returns | Description |
| --- | --- | --- |
| `static getDefault()` | `VisionSim` | The world cameras join when their config doesn't name one, named `"YALL"`. It holds the AprilTags of WPILib's `Fields.DEFAULT_FIELD`. |
| `static setDefault(VisionSim sim)` | `void` | Replaces the default world, for example to use another field. Call it before creating cameras. |
| `update(Pose2d robotPose)` | `void` | Moves the simulated robot and processes a frame on each camera that is due one. |
| `update(Pose3d robotPose)` | `void` | The same, with a 3D pose. |
| `resetRobotPose(Pose2d robotPose)` | `void` | Moves the simulated robot without processing frames, for example when resetting the robot pose. |
| `addVisionTargets(String type, VisionTargetSim... targets)` | `void` | Adds targets other than the field's AprilTags, such as game pieces for object detection. Give each a class ID with `new VisionTargetSim(pose, model, classId, confidence)`. `type` names the group for the debug field. |
| `removeVisionTargets(String type)` | `void` | Removes a group of targets added with `addVisionTargets`. |
| `addCamera(PhotonCameraSim cameraSim, Transform3d robotToCamera)` | `void` | Adds a simulated camera. [`VisionCamera`](../yall/vision-camera.md) calls this; robot code doesn't need to. |
| `removeCamera(PhotonCameraSim cameraSim)` | `void` | Removes a simulated camera. |
| `adjustCamera(PhotonCameraSim cameraSim, Transform3d robotToCamera)` | `void` | Moves a simulated camera. [`VisionCamera.setRobotToCamera`](../yall/vision-camera.md) calls this. |
| `getDebugField()` | `Field2d` | A field drawing of the simulated robot, cameras and the targets they see. |
| `getField()` | `Field` | The field whose AprilTags the cameras see. |
| `getVisionSystemSim()` | `VisionSystemSim` | The PhotonVision simulation underneath, for settings YALL doesn't cover. |
| `getCameraSim(String name)` | `Optional<PhotonCameraSim>` | Finds a simulated camera by its PhotonVision name. |

```java
// A ball 2 m ahead of the start, for an object detection pipeline with class 0.
VisionSim.getDefault().addVisionTargets(
    "balls",
    new VisionTargetSim(new Pose3d(5, 4, 0.15, Rotation3d.ZERO), new TargetModel(0.3), 0, 0.9f));
```

***

## Simulated cameras

Each simulated [`VisionCamera`](../yall/vision-camera.md) gets a PhotonVision `PhotonCameraSim`:

* A [`PhotonVisionWrapper`](../cameras/photonvision-wrapper.md)'s simulation publishes to its own `PhotonCamera`.
* Other cameras get a private `PhotonCamera` named `YALL-<camera name>`.
* The camera properties come from the config's [`CameraModel`](camera-model.md) or camera properties, or the wrapper's default model.
* Video streams are off unless the config turns them on.

***

## Read more

* [CameraModel](camera-model.md)
* [VisionCamera](../yall/vision-camera.md)
