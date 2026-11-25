---
icon: sliders
---

# Tuning Workflow Overview

[Limelight's own documentation](https://docs.limelightvision.io/) has all of the tuning guidance in this section, but it's spread across a dozen unrelated pages: sensor settings live under "Getting Started," color thresholding lives under the retro pipeline, MegaTag2 IMU tuning lives under AprilTag localization, and PID starting points are buried in a tutorial. This section reorganizes the same guidance into the order you'd actually tune a robot, cross-linked to the YALL API where a step is configured from robot code instead of the web UI.

## The order that works

1. [**Camera sensor settings**](camera-sensor-settings.md): exposure, black level, gain. Do this first, regardless of pipeline type. Every later step (thresholding, detection, pose accuracy) is easier against a clean image.
2. [**Crosshair calibration**](crosshair-calibration.md): only if you're driving off `tx`/`ty` directly (visual servoing) rather than a full 3D pose.
3. [**ChArUco camera calibration**](charuco-camera-calibration.md): only if you need accurate 3D measurements: AprilTag 3D tracking, robot localization, or `getXXXPose_YYYSpace()` calls in general.
4. Pipeline-specific tuning: pick the one matching your active pipeline:
   * [**Retroreflective pipeline tuning**](retroreflective-pipeline-tuning.md): HSV thresholding, then contour filtering/sorting.
   * [**AprilTag & MegaTag2 localization tuning**](apriltag-and-megatag2-tuning.md): robot-space offset, field map, IMU mode, assist alpha.
   * [**Neural network pipeline tuning**](neural-network-pipeline-tuning.md): confidence threshold, crop window.
5. [**PID tuning for aiming & ranging**](pid-tuning-for-aiming-and-ranging.md): once target tracking itself is stable, tune the control loop that drives on it.

{% hint style="info" %}
Steps 1–4 are almost entirely web UI configuration (the Limelight's pipeline JSON), not YALL API calls. YALL reads the _results_ of that tuning; it doesn't perform it. Each page below calls out the few settings that YALL does own, mainly camera offset, robot orientation, and IMU mode.
{% endhint %}

## See Also

* [LimelightSettings & Pipelines](../understanding/limelight-settings-and-pipelines.md)
* [Pose Estimation & Ambiguity](../understanding/pose-estimation-and-ambiguity.md)
