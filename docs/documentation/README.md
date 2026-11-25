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

YALL gives **PhotonVision**, **Limelight** and **Synapse** cameras one Java API for FRC robots. A camera object returns real results on the robot and simulated results in simulation, so the same code runs in both. Every camera is simulated with PhotonVision's camera simulation, whatever its vendor.

YALL builds against WPILib 2027.

```java
VisionCamera camera =
    new PhotonVisionWrapper(  // or LimelightWrapper / SynapseWrapper
        new PhotonCamera("front"),
        new VisionCameraConfig()
            // 12" forward of center, centered, 10" up, tilted 15° up.
            .withRobotToCamera(Inches.of(12), Inches.zero(), Inches.of(10),
                               Degrees.zero(), Degrees.of(-15), Degrees.zero()));

// Every loop:
camera.setRobotOrientation(gyroHeading);
camera.update();
camera.addVisionMeasurements(poseEstimator);
```

### Jump right in

<table data-view="cards"><thead><tr><th></th><th></th><th></th><th data-hidden data-card-target data-type="content-ref"></th></tr></thead><tbody><tr><td><h4><i class="fa-bolt">:bolt:</i></h4></td><td><strong>Basic setup</strong></td><td>Create your first camera and read its targets.</td><td><a href="tutorials/basic-setup.md">basic-setup.md</a></td></tr><tr><td><h4><i class="fa-crosshairs">:crosshairs:</i></h4></td><td><strong>AprilTag pose estimation</strong></td><td>Feed filtered vision poses into your pose estimator.</td><td><a href="tutorials/apriltag-pose-estimation.md">apriltag-pose-estimation.md</a></td></tr><tr><td><h4><i class="fa-eye">:eye:</i></h4></td><td><strong>Object detection</strong></td><td>Find game pieces with any camera.</td><td><a href="tutorials/object-detection.md">object-detection.md</a></td></tr><tr><td><h4><i class="fa-display">:display:</i></h4></td><td><strong>Simulation</strong></td><td>Run your vision code in simulation, for every vendor.</td><td><a href="tutorials/simulating-cameras.md">simulating-cameras.md</a></td></tr></tbody></table>

### Where to go next

* **Tutorials**: step-by-step walkthroughs for common setups.
* **How-to guides**: focused answers to specific "how do I…" questions.
* **[Tuning](tuning/tuning-workflow-overview.md)**: tuning steps for Limelight pipelines, in the order you'd apply them.
* **Understanding**: how YALL's results, pose filtering and vendor wrappers work.
* **API Reference** (YALL-Gitbook-API space): every class, field, and method.
