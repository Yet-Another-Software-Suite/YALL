# VisionCamera

**Package:** `yall`

```java
public abstract class VisionCamera implements AutoCloseable
```

A camera from any supported vendor, with one API for targets and robot pose estimates. On a real robot it reads the vendor's library. In simulation it reads a PhotonVision camera simulation, so the same robot code runs in both.

Create one with the wrapper for the camera's vendor: [`PhotonVisionWrapper`](../cameras/photonvision-wrapper.md), [`LimelightWrapper`](../cameras/limelight-wrapper.md) or [`SynapseWrapper`](../cameras/synapse-wrapper.md). Call `update()` once per loop (or [`VisionSystem.update()`](vision-system.md) for several cameras), then read the getters. They all report what the last `update()` read.

```java
VisionCamera front = new PhotonVisionWrapper(new PhotonCamera("front"), config);

public void periodic() {
  front.setRobotOrientation(gyro.getRotation2d());
  front.update();
  front.addVisionMeasurements(poseEstimator);
}
```

***

## Updating

| Method | Returns | Description |
| --- | --- | --- |
| `update()` | `void` | Reads new frames from the camera (or its simulation), filters their pose estimates with the config's [`VisionPoseFilter`](../filter/vision-pose-filter.md) and publishes telemetry. Call once per loop, before the getters. |
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
| `getConfig()` | `VisionCameraConfig` | The camera's config. |

***

## Pass-through

| Method | Returns | Description |
| --- | --- | --- |
| `getCamera()` | `Object` | The vendor's camera object, for settings YALL doesn't cover. Each wrapper narrows the return type: `PhotonCamera`, `Limelight` or `SynapseCamera`. |
| `getCameraSim()` | `Optional<PhotonCameraSim>` | The PhotonVision simulation of this camera. Empty when not simulating. |
| `getVisionSim()` | `Optional<VisionSim>` | The simulated world this camera is in. Empty when not simulating. |
| `getPhotonPoseEstimator()` | `PhotonPoseEstimator` | The PhotonVision pose estimator this camera uses in simulation (and on PhotonVision cameras), for settings YALL doesn't cover. |
| `close()` | `void` | Removes the camera from simulation and closes its telemetry. |

***

## Telemetry

With telemetry on (the default), each `update()` publishes to `YALL/<camera name>` in NetworkTables:

| Key | Type | Description |
| --- | --- | --- |
| `acceptedPoses` | `Pose3d[]` | Estimates that passed the pose filter. |
| `rejectedPoses` | `Pose3d[]` | Estimates the pose filter rejected. |
| `rejectionReason` | `String` | The last rejection reason. |
| `hasTargets` | `boolean` | Whether the newest frame has targets. |
| `targetCount` | `int` | Targets in the newest frame. |
| `latencyMillis` | `double` | Latency of the newest frame. |
| `connected` | `boolean` | Whether the camera is sending data. |

***

## Writing a wrapper

These are for vendor wrappers, not robot code.

```java
protected VisionCamera(String name, VisionCameraConfig config, PhotonCamera cameraForSimulation)
```

Sets up the pose filter, PhotonVision's pose estimator and, in simulation, the simulated camera. `cameraForSimulation` is the PhotonVision camera the simulation publishes to, or `null` to create a private one.

| Member | Description |
| --- | --- |
| `protected final String name` | Name of the camera. |
| `protected final VisionCameraConfig config` | The camera's config. |
| `protected abstract List<Frame> readFrames()` | Reads the frames the real camera produced since the last call, oldest first. |
| `protected abstract void applyPipelineIndex(int index)` | Switches the real camera's pipeline. |
| `protected abstract int readPipelineIndex()` | Reads the real camera's pipeline. |
| `protected abstract boolean readConnected()` | Whether the real camera is sending data. |
| `protected abstract SimCameraProperties defaultCameraProperties()` | Camera properties to simulate when the config doesn't set any. Called from the constructor, so it must not use the subclass's fields. |
| `protected void applyRobotOrientation(Rotation3d orientation, double yawRateDegreesPerSecond)` | Passes the robot orientation to the real camera. Does nothing unless overridden. `yawRateDegreesPerSecond` is NaN if unknown. |
| `protected void applyRobotToCamera(Transform3d robotToCamera)` | Passes a new camera position to the real camera. Does nothing unless overridden. |
| `protected final List<Frame> readPhotonFrames(PhotonCamera camera)` | Reads and converts a PhotonVision camera's unread results, with pose estimates. PhotonVision cameras use it on real robots, and every camera uses it in simulation. |

### VisionCamera.Frame

```java
public record Frame(
    VisionResult result,
    Optional<VisionPoseEstimate> estimate,
    Optional<String> vendorRejection,
    Optional<Matrix<N3, N1>> vendorStdDevs)
```

One frame read from the camera, before the pose filter.

| Component | Description |
| --- | --- |
| `result` | The frame's targets. |
| `estimate` | The robot pose estimated from the frame, if any. |
| `vendorRejection` | Why the vendor's own filter rejected the estimate, if it did. It replaces the pose filter's checks. |
| `vendorStdDevs` | Standard deviations from the vendor, used instead of the filter's. |

`public Frame(VisionResult result, Optional<VisionPoseEstimate> estimate)` creates a frame with no vendor filtering.

***

## See Also

* [VisionCameraConfig](vision-camera-config.md)
* [VisionSystem](vision-system.md)
* [VisionPoseFilter](../filter/vision-pose-filter.md)
