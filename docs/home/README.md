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

**YALL** is a WPILib vendordep for FRC teams using [Limelight](https://limelightvision.io/) smart cameras. It wraps Limelight's NetworkTables API and JSON results output in a typed, chainable Java API — one object per camera, chainable settings, MegaTag1/MegaTag2 pose estimation, and typed results for every pipeline type. A parallel Python port ships alongside it.

<table data-view="cards"><thead><tr><th></th><th></th><th></th><th data-hidden data-card-target data-type="content-ref"></th></tr></thead><tbody><tr><td><h4><i class="fa-book" style="color:$primary;">:book:</i></h4></td><td><strong>Documentation</strong></td><td>Tutorials, how-to guides, and explanations for using YALL on your robot.</td><td><a href="https://app.gitbook.com/o/MwECAyhaWCMK5V9K79gd/s/xvbH7hsvI1wbJLmWKbYA/">Documentation</a></td></tr><tr><td><h4><i class="fa-terminal" style="color:$primary;">:terminal:</i></h4></td><td><strong>API Reference</strong></td><td>Every class, field, and method, organized by package.</td><td><a href="https://app.gitbook.com/o/MwECAyhaWCMK5V9K79gd/s/SeN5mmDroa8PGOxzfk2Q/">API Reference</a></td></tr><tr><td><h4><i class="fa-github" style="color:$primary;">:github:</i></h4></td><td><strong>Source</strong></td><td>Source code, issues, and releases.</td><td><a href="https://github.com/BroncBotz3481/YALL">https://github.com/BroncBotz3481/YALL</a></td></tr></tbody></table>

## Install

Add the vendordep in WPILib VS Code (**WPILib: Manage Vendor Libraries → Install new library (online)**) using:

```
https://Yet-Another-Software-Suite.github.io/YALL/yall.json
```

## Quick example

```java
Limelight limelight = new Limelight("limelight");

limelight.getSettings()
         .withLimelightLEDMode(LEDMode.PipelineControl)
         .withCameraOffset(Pose3d.kZero)
         .save();

Optional<PoseEstimate> visionEstimate = limelight.createPoseEstimator(EstimationMode.MEGATAG2)
                                                  .getPoseEstimate();
visionEstimate.ifPresent(estimate ->
    poseEstimator.addVisionMeasurement(estimate.pose.toPose2d(), estimate.timestampSeconds));
```

## Part of Yet Another Software Suite

YALL is one of several FRC vendor libraries maintained under **Yet Another Software Suite**, alongside [YAGSL](https://github.com/Yet-Another-Software-Suite/YAGSL) (swerve drive) and [YAMS](https://github.com/Yet-Another-Software-Suite/YAMS) (mechanisms).
