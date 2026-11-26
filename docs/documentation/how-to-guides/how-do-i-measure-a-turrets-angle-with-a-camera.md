---
icon: rotate
---

# How do I measure a turret's angle with a camera?

A camera on a turret can tell you which way the turret points, from the AprilTags it sees. Use it to check your turret's encoder, or to find its zero.

## Before you start

* The camera is on the turret, and it turns with it.
* It needs to see at least one AprilTag, and solve it in 3D. PhotonVision cameras do this. A Limelight gives a camera-to-tag transform too, but it hasn't been tested on real hardware yet (see [issue #24](https://github.com/Yet-Another-Software-Suite/YALL/issues/24)). A Synapse camera can't: Synapse doesn't report where each tag is from the camera.
* You know the robot's heading on the field, from your pose estimator.

## How it works

[`VisionCamera.getCameraFieldPose()`](https://yall.yassrobotics.com/api-reference/java-reference/yall/vision-camera) works out where the camera itself is on the field from the closest tag it sees. It uses only the tag, not the robot's pose or the camera offset you set, so it can measure them. `getCameraYawOnRobot(gyroHeading)` takes the camera's heading on the field and subtracts the robot's heading: what's left is which way the camera points on the robot.

```
turret angle = camera yaw on robot - camera's angle on the turret
```

If the camera points straight along the turret, the camera's angle on the turret is 0, and the camera yaw on the robot is the turret angle.

## Measure it

{% stepper %}
{% step %}
### Read the camera's yaw on the robot

Every loop, in `robotPeriodic()`:

```java
turretCamera.update();
turretCamera
    .getCameraYawOnRobot(poseEstimator.getEstimatedPosition().getRotation())
    .ifPresent(cameraYaw -> {
      Rotation2d turretAngle = cameraYaw.minus(CAMERA_YAW_ON_TURRET);
      Telemetry.log("Turret/angle from camera", turretAngle.getMeasure());
    });
```

It is empty when the camera sees no tag it can solve, so only use it when it's there.

**Check it:** point the turret straight forward. The angle from the camera is close to 0°.
{% endstep %}

{% step %}
### Smooth it

Each reading wobbles a little, like any camera guess. Average a few, for example with WPILib's `LinearFilter.movingAverage(10)`, and only compare it with the encoder while the robot and turret stand still.

**Check it:** the averaged angle holds steady when nothing moves.
{% endstep %}

{% step %}
### Use it to check or zero the encoder

Compare the averaged angle with your turret encoder's angle. If they differ by the same amount everywhere, your encoder's zero is off by that much: subtract it, or reset the encoder to the camera's angle while the turret stands still.

**Check it:** turn the turret to a few angles; the encoder and the camera agree within a degree or two.
{% endstep %}
{% endstepper %}

## Keep the camera offset up to date

This page measures the turret from the camera. The other direction matters too: so the camera's robot pose guesses stay right, tell the camera where it is each loop with `setRobotToCamera(...)`, from the turret encoder. See [How do I mount a camera on a moving mechanism?](how-do-i-mount-a-camera-on-a-moving-mechanism.md) `getCameraYawOnRobot` doesn't use that offset, so it measures the turret correctly even if the offset is wrong.

## If it did not work

* **It is always empty:** the camera sees no AprilTag it can solve. Check `YALL/<camera name>/targetCount`, and that the tag is in the field layout.
* **It is off by a fixed angle:** the camera isn't pointing straight along the turret. Set `CAMERA_YAW_ON_TURRET` to that angle.
* **It jumps around:** the closest tag is far away or seen at a sharp angle. Move closer, or average more readings.

## The complete file

In simulation, the robot faces the blue alliance wall with the turret turned 20°, and the camera measures about 20°.

{% tabs %}
{% tab title="Commands v2" %}
`first/robot/Robot.java`:

```java
package first.robot;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Inches;

import org.photonvision.PhotonCamera;
import org.wpilib.framework.TimedRobot;
import org.wpilib.math.filter.LinearFilter;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.telemetry.Telemetry;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.PhotonVisionWrapper;
import yall.sim.CameraModel;
import yall.sim.VisionSim;

public class Robot extends TimedRobot {
  // The camera is mounted pointing straight along the turret (0° on the turret).
  private static final Rotation2d CAMERA_YAW_ON_TURRET = Rotation2d.ZERO;

  // In simulation, the robot stands still facing the blue alliance wall, turret turned 20°.
  private static final Pose2d SIMULATED_ROBOT_POSE = new Pose2d(3, 4, Rotation2d.PI);
  private static final Rotation2d SIMULATED_TURRET_ANGLE = Rotation2d.fromDegrees(20);

  private final VisionCamera turretCamera =
      new PhotonVisionWrapper(
          new PhotonCamera("turret"),
          new VisionCameraConfig()
              .withCameraModel(CameraModel.ARDUCAM_OV9281)
              // Where the camera is with the turret at 0°.
              .withRobotToCamera(
                  Inches.zero(), Inches.zero(), Inches.of(20),
                  Degrees.zero(), Degrees.zero(), Degrees.zero()));

  // Averages the last 10 readings, to smooth the camera's noise.
  private final LinearFilter average = LinearFilter.movingAverage(10);

  public Robot() {
    if (turretCamera.isSimulated()) {
      // In simulation, turn the simulated camera with the simulated turret.
      turretCamera.setRobotToCamera(
          Inches.zero(), Inches.zero(), Inches.of(20),
          Degrees.zero(), Degrees.zero(), SIMULATED_TURRET_ANGLE.getMeasure());
    }
  }

  @Override
  public void robotPeriodic() {
    Rotation2d robotHeading = SIMULATED_ROBOT_POSE.getRotation(); // on a real robot: your pose estimator's heading
    turretCamera.update();
    turretCamera
        .getCameraYawOnRobot(robotHeading)
        .ifPresent(
            cameraYaw -> {
              Rotation2d turretAngle = cameraYaw.minus(CAMERA_YAW_ON_TURRET);
              double averaged = average.calculate(turretAngle.getDegrees());
              Telemetry.log("Turret/angle from camera", Degrees.of(averaged));
            });
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
import org.wpilib.math.filter.LinearFilter;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.telemetry.Telemetry;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.PhotonVisionWrapper;
import yall.sim.CameraModel;
import yall.sim.VisionSim;

public class Robot extends OpModeRobot {
  // The camera is mounted pointing straight along the turret (0° on the turret).
  private static final Rotation2d CAMERA_YAW_ON_TURRET = Rotation2d.ZERO;

  // In simulation, the robot stands still facing the blue alliance wall, turret turned 20°.
  private static final Pose2d SIMULATED_ROBOT_POSE = new Pose2d(3, 4, Rotation2d.PI);
  private static final Rotation2d SIMULATED_TURRET_ANGLE = Rotation2d.fromDegrees(20);

  private final VisionCamera turretCamera =
      new PhotonVisionWrapper(
          new PhotonCamera("turret"),
          new VisionCameraConfig()
              .withCameraModel(CameraModel.ARDUCAM_OV9281)
              // Where the camera is with the turret at 0°.
              .withRobotToCamera(
                  Inches.zero(), Inches.zero(), Inches.of(20),
                  Degrees.zero(), Degrees.zero(), Degrees.zero()));

  // Averages the last 10 readings, to smooth the camera's noise.
  private final LinearFilter average = LinearFilter.movingAverage(10);

  public Robot() {
    if (turretCamera.isSimulated()) {
      // In simulation, turn the simulated camera with the simulated turret.
      turretCamera.setRobotToCamera(
          Inches.zero(), Inches.zero(), Inches.of(20),
          Degrees.zero(), Degrees.zero(), SIMULATED_TURRET_ANGLE.getMeasure());
    }
  }

  @Override
  public void robotPeriodic() {
    Rotation2d robotHeading = SIMULATED_ROBOT_POSE.getRotation(); // on a real robot: your pose estimator's heading
    turretCamera.update();
    turretCamera
        .getCameraYawOnRobot(robotHeading)
        .ifPresent(
            cameraYaw -> {
              Rotation2d turretAngle = cameraYaw.minus(CAMERA_YAW_ON_TURRET);
              double averaged = average.calculate(turretAngle.getDegrees());
              Telemetry.log("Turret/angle from camera", Degrees.of(averaged));
            });
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

* [`VisionCamera`](https://yall.yassrobotics.com/api-reference/java-reference/yall/vision-camera)
* [How do I mount a camera on a moving mechanism?](how-do-i-mount-a-camera-on-a-moving-mechanism.md)
