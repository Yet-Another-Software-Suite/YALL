# Target & Pipeline Types

A Limelight pipeline produces exactly one kind of target result, and `LimelightResults` exposes each kind as its own typed array. Only the array matching the active pipeline's type is populated for a given frame; the rest are empty.

| Pipeline type       | `LimelightResults` field | Type                    | Produced by                                                                |
| ------------------- | ------------------------ | ----------------------- | -------------------------------------------------------------------------- |
| AprilTag / fiducial | `targets_Fiducials`      | `AprilTagFiducial[]`    | AprilTag detection + SolvePnP.                                             |
| Retroreflective     | `targets_Retro`          | `RetroreflectiveTape[]` | Color/contour-based retroreflective tape pipelines.                        |
| Neural classifier   | `targets_Classifier`     | `NeuralClassifier[]`    | Whole-frame/zone classification model; labels the scene, no bounding box.  |
| Neural detector     | `targets_Detector`       | `NeuralDetector[]`      | Object detection model; per-object bounding boxes with class + confidence. |
| Barcode             | `targets_Barcode`        | `Barcode[]`             | QR/DataMatrix/etc. decoding.                                               |

Check `LimelightResults.pipeline_type` if your code needs to confirm which pipeline produced a given frame before reading its target array.

## Classifier vs. Detector

These are easy to confuse since both are "neural" pipelines:

* **Classifier** (`NeuralClassifier`): answers "what is this?" for the whole frame or a zone. Has `className`, `confidence`, and a rough `tx`/`ty` position, but no bounding box or area.
* **Detector** (`NeuralDetector`): answers "what objects are where?" with individual bounding boxes. Has `className`, `confidence`, `ta` (area), and crosshair-relative `tx`/`ty`, laid out the same way as `AprilTagFiducial`/`RetroreflectiveTape`. The raw-NT equivalent, `RawDetection`, also exposes the four bounding-box corner points.

## AprilTag vs. Retroreflective: shared pose API

`AprilTagFiducial` and `RetroreflectiveTape` expose the identical set of `getXXXPose_YYYSpace()` / `...2D()` methods (camera-in-target-space, robot-in-field-space, robot-in-target-space, target-in-camera-space, target-in-robot-space). The only real difference is that `AprilTagFiducial` additionally carries `fiducialID` and `fiducialFamily`. If you're writing code that only cares about "where is this target," it can be written generically against either type's pose methods.

## Barcode

`Barcode` is the odd one out: it has no pose accessors, since a printed barcode has no known real-world dimension for SolvePnP to use. It gives you `family`, `data` (the decoded string), pixel/degree position, and optional pixel-space `corners` (must be enabled in the Limelight output tab).

## See Also

* [Object Detection tutorial](../tutorials/object-detection.md)
* [Tuning a Retroreflective Pipeline](../tuning/retroreflective-pipeline-tuning.md)
* [Tuning a Neural Network Pipeline](../tuning/neural-network-pipeline-tuning.md)
* LimelightResults reference
