# Basic Setup

This tutorial creates a `Limelight`, configures it, and reads its 2D target data: the minimum setup for any pipeline type.

## 1. Install YALL

See [How do I install YALL?](../how-to-guides/how-do-i-install-yall.md) if you haven't added the vendordep yet.

## 2. Create the Limelight object

Create one `Limelight` per physical camera, named after its NetworkTables table (the name shown in the Limelight web UI, usually `limelight`):

```java
import limelight.Limelight;

Limelight limelight = new Limelight("limelight");
```

{% hint style="info" %}
Construction checks that the camera's NT table exists (skipped in simulation) and raises a WPILib `Alert` if it doesn't. Do this once, in your subsystem constructor, not in `periodic()`.
{% endhint %}

## 3. Configure it

`getSettings()` returns a chainable configuration object. Every `.withXXXX()` call applies immediately and returns `this`:

```java
import limelight.networktables.LimelightSettings.LEDMode;
import edu.wpi.first.math.geometry.Pose3d;

limelight.getSettings()
         .withLimelightLEDMode(LEDMode.PipelineControl)
         .withCameraOffset(Pose3d.kZero)
         .save();
```

`withCameraOffset` sets where the camera physically sits relative to robot center. Get this right, since MegaTag pose estimation is only as accurate as this offset. See [LimelightSettings & Pipelines](../understanding/limelight-settings-and-pipelines.md) for the full option list.

## 4. Read 2D target data

For simple aiming (turn-to-target, distance-to-target), `getData().targetData` gives you crosshair-relative offsets without any pose estimation:

```java
if (limelight.getData().targetData.getTargetStatus()) {
    double tx = limelight.getData().targetData.getHorizontalOffset();
    double ty = limelight.getData().targetData.getVerticalOffset();
    // turn toward tx, adjust range using ty
}
```

Always check `getTargetStatus()` before trusting `tx`/`ty`, since they hold stale values when no target is visible.

## Next steps

* [AprilTag Pose Estimation](apriltag-pose-estimation.md): fuse MegaTag2 into your robot's pose estimator.
* [Object Detection](object-detection.md): read neural classifier/detector results.
* [Simulating a Limelight](simulating-a-limelight.md): exercise this same code in desktop simulation.
* [How do I choose between MegaTag1 and MegaTag2?](../how-to-guides/how-do-i-choose-megatag1-vs-megatag2.md)
* [Tuning Workflow Overview](../tuning/tuning-workflow-overview.md): once basic setup works, this is the recommended order for getting accurate, reliable results out of whichever pipeline you're using.
