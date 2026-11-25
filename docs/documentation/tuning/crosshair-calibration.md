---
icon: crosshairs
---

# Crosshair Calibration

{% hint style="info" %}
This page is about Limelight's crosshair. See Limelight's [Crosshair Calibration](https://docs.limelightvision.io/docs/docs-limelight/getting-started/crosshair) for the full guide.
{% endhint %}

The crosshair is the reference point Limelight's `tx`/`ty`, and so YALL's [`VisionTarget.yawDegrees()`](https://yall.yassrobotics.com/api-reference/java-reference/results/vision-target) / `pitchDegrees()`, are measured against. If you're driving directly off `tx`/`ty` (visual servoing, not a full 3D pose), calibrating it correctly is what makes "`tx == 0`" actually mean "aligned with where I want to score," instead of just "aligned with the sensor's optical center."

## Single crosshair mode

Use this when your scoring position and camera angle don't vary.

{% stepper %}
{% step %}
### Put the robot where it scores

Drive the robot to its best scoring spot, facing the way it should.

**Check it:** the robot would score from here.
{% endstep %}

{% step %}
### Calibrate

Click **Calibrate** in the Limelight's web page.

**Check it:** yaw and pitch read 0 when the robot is back in that spot. Calibrate again whenever the camera mount or scoring spot changes.
{% endstep %}
{% endstepper %}

## Dual crosshair mode

Use this for off-axis cameras or shooters that need real-time compensation as range changes. The effective crosshair interpolates between crosshair A and B based on target area.

{% stepper %}
{% step %}
### Calibrate crosshair A up close

Drive to the **closest** scoring spot, facing the right way, and click **Calibrate** for crosshair A.

**Check it:** yaw and pitch read 0 there.
{% endstep %}

{% step %}
### Calibrate crosshair B far away

Drive to the **farthest** scoring spot and click **Calibrate** for crosshair B.

**Check it:** yaw and pitch read 0 there, and in between the Limelight blends A and B by the target's size, with no extra code.
{% endstep %}
{% endstepper %}

{% hint style="info" %}
Crosshair calibration only affects `tx`/`ty` (and the yaw and pitch YALL reads from them). It has no effect on 3D pose data (`cameraToTarget()`, [`VisionPoseEstimate`](https://yall.yassrobotics.com/api-reference/java-reference/results/vision-pose-estimate)); that accuracy comes from [camera calibration](charuco-camera-calibration.md) instead.
{% endhint %}

## Read more

* [Tuning Workflow Overview](tuning-workflow-overview.md)
* [PID Tuning for Aiming & Ranging](pid-tuning-for-aiming-and-ranging.md)
