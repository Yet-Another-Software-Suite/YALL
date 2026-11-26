---
icon: ruler
---

# Measuring Standard Deviations

A **standard deviation** says how far off a camera's position guess might be. YALL starts with safe guesses for it, but your camera, lens and mounting are different. In this tutorial you measure your camera's real standard deviations with the robot standing still, and give them to the pose filter.

## Before you start

* Finish [AprilTag Pose Estimation](apriltag-pose-estimation.md).
* The stats are on NetworkTables at `HIGH` telemetry verbosity. The code below turns that on; see [Telemetry](../understanding/telemetry.md).
* The idea: if the robot doesn't move, every guess should be the same. How much they wobble anyway is the camera's real standard deviation at that distance.

Every camera keeps [`PoseFilterStats`](https://yall.yassrobotics.com/api-reference/java-reference/filter/pose-filter-stats): what its [`VisionPoseFilter`](https://yall.yassrobotics.com/api-reference/java-reference/filter/vision-pose-filter) kept and threw out, by check, and how much the kept guesses wobble. Read them in code with `camera.getFilterStats()`, or on a dashboard under `YALL/<camera name>/stats/`.

{% stepper %}
{% step %}
### Park the robot where it sees tags

Put the robot where a camera sees tags at a distance you often drive at, for example 3 m from the speaker or hub. Turn it off or disable it so nothing moves. Start with a spot where the camera sees **one** tag.

**Check it:** `YALL/<camera name>/targetCount` is 1, and `stats/accepted` keeps going up.
{% endstep %}

{% step %}
### Reset the stats

On your dashboard, set `YALL/<camera name>/stats/reset` to true. The camera starts counting again and sets it back to false. In code, `camera.resetFilterStats()` does the same.

**Check it:** `stats/accepted` and `stats/sampleCount` start again from 0.
{% endstep %}

{% step %}
### Wait, then read the spread

Wait about 10 seconds, so a few hundred guesses are counted. Then read:

* `stats/stdDevs`: how much the guesses wobble, as x (meters), y (meters), heading (radians).
* `stats/averageTagDistanceMeters`: how far the tags were.
* `stats/suggestedBaseStdDevs`: the **base** values that give that wobble at that distance. YALL's formula grows standard deviations with distance, `base * (1 + d² / 30)`, so the base is the wobble with distance taken out.

**Check it:** `stats/sampleCount` is over 100 and `stats/stdDevs` has stopped changing much.
{% endstep %}

{% step %}
### Do it again with several tags

Move to a spot where the camera sees two or more tags, reset, wait and read again. Several tags give a much steadier guess, so these numbers are smaller.

**Check it:** the several-tag `stdDevs` are smaller than the one-tag ones.
{% endstep %}

{% step %}
### Give the numbers to the pose filter

Put the suggested bases into `withStdDevs`: the one-tag numbers first, then the several-tag numbers. Round up a little: a robot that is driving wobbles more than one standing still.

```java
new VisionCameraConfig()
    .withPoseFilter(
        new VisionPoseFilter()
            .withStdDevs(
                VecBuilder.fill(0.3, 0.3, 0.5),  // one tag: x, y (m), heading (rad)
                VecBuilder.fill(0.1, 0.1, 0.2),  // several tags
                30));                            // the distance divisor (keep 30)
```

These numbers are placeholders: use yours.

**Check it:** drive around. The pose estimate follows the tags without jumping.
{% endstep %}

{% step %}
### Check what the filter throws out

While you drive, watch `stats/acceptanceRate` (the share of guesses kept) and the counts under `stats/rejectedBy`, one per check. A check with a big count is throwing out a lot: if those guesses looked right on the field view, loosen that check. See [How do I tune the pose filter?](../how-to-guides/how-do-i-tune-the-pose-filter.md)

**Check it:** most guesses are kept, and the ones thrown out are the ones far from the robot.
{% endstep %}
{% endstepper %}

{% hint style="info" %}
**In simulation the numbers are much smaller.** The simulated cameras have only a little pixel noise, so a still robot wobbles a few millimeters to a few centimeters. That is good for checking your code works, but measure the real numbers on the real robot.
{% endhint %}

## Custom checks are counted too

A check you add with `withCustomCheck("name", ...)` must have a name. Its rejections are counted under `stats/rejectedBy/name`, so you always know which check threw out a guess.

## If it did not work

* **`stats/` isn't on the dashboard:** the camera's telemetry is off or below `HIGH`. Turn the `FILTER_STATS` field on, as in [Telemetry](../understanding/telemetry.md).
* **`stats/sampleCount` stays at 0:** every guess is being thrown out. Look at `stats/rejectedBy` to see which check, and loosen it while you measure.
* **`stats/stdDevs` keeps growing:** something is moving: the robot, the camera mount or the tag. Check the camera is fastened tightly.
* **More problems:** [Troubleshooting](../how-to-guides/troubleshooting.md).

## The complete file

This robot stands still in simulation 3 m in front of the blue alliance wall and logs the camera's spread. On a real robot, use your gyro for the heading and drop `simulationPeriodic`.

{% tabs %}
{% tab title="Commands v2" %}
`first/robot/Robot.java`:

```java
package first.robot;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Inches;

import org.photonvision.PhotonCamera;
import org.wpilib.framework.TimedRobot;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.telemetry.Telemetry;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.PhotonVisionWrapper;
import yall.sim.CameraModel;
import yall.sim.VisionSim;
import yall.telemetry.TelemetryVerbosity;
import yall.telemetry.VisionTelemetryConfig;

public class Robot extends TimedRobot {
  // In simulation, the robot stands still 3 m in front of the blue alliance wall, facing it.
  private static final Pose2d SIMULATED_ROBOT_POSE = new Pose2d(3, 4, Rotation2d.PI);

  private final VisionCamera camera =
      new PhotonVisionWrapper(
          new PhotonCamera("front"),
          new VisionCameraConfig()
              .withCameraModel(CameraModel.ARDUCAM_OV9281)
              .withRobotToCamera(
                  Inches.of(12), Inches.zero(), Inches.of(10),
                  Degrees.zero(), Degrees.of(-15), Degrees.zero())
              // The pose filter stats are published at HIGH verbosity.
              .withTelemetry(
                  new VisionTelemetryConfig().withVerbosity(TelemetryVerbosity.HIGH)));

  @Override
  public void robotPeriodic() {
    camera.setRobotOrientation(SIMULATED_ROBOT_POSE.getRotation()); // on a real robot: your pose estimator's heading
    camera.update();

    var stats = camera.getFilterStats();
    var divisor = camera.getConfig().getPoseFilter().getDistanceDivisor();
    Telemetry.log("Vision/samples", stats.getSampleCount());
    Telemetry.log("Vision/stdDevs", stats.getStdDevs().getData());
    Telemetry.log("Vision/suggestedBase", stats.getSuggestedBaseStdDevs(divisor).getData());
  }

  @Override
  public void simulationPeriodic() {
    VisionSim.getDefault().update(SIMULATED_ROBOT_POSE);
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

import org.photonvision.PhotonCamera;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.telemetry.Telemetry;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.PhotonVisionWrapper;
import yall.sim.CameraModel;
import yall.sim.VisionSim;
import yall.telemetry.TelemetryVerbosity;
import yall.telemetry.VisionTelemetryConfig;

public class Robot extends OpModeRobot {
  // In simulation, the robot stands still 3 m in front of the blue alliance wall, facing it.
  private static final Pose2d SIMULATED_ROBOT_POSE = new Pose2d(3, 4, Rotation2d.PI);

  private final VisionCamera camera =
      new PhotonVisionWrapper(
          new PhotonCamera("front"),
          new VisionCameraConfig()
              .withCameraModel(CameraModel.ARDUCAM_OV9281)
              .withRobotToCamera(
                  Inches.of(12), Inches.zero(), Inches.of(10),
                  Degrees.zero(), Degrees.of(-15), Degrees.zero())
              // The pose filter stats are published at HIGH verbosity.
              .withTelemetry(
                  new VisionTelemetryConfig().withVerbosity(TelemetryVerbosity.HIGH)));

  @Override
  public void robotPeriodic() {
    camera.setRobotOrientation(SIMULATED_ROBOT_POSE.getRotation()); // on a real robot: your pose estimator's heading
    camera.update();

    var stats = camera.getFilterStats();
    var divisor = camera.getConfig().getPoseFilter().getDistanceDivisor();
    Telemetry.log("Vision/samples", stats.getSampleCount());
    Telemetry.log("Vision/stdDevs", stats.getStdDevs().getData());
    Telemetry.log("Vision/suggestedBase", stats.getSuggestedBaseStdDevs(divisor).getData());
  }

  @Override
  public void simulationPeriodic() {
    VisionSim.getDefault().update(SIMULATED_ROBOT_POSE);
  }
}
```
{% endtab %}
{% endtabs %}

## Read more

* [Pose Estimation & Filtering](../understanding/pose-estimation-and-filtering.md)
* [`PoseFilterStats`](https://yall.yassrobotics.com/api-reference/java-reference/filter/pose-filter-stats)
