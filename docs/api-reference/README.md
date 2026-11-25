# Overview

Yet Another Limelight Library (YALL) is a WPILib vendordep for Limelight smart cameras used in FRC. It wraps the raw Limelight NetworkTables API and `limelight.results` JSON output in a typed, chainable Java API — pose estimation (MegaTag1/MegaTag2), AprilTag/neural/retroreflective/barcode target results, raw fiducial/detection data, IMU and hardware telemetry, and Limelight configuration (LEDs, pipelines, crop windows, camera offsets, etc).

A Python port with an equivalent API surface ships alongside the Java library. A C++ port is planned but not yet implemented.

## Features

* **`Limelight`** — one object per camera, resolves its NetworkTables table by name
* **`LimelightSettings`** — chainable `.withXXXX()` configuration for LEDs, pipelines, IMU, crop, offsets, and more
* **`LimelightPoseEstimator`** — MegaTag1/MegaTag2 pose estimates with alliance-aware helpers
* **`LimelightResults`** — full JSON `limelight.results` output, including all target/pipeline types
* **`LimelightData`** — direct NetworkTables reads for raw fiducials, raw detections, barcodes, and Python script I/O
* **Typed target results** — `AprilTagFiducial`, `RetroreflectiveTape`, `NeuralClassifier`, `NeuralDetector`, `Barcode`
* **Hardware telemetry** — `IMUResults`, `HardwareReport`, `HailoStats`, `RewindStats` (2026+ hardware)

## Quick Navigation

* [Java API](java-reference/java-reference.md)
* [Python API](python-reference/python-reference.md)

For tutorials, how-to guides, and conceptual explanations, see the main YALL documentation space (YALL-Gitbook).

## Supported Hardware

| Device                                   | Notes                                            |
| ---------------------------------------- | ------------------------------------------------ |
| Limelight 2/2+/3/3G/3A                   | AprilTag, retroreflective, neural pipelines      |
| Limelight 4                              | Adds onboard IMU, rewind capture, USB networking |
| Hailo accelerator (LL4, sold separately) | Reported via `HardwareReport.hailoStats`         |
