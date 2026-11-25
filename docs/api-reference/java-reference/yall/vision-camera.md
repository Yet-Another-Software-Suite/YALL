---
icon: camera
---

# VisionCamera

In short: one camera, from any maker. Call `update()` every loop, then read targets and pose guesses.

**Package:** `yall`

```java
public abstract class VisionCamera implements AutoCloseable
```

A camera from any supported vendor, with one API for targets and robot pose estimates. On a real robot it reads the vendor's library. In simulation it reads a PhotonVision camera simulation in a [`VisionSim`](../sim/vision-sim.md) world, which the camera makes for itself.

Create one with the wrapper for the camera's vendor: [`PhotonVisionWrapper`](../cameras/photonvision-wrapper.md), [`LimelightWrapper`](../cameras/limelight-wrapper.md) or [`SynapseWrapper`](../cameras/synapse-wrapper.md). Call `update()` once per loop (or [`VisionSystem.update()`](vision-system.md) for several cameras), then read the getters. They all report what the last `update()` read.

```java
VisionCamera front = new PhotonVisionWrapper(new PhotonCamera("front"), config);

// In Robot.robotPeriodic() (Commands v2 or v3):
public void robotPeriodic() {
  front.setRobotOrientation(gyro.getRotation2d());
  front.update();
  front.addVisionMeasurements(poseEstimator);
}
```

***

## Updating

| Method | Returns | Description |
| --- | --- | --- |
| `update()` | `void` | Reads new frames from the camera (or its simulation), filters their pose estimates with the config's [`VisionPoseFilter`](../filter/vision-pose-filter.md), counts the filter's decisions in the [`PoseFilterStats`](../filter/pose-filter-stats.md) and publishes telemetry. Call once per loop, before the getters. |
| `setRobotOrientation(Rotation3d orientation, AngularVelocity yawRate)` | `void` | Gives the camera the robot's orientation from the gyro (blue-origin, counterclockwise positive yaw). [`PoseEstimationMode.HEADING_ASSISTED`](pose-estimation-mode.md) needs it every loop, and the pose filter's yaw rate check uses the yaw rate. |
| `setRobotOrientation(Rotation2d heading)` | `void` | Gives the camera the robot's heading without a yaw rate. The yaw rate check is skipped. |

On a Limelight, the orientation is also sent to the camera for MegaTag2. In simulation and on PhotonVision cameras, it is added to PhotonVision's pose estimator as heading data.

***

## Camera position

| Method | Returns | Description |
| --- | --- | --- |
| `setRobotToCamera(Transform3d robotToCamera)` | `void` | Moves the camera on the robot, for a camera on a turret or other moving mechanism. Updates the pose estimator, the simulation and the vendor's camera. |
| `setRobotToCamera(Distance forward, Distance left, Distance up, Angle roll, Angle pitch, Angle yaw)` | `void` | The same, from measurements. See [`VisionCameraConfig.withRobotToCamera`](vision-camera-config.md#camera-position). |
| `getRobotToCamera()` | `Transform3d` | Transform from the robot origin to the camera. `Transform3d.ZERO` if the config didn't set one. |
| `getCameraFieldPose()` | `Optional<Pose3d>` | Where the camera itself is on the field, from the AprilTags in the newest frame alone: the closest solved tag's field pose, moved back along the camera-to-tag transform. It doesn't use the robot pose or the camera offset, so it can measure them. Empty if the newest frame has no solved AprilTag on the field. |
| `getCameraYawOnRobot(Rotation2d robotHeading)` | `Optional<Rotation2d>` | Which way the camera points on the robot, from what it sees: the camera's field heading minus the robot's heading, counterclockwise positive. For a camera on a turret, this is the turret's angle (plus any angle the camera is mounted at on the turret). |

See [How do I measure a turret's angle with a camera?](https://yall.yassrobotics.com/documentation/how-to-guides/how-do-i-measure-a-turrets-angle-with-a-camera)

***

## Targets

| Method | Returns | Description |
| --- | --- | --- |
| `getUnreadResults()` | `List<VisionResult>` | The frames read by the last `update()`, oldest first. |
| `getLatestResult()` | `Optional<VisionResult>` | The newest frame the camera has produced, even if the last `update()` read none. Empty if there hasn't been one. |
| `hasTargets()` | `boolean` | Whether the newest frame has targets. |
| `getTargets()` | `List<VisionTarget>` | The targets in the newest frame, best first. |
| `getBestTarget()` | `Optional<VisionTarget>` | The best target in the newest frame. |

See [`VisionResult`](../results/vision-result.md) and [`VisionTarget`](../results/vision-target.md).

***

## Pose estimates

| Method | Returns | Description |
| --- | --- | --- |
| `getEstimatedPose()` | `Optional<VisionPoseEstimate>` | The newest estimate from the last `update()` that passed the pose filter, with its timestamp and standard deviations. Empty if none passed. |
| `getEstimatedPoses()` | `List<VisionPoseEstimate>` | Every estimate from the last `update()` that passed the pose filter, oldest first. |
| `getRejectedPoses()` | `List<VisionPoseEstimate>` | The estimates from the last `update()` that the pose filter rejected. |
| `getLastRejectionReason()` | `String` | Why the pose filter last rejected an estimate, for example `"ambiguous"` or `"tags too far"`. Empty if it hasn't rejected any. |
| `forEachEstimatedPose(Consumer<VisionPoseEstimate> consumer)` | `void` | Passes every accepted estimate from the last `update()` to a consumer, oldest first. |
| `addVisionMeasurements(PoseEstimator<?> poseEstimator)` | `void` | Adds every accepted estimate from the last `update()` to a WPILib pose estimator, oldest first, with its timestamp and standard deviations. |
| `getFilterStats()` | [`PoseFilterStats`](../filter/pose-filter-stats.md) | What the pose filter has accepted and rejected since the stats were last reset, and how spread out the accepted poses are. |
| `resetFilterStats()` | `void` | Starts the pose filter stats counting again. |

See [`VisionPoseEstimate`](../results/vision-pose-estimate.md).

***

## Pipeline and state

| Method | Returns | Description |
| --- | --- | --- |
| `setPipelineIndex(int index)` | `void` | Switches the camera's pipeline. In simulation, only the index is stored. |
| `getPipelineIndex()` | `int` | The camera's current pipeline. |
| `isConnected()` | `boolean` | Whether the camera is sending data. Simulated cameras are always connected. |
| `isSimulated()` | `boolean` | Whether this camera is giving simulated results: true in simulation with simulation enabled in the config. |
| `getName()` | `String` | Name of the camera. |
| `getConfig()` | [`VisionCameraConfig`](vision-camera-config.md) | The camera's config. |

***

## Pass-through

| Method | Returns | Description |
| --- | --- | --- |
| `getCamera()` | `Object` | The vendor's camera object, for settings YALL doesn't cover. Each wrapper narrows the return type: `PhotonCamera`, `Limelight` or `SynapseCamera`. |
| `getCameraSim()` | `Optional<PhotonCameraSim>` | The camera's PhotonVision simulation, for settings YALL doesn't cover. Empty when not simulating. |
| `getVisionSim()` | `Optional<VisionSim>` | The simulated world the camera is in. Empty when not simulating. |
| `getSimulatedPoseEstimator()` | `Optional<PhotonPoseEstimator>` | PhotonVision's pose estimator for the simulated results. Empty when not simulating. |
| `close()` | `void` | Removes the camera from simulation and closes its telemetry. |

***

## Telemetry

With telemetry on (the default), each `update()` publishes to `YALL/<camera name>` in NetworkTables. What it publishes depends on the config's [`VisionTelemetryConfig`](../telemetry/vision-telemetry-config.md):

| Key | Type | Verbosity | Description |
| --- | --- | --- | --- |
| `connected` | `boolean` | `LOW` | Whether the camera is sending data. |
| `hasTargets` | `boolean` | `LOW` | Whether the newest frame has targets. |
| `targetCount` | `int` | `LOW` | Targets in the newest frame. |
| `acceptedPoses` | `Pose3d[]` | `LOW` | Estimates that passed the pose filter. |
| `rejectedPoses` | `Pose3d[]` | `MID` | Estimates the pose filter rejected. |
| `rejectionReason` | `String` | `MID` | The last rejection reason. |
| `latencyMillis` | `double` | `MID` | Latency of the newest frame. |
| `stats/...` | | `HIGH` | The [`PoseFilterStats`](../filter/pose-filter-stats.md#networktables). |
| `targets/...` | `long[]`, `double[]` | `HIGH` | Every target's `fiducialIds`, `yawDegrees`, `pitchDegrees` and `areaPercent`. |

***

## Writing a wrapper

These are for vendor wrappers, not robot code.

```java
protected VisionCamera(String name, VisionCameraConfig config, PhotonCameraSim cameraSim)
```

Sets up the parts every camera shares. `cameraSim` comes from [`VisionSim.createCameraSim(name, config, defaultModel, photonCameraOrNull)`](../sim/vision-sim.md), which returns `null` when not simulating. Each wrapper passes its default [`CameraModel`](../sim/camera-model.md) there.

| Member | Description |
| --- | --- |
| `protected final String name` | Name of the camera. |
| `protected final VisionCameraConfig config` | The camera's config. |
| `protected abstract List<VisionFrame> readFrames()` | Reads the frames the real camera produced since the last call, oldest first, as [`VisionFrame`](vision-frame.md)s. |
| `protected abstract void applyPipelineIndex(int index)` | Switches the real camera's pipeline. |
| `protected abstract int readPipelineIndex()` | Reads the real camera's pipeline. |
| `protected abstract boolean readConnected()` | Whether the real camera is sending data. |
| `protected void applyRobotOrientation(double timestampSeconds, Rotation3d orientation, double yawRateDegreesPerSecond)` | Passes the robot orientation to the real camera. Does nothing unless overridden. `yawRateDegreesPerSecond` is NaN if unknown. |
| `protected void applyRobotToCamera(Transform3d robotToCamera)` | Passes a new camera position to the real camera. Does nothing unless overridden. |

A PhotonVision-based wrapper can read frames with [`PhotonVisionFrames.read(...)`](../cameras/photonvision-frames.md).

***

## Read more

* [VisionCameraConfig](vision-camera-config.md)
* [VisionSystem](vision-system.md)
* [VisionPoseFilter](../filter/vision-pose-filter.md)
* [PoseFilterStats](../filter/pose-filter-stats.md)
