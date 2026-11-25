---
icon: sliders
---

# VisionCameraConfig

In short: the settings for one camera: where it sits, which model it is, and how it filters pose guesses.

**Package:** `yall`

```java
public class VisionCameraConfig
```

Settings shared by every [`VisionCamera`](vision-camera.md), whatever the vendor. Every `withX` method returns `this` for chaining. Pass the config to a wrapper's constructor.

```java
VisionCameraConfig config =
    new VisionCameraConfig()
        .withCameraModel(CameraModel.LIMELIGHT_4)
        .withRobotToCamera(Inches.of(12), Inches.zero(), Inches.of(10),
                           Degrees.zero(), Degrees.of(-15), Degrees.zero())
        .withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED)
        .withPoseFilter(new VisionPoseFilter().withMaxAmbiguity(0.15));
```

Vendor settings YALL doesn't cover go through `withVendorConfig(Object)`, or straight to the vendor object from [`VisionCamera.getCamera()`](vision-camera.md#pass-through).

***

## Constructor

```java
VisionCameraConfig()
```

Creates a config with the defaults below.

***

## Defaults

| Setting | Default |
| --- | --- |
| Robot-to-camera transform | Not set: the camera is treated as at the robot origin, and a Limelight keeps the pose from its web interface. |
| Field | WPILib's `Fields.DEFAULT_FIELD` |
| Pose estimation mode | [`PoseEstimationMode.TAGS_ONLY`](pose-estimation-mode.md) |
| Pose filter | `new VisionPoseFilter()`, with its [default checks](../filter/vision-pose-filter.md#checks-and-defaults) |
| Pipeline | Not set: the camera keeps its current pipeline. |
| Camera properties | Not set: the wrapper's default [`CameraModel`](../sim/camera-model.md) |
| Simulation | On, in [`VisionSim.getDefault()`](../sim/vision-sim.md) |
| Simulated video streams | Off |
| Vendor config | None |
| Telemetry | On: every `MID` field of [`VisionTelemetryConfig`](../telemetry/vision-telemetry-config.md) under `YALL/<camera name>` |

***

## Camera position

| Method | Description |
| --- | --- |
| `withRobotToCamera(Transform3d robotToCamera)` | Sets where the camera is on the robot: the transform from the robot origin to the camera lens. Pose estimates, simulation and Synapse's robot pose need it. On a Limelight it overrides the camera pose set in the web interface. |
| `withRobotToCamera(Distance forward, Distance left, Distance up, Angle roll, Angle pitch, Angle yaw)` | The same, measured from the robot's center on the floor, in the order of Limelight's camera pose. `forward` is negative behind the center, `left` negative to the right, `up` is the height above the floor. `roll` and `yaw` are counterclockwise positive; negative `pitch` tilts the camera up; a `yaw` of 180° faces backward. |

```java
// 12" forward, centered, 10" up, tilted 15° up.
config.withRobotToCamera(Inches.of(12), Inches.zero(), Inches.of(10),
                         Degrees.zero(), Degrees.of(-15), Degrees.zero());
```

***

## Pose estimation

| Method | Description |
| --- | --- |
| `withField(Field field)` | Sets the field, which holds the AprilTag layout. |
| `withPoseEstimationMode(PoseEstimationMode mode)` | Sets how AprilTags become a robot pose. |
| `withPoseFilter(VisionPoseFilter filter)` | Sets the filter that accepts or rejects pose estimates and weighs them. |
| `withPipelineIndex(int index)` | Sets the pipeline to switch to when the camera is created. |

***

## Camera properties and simulation

| Method | Description |
| --- | --- |
| `withCameraModel(CameraModel model)` | Sets the camera's resolution, field of view and frame rate from a camera's published specs. Use the model that matches the real camera, so simulation sees what it would. |
| `withCameraProperties(SimCameraProperties properties)` | Sets the camera's resolution, field of view, frame rate, latency and noise from PhotonVision camera properties. Simulation uses them, and Synapse uses them to turn pixel positions into angles. |
| `withCameraProperties(int widthPixels, int heightPixels, Angle diagonalFov, Frequency frameRate)` | Sets the resolution, diagonal field of view and frame rate, with a 25 ± 5 ms latency and 0.25 ± 0.08 px calibration error. For example `withCameraProperties(1280, 720, Degrees.of(70), Hertz.of(30))`. |
| `withSimulation(boolean enabled)` | Sets whether the camera gives simulated results when the robot program runs in simulation. |
| `withSimulation(VisionSim sim)` | Sets the simulated world the camera belongs to, and turns simulation on. |
| `withSimulatedVideoStreams(boolean enabled)` | Sets whether the simulated camera streams what it would see. Off by default because it costs a lot of CPU. Streams are served by CameraServer as `<name>-raw` and `<name>-processed` for a PhotonVision camera, or `YALL-<camera name>-raw` and `-processed` for others, at `http://localhost:1181`, `1182` and up, one port per stream. |
| `withSimulatedWireframe(boolean enabled)` | Sets whether the simulated streams also draw the field's floor and walls. Off by default because it costs more CPU. Needs `withSimulatedVideoStreams(true)`. |

***

## Vendor config and telemetry

| Method | Description |
| --- | --- |
| `withVendorConfig(Object vendorConfig)` | Passes a vendor config object to the camera, as YAMS does for motor controllers. Each wrapper documents the types it accepts and throws `IllegalArgumentException` for others. Only [`LimelightWrapper`](../cameras/limelight-wrapper.md#vendor-configs) accepts any. |
| `withTelemetry(boolean enabled)` | Sets whether the camera publishes telemetry, with the default [`VisionTelemetryConfig`](../telemetry/vision-telemetry-config.md): every `MID` field under `YALL/<camera name>`. Defaults to true. |
| `withTelemetry(VisionTelemetryConfig telemetryConfig)` | Sets what the camera publishes to NetworkTables, and where. `null` publishes nothing. |

***

## Getters

| Method | Returns | Description |
| --- | --- | --- |
| `getRobotToCamera()` | `Optional<Transform3d>` | The robot-to-camera transform, if set. |
| `getField()` | `Field` | The configured field, or `Fields.DEFAULT_FIELD`. |
| `getPoseEstimationMode()` | [`PoseEstimationMode`](pose-estimation-mode.md) | The pose estimation mode. |
| `getPoseFilter()` | [`VisionPoseFilter`](../filter/vision-pose-filter.md) | The pose filter. |
| `getPipelineIndex()` | `Optional<Integer>` | The pipeline to switch to on creation, if set. |
| `getCameraProperties()` | `Optional<SimCameraProperties>` | The camera properties, if set. |
| `isSimulationEnabled()` | `boolean` | Whether the camera is simulated in simulation. |
| `getVisionSim()` | `Optional<VisionSim>` | The simulated world, if set. |
| `isSimulatedVideoStreamsEnabled()` | `boolean` | Whether the simulated camera streams video. |
| `isSimulatedWireframeEnabled()` | `boolean` | Whether the simulated streams draw the field. |
| `getVendorConfig()` | `Optional<Object>` | The vendor config object, if set. |
| `isTelemetryEnabled()` | `boolean` | Whether the camera publishes telemetry. |
| `getTelemetryConfig()` | `Optional<VisionTelemetryConfig>` | What the camera publishes, or empty to publish nothing. |

***

## Read more

* [VisionCamera](vision-camera.md)
* [VisionPoseFilter](../filter/vision-pose-filter.md)
* [CameraModel](../sim/camera-model.md)
