# VisionCameraConfig

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
| Telemetry | On |

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
| `withCameraProperties(int widthPixels, int heightPixels, Rotation2d diagonalFov, double fps)` | Sets the resolution, diagonal field of view and frame rate, with a 25 ± 5 ms latency and 0.25 ± 0.08 px calibration error. |
| `withSimulation(boolean enabled)` | Sets whether the camera gives simulated results when the robot program runs in simulation. |
| `withSimulation(VisionSim sim)` | Sets the simulated world the camera belongs to, and turns simulation on. |
| `withSimulatedVideoStreams(boolean enabled)` | Sets whether the simulated camera streams rendered raw and processed video. The streams cost CPU. |

***

## Vendor config and telemetry

| Method | Description |
| --- | --- |
| `withVendorConfig(Object vendorConfig)` | Passes a vendor config object to the camera, as YAMS does for motor controllers. Each wrapper documents the types it accepts and throws `IllegalArgumentException` for others. Only [`LimelightWrapper`](../cameras/limelight-wrapper.md#vendor-configs) accepts any. |
| `withTelemetry(boolean enabled)` | Sets whether the camera publishes its targets and pose estimates to NetworkTables under `YALL/<camera name>`. |

***

## Getters

| Method | Returns | Description |
| --- | --- | --- |
| `getRobotToCamera()` | `Optional<Transform3d>` | The robot-to-camera transform, if set. |
| `getField()` | `Field` | The configured field, or `Fields.DEFAULT_FIELD`. |
| `getPoseEstimationMode()` | `PoseEstimationMode` | The pose estimation mode. |
| `getPoseFilter()` | `VisionPoseFilter` | The pose filter. |
| `getPipelineIndex()` | `Optional<Integer>` | The pipeline to switch to on creation, if set. |
| `getCameraProperties()` | `Optional<SimCameraProperties>` | The camera properties, if set. |
| `isSimulationEnabled()` | `boolean` | Whether the camera is simulated in simulation. |
| `getVisionSim()` | `Optional<VisionSim>` | The simulated world, if set. |
| `isSimulatedVideoStreamsEnabled()` | `boolean` | Whether the simulated camera streams video. |
| `getVendorConfig()` | `Optional<Object>` | The vendor config object, if set. |
| `isTelemetryEnabled()` | `boolean` | Whether the camera publishes telemetry. |

***

## See Also

* [VisionCamera](vision-camera.md)
* [VisionPoseFilter](../filter/vision-pose-filter.md)
* [CameraModel](../sim/camera-model.md)
