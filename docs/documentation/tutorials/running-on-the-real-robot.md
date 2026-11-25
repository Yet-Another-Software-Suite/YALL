---
icon: robot
---

# Running on the Real Robot

Simulation is a great start, but the real robot has real cameras, a real gyro and real mistakes. This tutorial checks each piece safely before you trust vision in a match.

## Before you start

* Finish [AprilTag Pose Estimation](apriltag-pose-estimation.md) in simulation.
* Set up the camera in its maker's tools first: name it, give it a fixed IP address, and set up an AprilTag pipeline.
  * PhotonVision: [Selecting Hardware](https://docs.photonvision.org/en/latest/docs/hardware/selecting-hardware.html), then [Camera Tuning / Input](https://docs.photonvision.org/en/latest/docs/pipelines/input.html) and [2D AprilTag Tuning](https://docs.photonvision.org/en/latest/docs/apriltag-pipelines/2D-tracking-tuning.html).
  * Limelight: [Pipeline Setup](https://docs.limelightvision.io/docs/docs-limelight/getting-started/pipelines) and [Tracking AprilTags](https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltags).
  * Synapse: [the Synapse docs](https://danpeled.gitbook.io/synapse/).
* Calibrate the camera (teach it its exact lens), so its distances are right: [ChArUco Camera Calibration](../tuning/charuco-camera-calibration.md).
* Put the robot on blocks or somewhere it can't drive into anything. This tutorial doesn't drive.

{% stepper %}
{% step %}
### Match the names

The name in your code must match the camera's name in its own tools: the PhotonVision camera name, the Limelight's hostname (for example `limelight` or `limelight-front`), or the Synapse camera name.

**Check it:** deploy, then look at `YALL/<camera name>/connected` on a dashboard. It's `true`.
{% endstep %}

{% step %}
### Check the camera sees tags

Hold an AprilTag (or move the robot) in front of the camera.

**Check it:** `YALL/<camera name>/targetCount` goes above 0, and `Vision/best tag` shows the tag's ID.
{% endstep %}

{% step %}
### Check left and right

Move the tag to the right side of the camera's picture.

**Check it:** `Vision/best yaw` goes **positive**. Move it up: `Vision/best pitch` goes **positive**. If a sign is backward, the camera is probably mounted upside down or set to a flipped orientation in its own tools; fix it there.
{% endstep %}

{% step %}
### Measure the camera offset

Measure from the middle of the robot, on the floor, to the middle of the camera lens: how far forward, left and up, and how it is tilted and turned. Put the numbers in `withRobotToCamera`. Every position guess is only as good as these numbers.

**Check it:** put the robot at a spot you can measure on the field (for example against a wall, a known distance from a tag). `Vision/robot pose` should be within a few centimeters of the real spot. If it's off by about the same amount everywhere, recheck forward and left. If it's off more the farther the tag is, recheck the tilt.
{% endstep %}

{% step %}
### Compare with odometry

Turn on `addVisionMeasurements` (as in [AprilTag Pose Estimation](apriltag-pose-estimation.md)), then drive slowly around the field.

**Check it:** the pose estimate stays steady and matches where the robot really is. `YALL/<camera name>/acceptedPoses` sit on the robot, and `rejectionReason` says why the others were thrown out.
{% endstep %}
{% endstepper %}

{% hint style="info" %}
**Why `Robot.java`?** YALL doesn't use the command framework, so it works the same with Commands v2 and Commands v3. Both robot classes, `TimedRobot` (Commands v2) and `OpModeRobot` (Commands v3), have `robotPeriodic()`, which runs every loop, and `simulationPeriodic()`, which runs every loop in simulation. Commands v3 mechanisms have no `simulationPeriodic()`, so `Robot.java` is the one place that works for both.
{% endhint %}

## If it did not work

* **`connected` is false:** the name doesn't match, the camera has no power, or it isn't on the robot's network.
* **Every guess is rejected:** read `YALL/<camera name>/rejectionReason`. `too old` usually means the camera's clock isn't synced; `off the field` usually means a wrong camera offset or field.
* **More problems:** [Troubleshooting](../how-to-guides/troubleshooting.md).

## The complete file

This one uses a Limelight. Swap the wrapper for your camera, as in [Basic Setup](basic-setup.md).

{% tabs %}
{% tab title="Commands v2" %}
`first/robot/Robot.java`:

```java
package first.robot;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Inches;

import com.limelightvision.Limelight;
import org.wpilib.framework.TimedRobot;
import org.wpilib.telemetry.Telemetry;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.LimelightWrapper;
import yall.sim.CameraModel;

public class Robot extends TimedRobot {
  private final VisionCamera camera =
      new LimelightWrapper(
          new Limelight("limelight"),
          new VisionCameraConfig()
              .withCameraModel(CameraModel.LIMELIGHT_4)
              // Measure these on your robot.
              .withRobotToCamera(
                  Inches.of(12), Inches.zero(), Inches.of(10),
                  Degrees.zero(), Degrees.of(-15), Degrees.zero()));

  @Override
  public void robotPeriodic() {
    camera.update();
    Telemetry.log("Vision/connected", camera.isConnected());
    camera
        .getBestTarget()
        .ifPresent(
            target -> {
              Telemetry.log("Vision/best tag", target.fiducialId());
              Telemetry.log("Vision/best yaw", Degrees.of(target.yawDegrees()));
              Telemetry.log("Vision/best pitch", Degrees.of(target.pitchDegrees()));
            });
    camera
        .getEstimatedPose()
        .ifPresent(estimate -> Telemetry.log("Vision/robot pose", estimate.pose2d()));
  }
}
```
{% endtab %}

{% tab title="Commands v3" %}
`first/robot/Robot.java`:

```java
package first.robot;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Inches;

import com.limelightvision.Limelight;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.telemetry.Telemetry;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.LimelightWrapper;
import yall.sim.CameraModel;

public class Robot extends OpModeRobot {
  private final VisionCamera camera =
      new LimelightWrapper(
          new Limelight("limelight"),
          new VisionCameraConfig()
              .withCameraModel(CameraModel.LIMELIGHT_4)
              // Measure these on your robot.
              .withRobotToCamera(
                  Inches.of(12), Inches.zero(), Inches.of(10),
                  Degrees.zero(), Degrees.of(-15), Degrees.zero()));

  @Override
  public void robotPeriodic() {
    camera.update();
    Telemetry.log("Vision/connected", camera.isConnected());
    camera
        .getBestTarget()
        .ifPresent(
            target -> {
              Telemetry.log("Vision/best tag", target.fiducialId());
              Telemetry.log("Vision/best yaw", Degrees.of(target.yawDegrees()));
              Telemetry.log("Vision/best pitch", Degrees.of(target.pitchDegrees()));
            });
    camera
        .getEstimatedPose()
        .ifPresent(estimate -> Telemetry.log("Vision/robot pose", estimate.pose2d()));
  }
}
```
{% endtab %}
{% endtabs %}
