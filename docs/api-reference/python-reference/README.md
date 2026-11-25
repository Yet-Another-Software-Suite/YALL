---
icon: python
---

# Overview

In short: the older, Limelight-only Python library, not the camera API on this site.

**Package:** `yall`

{% hint style="warning" %}
The Python port is the older, Limelight-only YALL. It hasn't changed for the 2027 library: the camera API in the [Java reference](../java-reference/README.md) ([`VisionCamera`](../java-reference/yall/vision-camera.md), the PhotonVision, Limelight and Synapse wrappers, pose filtering, fusion and simulation) is Java-only. The Python port still reads a Limelight's NetworkTables directly, as the old Java `limelight` package did.
{% endhint %}

YALL ships a Python port of its older, Limelight-only Java library (the `limelight` package), with the same class names and most of the same method names, built on `ntcore` directly (no WPILib Python `Limelight`-specific dependency beyond networktables/geometry).

```python
from yall.limelight import Limelight, EstimationMode
from yall.networktables.geometry import Orientation3d, AngularVelocity3d

limelight = Limelight("limelight")
limelight.getSettings().withLimelightLEDMode(LEDMode.PipelineControl).withCameraToRobot(Pose3d())
```

## Module Layout

| Module                          | Contents                                                                 |
| ---------------------------------- | ------------------------------------------------------------------------- |
| `yall.limelight`                    | `Limelight`, `LimelightSettings`, `LimelightData`, `LimelightPipelineData`, `LimelightTargetData`, `LimelightResults`, `LimelightPoseEstimator`, `PoseEstimate`, `BotPose`, `EstimationMode` |
| `yall.results`                      | `RawFiducial`, `RawDetection`                                              |
| `yall.networktables.geometry`       | `Orientation3d`, `AngularVelocity3d`                                       |
| `yall.networktables.targets`       | `RetroreflectiveTape`, `Barcode`, `AprilTagFiducial`, `NeuralClassifier`, `NeuralDetector` |
| `yall.networktables.util`           | Array/pose conversion helpers, equivalent to Java's `LimelightUtils`       |
| `yall.deprecated`                   | Legacy compatibility shims                                                 |

## Differences from the old Java library

The Python port is a direct translation of an earlier revision of the Java library and has not yet caught up to every 2026 addition. Known differences:

* `createPoseEstimator(megatag2: bool)` takes a `bool` instead of Java's `EstimationMode` enum.
* `LimelightSettings.withCameraToRobot(pose)` is the Python equivalent of Java's `withCameraOffset(Pose3d)`.
* `withArilTagIdFilter(...)`: the spelling mistake comes from an earlier version of the API. It matches Java's `withAprilTagIdFilter`.
* No Python equivalents yet for `IMUResults`, `HardwareReport`, `HailoStats`, `RewindStats`, or rewind-capture / USB port-forwarding helpers.

For the exact signatures, see `yall/python/yall/` in the YALL repository. The Java reference covers the 2027 camera API, which the Python port doesn't have.

## Read more

* [Java API](../java-reference/README.md) (the 2027 camera API)
