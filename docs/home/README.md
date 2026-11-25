---
description: >-
  Yet Another Limelight Library — a WPILib vendordep for Limelight vision on FRC
  robots
icon: house
layout:
  width: wide
  title:
    visible: true
  description:
    visible: true
  tableOfContents:
    visible: false
  outline:
    visible: false
  pagination:
    visible: false
  metadata:
    visible: false
  tags:
    visible: true
  actions:
    visible: true
  anchors:
    visible: false
---

# Yet Another Limelight Library

**YALL** gives PhotonVision, Limelight and Synapse cameras one Java API for FRC robots. A camera returns real results on the robot and simulated results in simulation, so the same code runs in both. Every camera is simulated with PhotonVision's camera simulation, whatever its vendor.

* **One camera API:** targets, robot pose estimates, pipeline and connection state, the same way for every vendor.
* **Simulation for every vendor:** presets for every Limelight, the Luma P1 and P2, the ThriftyCam, Thriftiest Cam and Arducam OV9281, from their published specs.
* **Pose filtering:** every check can be tuned or turned off, and accepted estimates get standard deviations scaled by tag distance and count.
* **Multi-camera fusion:** feed every camera's estimates to a WPILib pose estimator, or combine them into one pose.
* **Vendor pass-through:** `getCamera()` returns the vendor's own camera object.

<table data-view="cards"><thead><tr><th></th><th></th><th></th><th data-hidden data-card-target data-type="content-ref"></th></tr></thead><tbody><tr><td><h4><i class="fa-book" style="color:$primary;">:book:</i></h4></td><td><strong>Documentation</strong></td><td>Tutorials, how-to guides, tuning, and explanations for using YALL on your robot.</td><td><a href="https://app.gitbook.com/o/MwECAyhaWCMK5V9K79gd/s/xvbH7hsvI1wbJLmWKbYA/">Documentation</a></td></tr><tr><td><h4><i class="fa-terminal" style="color:$primary;">:terminal:</i></h4></td><td><strong>API Reference</strong></td><td>Every class, field, and method, organized by package.</td><td><a href="https://app.gitbook.com/o/MwECAyhaWCMK5V9K79gd/s/SeN5mmDroa8PGOxzfk2Q/">API Reference</a></td></tr><tr><td><h4><i class="fa-github" style="color:$primary;">:github:</i></h4></td><td><strong>Source</strong></td><td>Source code, issues, and releases.</td><td><a href="https://github.com/Yet-Another-Software-Suite/YALL">https://github.com/Yet-Another-Software-Suite/YALL</a></td></tr></tbody></table>

## Install

YALL builds against WPILib 2027. Install these vendordeps in WPILib VS Code (**WPILib: Manage Vendor Libraries**):

1. YALL: `https://Yet-Another-Software-Suite.github.io/YALL/yall.json`
2. PhotonLib, which YALL simulates with.
3. WPILib's **CameraServer** and **AprilTag** vendordeps, which ship with WPILib.
4. The library of each camera vendor you use: LimelightLib 2 for Limelights, SynapseLib for Synapse.

## Quick example

One camera feeding a drivetrain's pose estimator:

```java
VisionCamera camera =
    new PhotonVisionWrapper(  // or LimelightWrapper / SynapseWrapper
        new PhotonCamera("front"),
        new VisionCameraConfig()
            // The real camera, so simulation sees what it would.
            .withCameraModel(CameraModel.ARDUCAM_OV9281)
            // 12" forward of center, centered, 10" up, tilted 15° up.
            .withRobotToCamera(Inches.of(12), Inches.zero(), Inches.of(10),
                               Degrees.zero(), Degrees.of(-15), Degrees.zero()));

// Every loop:
camera.setRobotOrientation(gyro.getRotation2d());
camera.update();
camera.addVisionMeasurements(poseEstimator);

// In simulationPeriodic:
VisionSim.getDefault().update(simulatedTruePose);
```

## Part of Yet Another Software Suite

YALL is one of several FRC vendor libraries maintained under **Yet Another Software Suite**, alongside [YAGSL](https://github.com/Yet-Another-Software-Suite/YAGSL) (swerve drive) and [YAMS](https://github.com/Yet-Another-Software-Suite/YAMS) (mechanisms).
