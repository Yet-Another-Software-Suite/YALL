# NeuralDetector

**Package:** `limelight.networktables.target.pipeline`

A single result from the JSON `Detector` array in [`LimelightResults`](../networktables/limelight-results.md). Unlike [`NeuralClassifier`](neural-classifier.md), the detector pipeline reports individual bounding-box detections with crosshair-relative angles, matching the layout of [`AprilTagFiducial`](apriltag-fiducial.md)/[`RetroreflectiveTape`](retroreflective-tape.md).

***

## Fields

| Field                              | Type     | Description                                    |
| ---------------------------------- | -------- | ---------------------------------------------- |
| `className`                        | `String` | Predicted class name (`class`).                |
| `classID`                          | `double` | Predicted class ID (`classID`).                |
| `confidence`                       | `double` | Prediction confidence (`conf`).                |
| `ta`                               | `double` | Target area as a percentage of the image, 0-1. |
| `tx`, `ty`                         | `double` | Crosshair-relative offset in degrees.          |
| `tx_pixels`, `ty_pixels`           | `double` | Crosshair-relative offset in pixels.           |
| `tx_nocrosshair`, `ty_nocrosshair` | `double` | Principal-pixel-relative offset in degrees.    |

## See Also

* [NeuralClassifier](neural-classifier.md)
* [RawDetection](../results-and-telemetry/raw-detection.md)
