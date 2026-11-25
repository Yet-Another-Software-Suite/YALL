# Overview

**Package:** `yall`

YALL ships a Python port with the same class names and most of the same method names as the Java library, built on `ntcore` directly (no WPILib Python `Limelight`-specific dependency beyond networktables/geometry). If you know the [Java API](../java-reference/java-reference.md), the Python API will look immediately familiar.

```python
from yall.limelight import Limelight, EstimationMode
from yall.networktables.geometry import Orientation3d, AngularVelocity3d

limelight = Limelight("limelight")
limelight.getSettings().withLimelightLEDMode(LEDMode.PipelineControl).withCameraToRobot(Pose3d())
```

## Module Layout

| Module                        | Contents                                                                                                                                                                                     |
| ----------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `yall.limelight`              | `Limelight`, `LimelightSettings`, `LimelightData`, `LimelightPipelineData`, `LimelightTargetData`, `LimelightResults`, `LimelightPoseEstimator`, `PoseEstimate`, `BotPose`, `EstimationMode` |
| `yall.results`                | `RawFiducial`, `RawDetection`                                                                                                                                                                |
| `yall.networktables.geometry` | `Orientation3d`, `AngularVelocity3d`                                                                                                                                                         |
| `yall.networktables.targets`  | `RetroreflectiveTape`, `Barcode`, `AprilTagFiducial`, `NeuralClassifier`, `NeuralDetector`                                                                                                   |
| `yall.networktables.util`     | Array/pose conversion helpers, equivalent to Java's `LimelightUtils`                                                                                                                         |
| `yall.deprecated`             | Legacy compatibility shims                                                                                                                                                                   |

## Differences from the Java API

The Python port is a direct translation of an earlier revision of the Java library and has not yet caught up to every 2026 addition. Known differences:

* `createPoseEstimator(megatag2: bool)` takes a `bool` instead of Java's `EstimationMode` enum.
* `LimelightSettings.withCameraToRobot(pose)` is the Python equivalent of Java's `withCameraOffset(Pose3d)`.
* `withArilTagIdFilter(...)` — note the typo carried over from an earlier API revision; this is the equivalent of Java's `withAprilTagIdFilter`.
* No Python equivalents yet for `IMUResults`, `HardwareReport`, `HailoStats`, `RewindStats`, or rewind-capture / USB port-forwarding helpers.

For the full method-by-method reference, use the [Java reference](../java-reference/java-reference.md) as the source of truth — field names and behavior are otherwise identical — and cross-check against `yall/python/yall/` in the YALL repository for the exact Python signature.

## See Also

* [Java API](../java-reference/java-reference.md)
