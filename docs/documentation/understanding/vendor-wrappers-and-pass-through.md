---
icon: plug
---

# Vendor Wrappers & Pass-Through

A **wrapper** is a class that sits around another one and gives it a new, common shape, like a universal phone case. YALL's wrappers sit around each camera maker's own camera class so they all work the same way. **Pass-through** means you can still reach the maker's own class inside.

## One base class, three wrappers

[`VisionCamera`](https://yall.yassrobotics.com/api-reference/java-reference/yall/vision-camera) holds everything the vendors share: reading frames, the pose filter, robot orientation, the camera offset, pipelines, telemetry and simulation. Each wrapper fills in how its vendor reads frames and applies settings:

| Wrapper | Wraps | `getCamera()` returns |
| --- | --- | --- |
| [`PhotonVisionWrapper`](https://yall.yassrobotics.com/api-reference/java-reference/cameras/photonvision-wrapper) | PhotonLib's `PhotonCamera` | `PhotonCamera` |
| [`LimelightWrapper`](https://yall.yassrobotics.com/api-reference/java-reference/cameras/limelight-wrapper) | LimelightLib 2's `Limelight` | `Limelight` |
| [`SynapseWrapper`](https://yall.yassrobotics.com/api-reference/java-reference/cameras/synapse-wrapper) | SynapseLib's `SynapseCamera` | `SynapseCamera` |

The camera's name is the vendor object's name: the PhotonVision camera name, the Limelight hostname, or the Synapse camera name.

## What each wrapper does with the config

| Config | PhotonVision | Limelight | Synapse |
| --- | --- | --- | --- |
| `withRobotToCamera` | PhotonVision's pose estimator | Overrides the camera pose from the web interface | Moves Synapse's camera pose to the robot |
| `withPipelineIndex` | `setPipelineIndex` | `setPipelineIndex` | `setPipeline` |
| `withPoseEstimationMode` | multi-tag or trig solve | MegaTag1 or MegaTag2 | `TAGS_ONLY` only |
| `withCameraModel`, `withCameraProperties` | simulation | simulation | simulation, and turning pixels into angles |
| `withVendorConfig` | none accepted | `PoseEstimateConfig`, `PipelineConfiguration` | none accepted |

If you leave out `withRobotToCamera`, a Limelight keeps the camera pose from its web interface, and the other cameras (and every simulated camera) assume the camera is at the robot's center.

`setRobotOrientation` sends the robot's orientation on the field to a Limelight for MegaTag2. Every camera also gives it to PhotonVision's pose estimator, which the PhotonVision camera and every simulated camera use.

## Real or simulated

A camera simulates when the robot program runs in simulation and its config hasn't turned simulation off (`withSimulation(false)`). `isSimulated()` says which:

* **On the robot**, `update()` reads the vendor library.
* **In simulation**, `update()` reads a PhotonVision camera simulation. For a PhotonVision camera, that is the team's own `PhotonCamera`. For a Limelight or Synapse camera, it's a private PhotonVision camera named `YALL-<camera name>`. Vendor settings still go to the vendor object, which has no camera behind it in simulation.

In simulation, `isConnected()` is always true, and `setPipelineIndex` only stores the index.

## Pass-through

YALL doesn't try to cover every vendor feature. `getCamera()` and `withVendorConfig(...)` reach the rest, the way YAMS's `getMotorController()` and `withVendorConfig(...)` do for motor controllers. See [How do I use vendor-specific settings?](../how-to-guides/how-do-i-use-vendor-specific-settings.md)

## Closing a camera

`close()` removes the camera from simulation and closes its telemetry. [`VisionSystem.close()`](https://yall.yassrobotics.com/api-reference/java-reference/yall/vision-system) closes every camera in it. Tests that create cameras should close them.

## Read more

* [Results Model](results-model.md)
* [Simulating Cameras](../tutorials/simulating-cameras.md)
