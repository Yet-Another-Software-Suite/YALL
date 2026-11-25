# Basic Setup

This tutorial creates one camera, tells YALL where it sits on the robot, and reads its targets. It works the same for PhotonVision, Limelight and Synapse cameras. Install YALL first: [How do I install YALL?](../how-to-guides/how-do-i-install-yall.md)

## The basic config

One camera feeding a drivetrain's pose estimator. Everything not set here keeps its default: the current season's field, the default pose filter and `PoseEstimationMode.TAGS_ONLY`. Set the camera model to your real camera; [Simulating Cameras](simulating-cameras.md#2-match-the-simulated-camera-to-the-real-one) lists them.

```java
import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Inches;

import org.photonvision.PhotonCamera;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.PhotonVisionWrapper;
import yall.sim.CameraModel;
import yall.sim.VisionSim;

public class Drivetrain extends SubsystemBase {
  // Swap in LimelightWrapper(new Limelight("limelight"), ...) or
  // SynapseWrapper(new SynapseCamera("camera"), ...) for the other vendors.
  private final VisionCamera camera =
      new PhotonVisionWrapper(
          new PhotonCamera("front"),
          new VisionCameraConfig()
              // The real camera, so simulation sees what it would.
              .withCameraModel(CameraModel.ARDUCAM_OV9281)
              // 12" forward of center, centered, 10" up, tilted 15° up.
              .withRobotToCamera(Inches.of(12), Inches.zero(), Inches.of(10),
                                 Degrees.zero(), Degrees.of(-15), Degrees.zero()));

  @Override
  public void periodic() {
    poseEstimator.update(gyro.getRotation2d(), getModulePositions());
    camera.setRobotOrientation(gyro.getRotation2d());
    camera.update();
    camera.addVisionMeasurements(poseEstimator);
  }

  @Override
  public void simulationPeriodic() {
    // The simulated cameras see the field from the robot's true pose.
    VisionSim.getDefault().update(simulatedTruePose);
  }
}
```

## Where the camera is

`withRobotToCamera` takes measurements from the robot's center on the floor, in the same order as Limelight's camera pose:

| Argument | Positive means |
| --- | --- |
| forward | in front of the robot center |
| left | left of the robot center |
| up | above the floor |
| roll | rotated counterclockwise about the forward axis |
| pitch | tilted **down**. Use a negative pitch for a camera tilted up. |
| yaw | turned counterclockwise. `Degrees.of(180)` faces backward. |

If you already have a `Transform3d`, pass it instead: `withRobotToCamera(new Transform3d(...))`.

Measure carefully. Every pose estimate is only as good as this offset.

## Reading targets

`update()` reads every frame the camera produced since the last call. Call it once per loop, then read what it found:

```java
camera.update();

if (camera.hasTargets()) {
  VisionTarget target = camera.getBestTarget().get();
  double yaw = target.yawDegrees();     // positive right of the crosshair
  double pitch = target.pitchDegrees(); // positive above the crosshair
  double area = target.areaPercent();   // 0 to 100; NaN on Synapse
  int tagId = target.fiducialId();      // -1 if not an AprilTag
}
```

* `getLatestResult()` returns the newest frame (`VisionResult`), even if the last `update()` read none.
* `getUnreadResults()` returns every frame the last `update()` read, oldest first.
* `getTargets()` and `getBestTarget()` read the newest frame. The best target is the camera's own ranking.

## Aiming at a target

Turn toward the best target with a proportional controller:

```java
double kP = 0.03;
double turn = camera.getBestTarget()
    .map(target -> -kP * target.yawDegrees()) // target to the right: turn clockwise
    .orElse(0.0);
```

Yaw is positive right, and WPILib turns are counterclockwise-positive, so the sign flips. See [PID Tuning for Aiming & Ranging](../tuning/pid-tuning-for-aiming-and-ranging.md) to tune the gain.

## Pipelines and connection

```java
camera.setPipelineIndex(1);
int pipeline = camera.getPipelineIndex();
boolean connected = camera.isConnected(); // always true in simulation
```

Set the starting pipeline in the config with `withPipelineIndex(1)`.

## See Also

* [AprilTag Pose Estimation](apriltag-pose-estimation.md)
* [Simulating Cameras](simulating-cameras.md)
* [Results Model](../understanding/results-model.md)
