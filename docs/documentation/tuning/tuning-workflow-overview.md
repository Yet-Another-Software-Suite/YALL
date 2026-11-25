---
icon: list-check
---

# Tuning Workflow Overview

**Tuning** means adjusting the camera so it finds targets reliably, then adjusting your code so the robot uses them well. Most camera tuning happens in the camera maker's own web page (PhotonVision's or Limelight's), not in robot code. YALL reads the results of that tuning.

Tune in this order. Each step makes the next one easier.

{% stepper %}
{% step %}
### Make the picture clean

Set exposure, brightness and gain so the picture is mostly dark and the targets stand out: [Camera Sensor Settings](camera-sensor-settings.md).

**Check it:** in the camera's web page, targets are sharp and the background is dark.
{% endstep %}

{% step %}
### Calibrate the camera

Teach the camera its exact lens, so distances and positions come out right: [ChArUco Camera Calibration](charuco-camera-calibration.md). Needed for robot positions; you can skip it if you only aim with yaw and pitch.

**Check it:** the calibration's error is under 1 pixel.
{% endstep %}

{% step %}
### Tune the pipeline you use

A **pipeline** is the set of steps a camera runs on each picture.

* AprilTags and robot position: [AprilTag Localization Tuning](apriltag-and-megatag2-tuning.md).
* Game pieces: [Tuning an Object Detection Pipeline](neural-network-pipeline-tuning.md).
* Reflective tape or colored targets: tune them in the camera's own web interface. Tuning a Limelight: [Color Thresholding](https://docs.limelightvision.io/docs/docs-limelight/pipeline-retro/color-filtering) and [Contour Filtering and Sorting](https://docs.limelightvision.io/docs/docs-limelight/pipeline-retro/contour-filtering-and-sorting).

**Check it:** the camera finds the targets you care about, and nothing else.
{% endstep %}

{% step %}
### Tune your code

* Aiming with yaw and pitch: [Crosshair Calibration](crosshair-calibration.md) (Limelight), then [PID Tuning for Aiming & Ranging](pid-tuning-for-aiming-and-ranging.md).
* Robot position: [How do I tune the pose filter?](../how-to-guides/how-do-i-tune-the-pose-filter.md)

**Check it:** the robot aims or drives where you expect, without wobbling.
{% endstep %}
{% endstepper %}

## Each maker's own guides

* Tuning a PhotonVision camera: [Camera Tuning / Input](https://docs.photonvision.org/en/latest/docs/pipelines/input.html), [Calibrating Your Camera](https://docs.photonvision.org/en/latest/docs/calibration/calibration.html), [2D AprilTag Tuning](https://docs.photonvision.org/en/latest/docs/apriltag-pipelines/2D-tracking-tuning.html), [3D Tracking](https://docs.photonvision.org/en/latest/docs/apriltag-pipelines/3D-tracking.html), [MultiTag Localization](https://docs.photonvision.org/en/latest/docs/apriltag-pipelines/multitag.html), [Best Practices For Competition](https://docs.photonvision.org/en/latest/docs/additional-resources/best-practices.html).
* Tuning a Limelight: [Pipeline Setup](https://docs.limelightvision.io/docs/docs-limelight/getting-started/pipelines), [ChArUco Camera Calibration](https://docs.limelightvision.io/docs/docs-limelight/getting-started/performing-charuco-camera-calibration), [Tracking AprilTags](https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltags), [Robot Localization with MegaTag2](https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltag-robot-localization-megatag2), [Example Projects](https://docs.limelightvision.io/docs/docs-limelight/tutorials/example-projects).
* Tuning a Synapse camera: [the Synapse docs](https://danpeled.gitbook.io/synapse/).
