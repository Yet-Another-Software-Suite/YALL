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

## Fault injection

Faults make the simulated cameras misbehave like real ones, to test how your robot copes. They apply to every camera in this world, and only in simulation. The random ones take a seed, so a test gives the same result every run.

| Method | Returns | Description |
| --- | --- | --- |
| `injectBlackout(double currentTimeSeconds, Time duration)` | `VisionSim` | Drops every frame from `currentTimeSeconds` for `duration`, as if the cameras lost power or their network link. Times are `Timer.getMonotonicTimestamp()`. A longer overlapping blackout extends it. |
| `isBlackedOut(double nowSeconds)` | `boolean` | Whether a blackout is in effect at `nowSeconds`. |
| `withDropoutProbability(double probability, long seed)` | `VisionSim` | Drops each frame with this probability, from 0 to 1. |
| `withFieldTagJitter(Distance stdDev, long seed)` | `VisionSim` | Moves each simulated pose estimate's x and y by random amounts with this standard deviation, like AprilTags mounted a little off their mapped spots (for example `Millimeters.of(13)`). 0 turns it off. |
| `clearFaults()` | `void` | Ends any blackout and turns dropout and jitter off. |
| `shouldDropFrame(double nowSeconds)` | `boolean` | Whether a frame arriving at `nowSeconds` is dropped. [`VisionCamera.update()`](../yall/vision-camera.md) calls it for each simulated frame. |
| `applyFieldTagJitter(Pose3d pose)` | `Pose3d` | The pose with the jitter added. `VisionCamera.update()` calls it for each simulated pose estimate. |

```java
VisionSim sim = VisionSim.getDefault();
// Lose the cameras for 2 seconds, starting now.
sim.injectBlackout(Timer.getMonotonicTimestamp(), Seconds.of(2));
// Drop 10% of frames, and mount every tag up to about a centimeter off.
sim.withDropoutProbability(0.1, 2027).withFieldTagJitter(Millimeters.of(13), 2027);
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

***

## Making a camera's simulation

Vendor wrappers call these; robot code doesn't need to.

| Method | Returns | Description |
| --- | --- | --- |
| `static createCameraSim(String name, VisionCameraConfig config, CameraModel defaultModel, PhotonCamera camera)` | `PhotonCameraSim` | Makes the PhotonVision simulation of a camera, from the config's camera properties (or `defaultModel`), and adds it to the config's world (or the default one). `camera` is the PhotonVision camera it publishes to, or `null` to make a private one named `YALL-<name>`. Returns `null` when the robot program isn't in simulation or the config turned simulation off. |
| `static privateCameraName(String cameraName)` | `String` | `YALL-<cameraName>`, the name of that private camera. |
