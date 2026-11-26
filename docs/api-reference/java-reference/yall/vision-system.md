---
icon: layer-group
---

# VisionSystem

In short: several cameras working together, each estimate going to your pose estimator.

**Package:** `yall`

```java
public class VisionSystem implements AutoCloseable
```

Several [`VisionCamera`](vision-camera.md)s working together, from any mix of vendors.

```java
VisionSystem vision = new VisionSystem(frontLimelight, backPhoton, sideSynapse);

// In Robot.robotPeriodic() (Commands v2 or v3):
public void robotPeriodic() {
  vision.setRobotOrientation(poseEstimator.getEstimatedPosition().getRotation(), gyroYawRate);
  vision.update();
  vision.addVisionMeasurements(poseEstimator);
}

// In Robot.simulationPeriodic() (Commands v2 or v3):
public void simulationPeriodic() {
  vision.updateSimulation(drivetrainSim.getPose());
}
```

`addVisionMeasurements(PoseEstimator)` calls the pose estimator's `addVisionMeasurement` once for every accepted estimate, with the estimate's own timestamp and standard deviations, so the pose estimator applies each at the moment its image was taken and trusts each camera as much as it should. For a drivetrain with its own `addVisionMeasurement` method, such as a YAMS `SwerveDrive`, use `forEachEstimatedPose`:

```java
vision.forEachEstimatedPose(
    estimate ->
        swerveDrive.addVisionMeasurement(
            estimate.pose2d(), estimate.timestampSeconds(), estimate.stdDevs()));
```

Don't combine the cameras' estimates before adding them: that loses their timestamps and standard deviations. See [How do I use multiple cameras?](https://yall.yassrobotics.com/documentation/how-to-guides/how-do-i-use-multiple-cameras#how-to-use-addvisionmeasurement)

***

## Constructor

```java
VisionSystem(VisionCamera... cameras)
```

| Parameter | Description |
| --- | --- |
| `cameras` | The cameras. |

***

## Methods

| Method | Returns | Description |
| --- | --- | --- |
| `update()` | `void` | Updates every camera and gathers their accepted estimates, oldest first. Call once per loop. |
| `updateSimulation(Pose2d robotPose)` | `void` | Updates each simulated world the cameras are in, once, with the robot's true pose. Call once per loop from `simulationPeriodic`. |
| `setRobotOrientation(Rotation3d orientation, AngularVelocity yawRate)` | `void` | Gives every camera the robot's orientation on the field: the yaw from your pose estimator, the roll and pitch from the gyro. |
| `setRobotOrientation(Rotation2d heading, AngularVelocity yawRate)` | `void` | Gives every camera the robot's heading on the field, your pose estimator's rotation, and its yaw rate. |
| `setRobotOrientation(Rotation2d heading)` | `void` | Gives every camera the robot's heading on the field, without a yaw rate. |
| `getCameras()` | `List<VisionCamera>` | The cameras in this system. |
| `getEstimatedPoses()` | `List<VisionPoseEstimate>` | Every accepted estimate from every camera in the last `update()`, oldest first. |
| `forEachEstimatedPose(Consumer<VisionPoseEstimate> consumer)` | `void` | Passes every accepted estimate from the last `update()` to a consumer, oldest first. Use it for drivetrains with their own `addVisionMeasurement` method, passing each estimate's `pose2d()`, `timestampSeconds()` and `stdDevs()`. |
| `addVisionMeasurements(PoseEstimator<?> poseEstimator)` | `void` | Adds every accepted estimate from the last `update()` to a WPILib pose estimator, oldest first, with its timestamp and standard deviations. Call once per loop, after `update()`. |
| `close()` | `void` | Closes every camera. |

***

## Read more

* [VisionCamera](vision-camera.md)
* [VisionPoseEstimate](../results/vision-pose-estimate.md)
