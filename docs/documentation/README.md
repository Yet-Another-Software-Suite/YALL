---
icon: hand-wave
layout:
  width: default
  title:
    visible: true
  description:
    visible: false
  tableOfContents:
    visible: true
  outline:
    visible: true
  pagination:
    visible: true
  metadata:
    visible: true
  tags:
    visible: true
  actions:
    visible: true
  anchors:
    visible: true
---

# Welcome

Welcome to the **Yet Another Limelight Library (YALL)** documentation!

YALL is a WPILib vendordep that wraps [Limelight](https://limelightvision.io/)'s NetworkTables API and JSON results output in a typed, chainable Java (and Python) API for FRC robots. It replaces the raw `LimelightHelpers` script with real objects: a `Limelight` per camera, chainable settings, MegaTag1/MegaTag2 pose estimation, and typed results for every pipeline type, including AprilTag, retroreflective, neural classifier/detector, and barcode.

### Jump right in

<table data-view="cards"><thead><tr><th></th><th></th><th></th><th data-hidden data-card-target data-type="content-ref"></th></tr></thead><tbody><tr><td><h4><i class="fa-bolt">:bolt:</i></h4></td><td><strong>Basic setup</strong></td><td>Create your first Limelight object and configure it.</td><td><a href="tutorials/basic-setup.md">basic-setup.md</a></td></tr><tr><td><h4><i class="fa-crosshairs">:crosshairs:</i></h4></td><td><strong>AprilTag pose estimation</strong></td><td>Fuse MegaTag2 vision measurements into your pose estimator.</td><td><a href="tutorials/apriltag-pose-estimation.md">apriltag-pose-estimation.md</a></td></tr><tr><td><h4><i class="fa-eye">:eye:</i></h4></td><td><strong>Object detection</strong></td><td>Read neural classifier and detector results.</td><td><a href="tutorials/object-detection.md">object-detection.md</a></td></tr><tr><td><h4><i class="fa-display">:display:</i></h4></td><td><strong>Simulation</strong></td><td>Exercise vision code in desktop simulation without hardware.</td><td><a href="tutorials/simulating-a-limelight.md">simulating-a-limelight.md</a></td></tr></tbody></table>

### Where to go next

* **Tutorials**: step-by-step walkthroughs for common setups.
* **How-to guides**: focused answers to specific "how do I…" questions.
* [**Tuning**](tuning/tuning-workflow-overview.md): the tuning steps from Limelight's own documentation, reorganized into the order you'd actually apply them (sensor settings, crosshair/camera calibration, pipeline-specific tuning, then PID).
* **Understanding**: explanations of how YALL's settings, pose estimation, and results model work.
* **API Reference** (YALL-Gitbook-API space): every class, field, and method.
* [**Javadocs**](https://yet-another-software-suite.github.io/YALL/javadocs/): generated API docs.
* [**GitHub**](https://github.com/BroncBotz3481/YALL): source, issues, and releases.
