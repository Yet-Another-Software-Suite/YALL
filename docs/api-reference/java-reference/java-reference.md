# Overview

**Root package:** `limelight`

```java
import limelight.Limelight;
```

## Package Layout

| Package                                   | Contents                                                                   |
| ----------------------------------------- | -------------------------------------------------------------------------- |
| `limelight`                               | [`Limelight`](limelight.md) — the camera object                            |
| `limelight.networktables`                 | Settings, data access, pose estimation, JSON results, and geometry helpers |
| `limelight.networktables.target`          | Typed 2D/3D target results: AprilTag, retroreflective, barcode             |
| `limelight.networktables.target.pipeline` | Typed neural pipeline results: classifier, detector                        |
| `limelight.results`                       | Raw NetworkTables detection structs and hardware/IMU/rewind telemetry      |

## Where to Start

Every use of YALL begins with one `Limelight` object per camera:

```java
Limelight limelight = new Limelight("limelight");
```

From there:

* [`getSettings()`](networktables/limelight-settings.md) configures the camera (LEDs, pipeline, crop, camera offset, IMU mode, robot orientation, ...)
* [`createPoseEstimator(EstimationMode)`](networktables/limelight-pose-estimator.md) returns a [`LimelightPoseEstimator`](networktables/limelight-pose-estimator.md) for MegaTag1/MegaTag2 field-relative pose
* [`getData()`](networktables/limelight-data.md) exposes raw fiducials, raw detections, barcodes, and Python script I/O
* [`getLatestResults()`](networktables/limelight-results.md) returns the full JSON `LimelightResults` for a frame, including every target type

## Reference

* [Limelight](limelight.md)
* NetworkTables
  * [LimelightSettings](networktables/limelight-settings.md)
  * [LimelightPoseEstimator](networktables/limelight-pose-estimator.md)
  * [PoseEstimate](networktables/pose-estimate.md)
  * [LimelightData](networktables/limelight-data.md)
  * [LimelightPipelineData](networktables/limelight-pipeline-data.md)
  * [LimelightTargetData](networktables/limelight-target-data.md)
  * [LimelightResults](networktables/limelight-results.md)
  * [LimelightUtils](networktables/limelight-utils.md)
  * [Orientation3d](networktables/orientation3d.md)
  * [AngularVelocity3d](networktables/angular-velocity3d.md)
  * [AngularAcceleration3d](networktables/angular-acceleration3d.md)
* Target Types
  * [AprilTagFiducial](target-types/apriltag-fiducial.md)
  * [RetroreflectiveTape](target-types/retroreflective-tape.md)
  * [Barcode](target-types/barcode.md)
  * [NeuralClassifier](target-types/neural-classifier.md)
  * [NeuralDetector](target-types/neural-detector.md)
* Results & Telemetry
  * [RawFiducial](results-and-telemetry/raw-fiducial.md)
  * [RawDetection](results-and-telemetry/raw-detection.md)
  * [IMUResults](results-and-telemetry/imu-results.md)
  * [HardwareReport](results-and-telemetry/hardware-report.md)
  * [HailoStats](results-and-telemetry/hailo-stats.md)
  * [RewindStats](results-and-telemetry/rewind-stats.md)
