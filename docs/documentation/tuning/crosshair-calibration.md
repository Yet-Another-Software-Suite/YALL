# Crosshair Calibration

The crosshair is the reference point Limelight's `tx`/`ty`, and so YALL's `VisionTarget.yawDegrees()` / `pitchDegrees()`, are measured against. If you're driving directly off `tx`/`ty` (visual servoing, not a full 3D pose), calibrating it correctly is what makes "`tx == 0`" actually mean "aligned with where I want to score," instead of just "aligned with the sensor's optical center."

## Single crosshair mode

Use this when your scoring position and camera angle don't vary.

1. Drive the robot to its ideal scoring location and rotation.
2. Click **Calibrate** in the Limelight web UI.
3. Verify: `tx`/`ty` should now read `0`/`0` when the robot is back in that exact position and rotation.
4. Recalibrate whenever you change field setup or camera mounting. For example, if a practice match reveals a better scoring position, recalibrate during the next lull.

## Dual crosshair mode

Use this for off-axis cameras or shooters that need real-time compensation as range changes. The effective crosshair interpolates between crosshair A and B based on target area.

1. Drive to the **closest** scoring position with ideal rotation, click **Calibrate** for crosshair A.
2. Drive to the **farthest** scoring position with ideal rotation, click **Calibrate** for crosshair B.
3. As the robot moves between those distances, the Limelight uses the target's area to interpolate between A and B automatically, with no additional code required.

{% hint style="info" %}
Crosshair calibration only affects `tx`/`ty` (and the yaw and pitch YALL reads from them). It has no effect on 3D pose data (`cameraToTarget()`, `VisionPoseEstimate`); that accuracy comes from [camera calibration](charuco-camera-calibration.md) instead.
{% endhint %}

## See Also

* [Tuning Workflow Overview](tuning-workflow-overview.md)
* [PID Tuning for Aiming & Ranging](pid-tuning-for-aiming-and-ranging.md)
