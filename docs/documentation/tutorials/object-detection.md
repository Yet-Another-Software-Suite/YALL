---
icon: magnifying-glass
---

# Object Detection

**Object detection** finds game pieces in the camera's picture with a trained model. In this tutorial a camera finds a ball in simulation and tells you which way to turn to reach it.

## Before you start

* Finish [Basic Setup](basic-setup.md).
* On a real robot you need a camera that runs object detection: a Limelight neural detector pipeline, or a PhotonVision object detection pipeline.
  * Tuning a PhotonVision camera: [About Object Detection](https://docs.photonvision.org/en/latest/docs/objectDetection/about-object-detection.html).
  * Tuning a Limelight: [Getting Started with Neural Networks](https://docs.limelightvision.io/docs/docs-limelight/pipeline-neural/getting-started-with-neural-networks).
* Synapse's Java library reports AprilTag and color pipelines, not object detection. A Synapse color target comes back with yaw and pitch but no class ID.

Every detected object comes back as a `VisionTarget` with a **class ID** (which kind of object, numbered in the order of your model's labels) and a **confidence** (0 to 1: how sure the model is).

{% stepper %}
{% step %}
### Point a camera at the floor

An intake camera is usually low on the bumper, tilted down. A positive pitch tilts the camera down:

```java
.withRobotToCamera(
    Inches.of(14), Inches.zero(), Inches.of(8),
    Degrees.zero(), Degrees.of(20), Degrees.zero())
```

**Check it:** the project builds.
{% endstep %}

{% step %}
### Put a game piece in the simulated world

Give it the same class ID your model uses (0 here is a placeholder):

```java
VisionSim.getDefault().addVisionTargets(
    "gamePieces",
    new VisionTargetSim(
        new Pose3d(4, 2.3, 0.075, Rotation3d.ZERO), // on the floor
        new TargetModel(0.15),                       // a 15 cm ball
        GAME_PIECE_CLASS,
        0.9f));                                      // how sure the model is
```

`VisionTargetSim` and `TargetModel` come from PhotonVision.

**Check it:** the project builds.
{% endstep %}

{% step %}
### Find the closest piece

On a camera tilted down, the closest piece is the lowest one in the picture, so it has the smallest pitch:

```java
camera.getTargets().stream()
    .filter(VisionTarget::isDetectedObject)
    .filter(target -> target.classId() == GAME_PIECE_CLASS && target.confidence() > 0.6)
    .min(Comparator.comparingDouble(VisionTarget::pitchDegrees))
    .ifPresent(piece -> Telemetry.log("Vision/game piece yaw", Degrees.of(piece.yawDegrees())));
```

**Check it:** run the simulation. `Vision/game piece yaw` is about −11°: the ball is a little to the robot's left, so its yaw is negative.
{% endstep %}
{% endstepper %}

{% hint style="info" %}
**Why `Robot.java`?** YALL doesn't use the command framework, so it works the same with Commands v2 and Commands v3. Both robot classes, `TimedRobot` (Commands v2) and `OpModeRobot` (Commands v3), have `robotPeriodic()`, which runs every loop, and `simulationPeriodic()`, which runs every loop in simulation. Commands v3 mechanisms have no `simulationPeriodic()`, so `Robot.java` is the one place that works for both.
{% endhint %}

## If it did not work

* **No pieces found:** check the class ID matches, and that the camera points at the piece (turn on the [simulated video stream](simulating-cameras.md#see-what-the-camera-sees) to look).
* **Real robot, no pieces:** the pipeline isn't an object detection pipeline, or its confidence threshold is higher than the piece's confidence.

## The complete file

{% tabs %}
{% tab title="Commands v2" %}
`first/robot/Robot.java`:

```java
package first.robot;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Inches;

import java.util.Comparator;
import org.photonvision.PhotonCamera;
import org.photonvision.estimation.TargetModel;
import org.photonvision.simulation.VisionTargetSim;
import org.wpilib.framework.TimedRobot;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.telemetry.Telemetry;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.PhotonVisionWrapper;
import yall.results.VisionTarget;
import yall.sim.CameraModel;
import yall.sim.VisionSim;

public class Robot extends TimedRobot {
  // The class ID of the game piece in your object detection model (a placeholder: use yours).
  private static final int GAME_PIECE_CLASS = 0;

  private static final Pose2d SIMULATED_ROBOT_POSE = new Pose2d(2, 2, Rotation2d.ZERO);

  private final VisionCamera camera =
      new PhotonVisionWrapper(
          new PhotonCamera("intake"),
          new VisionCameraConfig()
              .withCameraModel(CameraModel.ARDUCAM_OV9281)
              // On the front bumper, 8" up, tilted 20° down to see the floor.
              .withRobotToCamera(
                  Inches.of(14), Inches.zero(), Inches.of(8),
                  Degrees.zero(), Degrees.of(20), Degrees.zero()));

  public Robot() {
    // A 15 cm ball on the floor, 2 m ahead of the simulated robot and a little to its left.
    VisionSim.getDefault()
        .addVisionTargets(
            "gamePieces",
            new VisionTargetSim(
                new Pose3d(4, 2.3, 0.075, Rotation3d.ZERO),
                new TargetModel(0.15),
                GAME_PIECE_CLASS,
                0.9f));
  }

  @Override
  public void robotPeriodic() {
    camera.update();
    camera.getTargets().stream()
        .filter(VisionTarget::isDetectedObject)
        .filter(target -> target.classId() == GAME_PIECE_CLASS && target.confidence() > 0.6)
        // On a camera tilted down, the closest piece is the lowest one in the image.
        .min(Comparator.comparingDouble(VisionTarget::pitchDegrees))
        .ifPresent(piece -> Telemetry.log("Vision/game piece yaw", Degrees.of(piece.yawDegrees())));
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

import java.util.Comparator;
import org.photonvision.PhotonCamera;
import org.photonvision.estimation.TargetModel;
import org.photonvision.simulation.VisionTargetSim;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.telemetry.Telemetry;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.PhotonVisionWrapper;
import yall.results.VisionTarget;
import yall.sim.CameraModel;
import yall.sim.VisionSim;

public class Robot extends OpModeRobot {
  // The class ID of the game piece in your object detection model (a placeholder: use yours).
  private static final int GAME_PIECE_CLASS = 0;

  private static final Pose2d SIMULATED_ROBOT_POSE = new Pose2d(2, 2, Rotation2d.ZERO);

  private final VisionCamera camera =
      new PhotonVisionWrapper(
          new PhotonCamera("intake"),
          new VisionCameraConfig()
              .withCameraModel(CameraModel.ARDUCAM_OV9281)
              // On the front bumper, 8" up, tilted 20° down to see the floor.
              .withRobotToCamera(
                  Inches.of(14), Inches.zero(), Inches.of(8),
                  Degrees.zero(), Degrees.of(20), Degrees.zero()));

  public Robot() {
    // A 15 cm ball on the floor, 2 m ahead of the simulated robot and a little to its left.
    VisionSim.getDefault()
        .addVisionTargets(
            "gamePieces",
            new VisionTargetSim(
                new Pose3d(4, 2.3, 0.075, Rotation3d.ZERO),
                new TargetModel(0.15),
                GAME_PIECE_CLASS,
                0.9f));
  }

  @Override
  public void robotPeriodic() {
    camera.update();
    camera.getTargets().stream()
        .filter(VisionTarget::isDetectedObject)
        .filter(target -> target.classId() == GAME_PIECE_CLASS && target.confidence() > 0.6)
        // On a camera tilted down, the closest piece is the lowest one in the image.
        .min(Comparator.comparingDouble(VisionTarget::pitchDegrees))
        .ifPresent(piece -> Telemetry.log("Vision/game piece yaw", Degrees.of(piece.yawDegrees())));
  }

  @Override
  public void simulationPeriodic() {
    VisionSim.getDefault().update(SIMULATED_ROBOT_POSE);
  }
}
```
{% endtab %}
{% endtabs %}

To drive to the piece, turn by its yaw, as in [PID Tuning for Aiming & Ranging](../tuning/pid-tuning-for-aiming-and-ranging.md).
