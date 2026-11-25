---
icon: layer-group
---

# VisionSystem

In short: several cameras working together, feeding one pose estimator.

**Package:** `yall`

```java
public class VisionSystem implements AutoCloseable
```

Several [`VisionCamera`](vision-camera.md)s working together, from any mix of vendors.

```java
VisionSystem vision = new VisionSystem(frontLimelight, backPhoton, sideSynapse);

// In Robot.robotPeriodic() (Commands v2 or v3):
public void robotPeriodic() {
  vision.setRobotOrientation(gyro.getRotation3d(), gyroYawRate);
  vision.update();
  vision.addVisionMeasurements(poseEstimator);
}

// In Robot.simulationPeriodic() (Commands v2 or v3):
public void simulationPeriodic() {
  vision.updateSimulation(drivetrainSim.getPose());
}
```

`addVisionMeasurements(PoseEstimator)` is the most accurate way to use several cameras: the pose estimator weighs each estimate at the moment its image was taken. `getFusedPose()` combines the cameras' newest estimates into one pose, for code that wants a single answer.

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
| `withMaxFusionTimeSkew(Time maxSkew)` | `VisionSystem` | Sets how far apart in time the cameras' newest estimates can be and still be fused. Older ones are left out of `getFusedPose()`. Defaults to 0.1 s. |
| `update()` | `void` | Updates every camera. Call once per loop. |
| `updateSimulation(Pose2d robotPose)` | `void` | Updates each simulated world the cameras are in, once, with the robot's true pose. Call once per loop from `simulationPeriodic`. |
| `setRobotOrientation(Rotation3d orientation, AngularVelocity yawRate)` | `void` | Gives every camera the robot's orientation from the gyro. |
| `setRobotOrientation(Rotation2d heading)` | `void` | Gives every camera the robot's heading from the gyro, without a yaw rate. |
| `getCameras()` | `List<VisionCamera>` | The cameras in this system. |
| `getEstimatedPoses()` | `List<VisionPoseEstimate>` | Every accepted estimate from every camera in the last `update()`, oldest first. |
| `forEachEstimatedPose(Consumer<VisionPoseEstimate> consumer)` | `void` | Passes every accepted estimate from the last `update()` to a consumer, oldest first. Use it for drivetrains with their own vision measurement method. |
| `addVisionMeasurements(PoseEstimator<?> poseEstimator)` | `void` | Adds every accepted estimate from the last `update()` to a WPILib pose estimator, oldest first, with its timestamp and standard deviations. |
| `getFusedPose()` | `Optional<VisionPoseEstimate>` | Combines each camera's newest accepted estimate from the last `update()` into one pose, named `"fused"`. Empty if no camera has one. |
| `static fuse(List<VisionPoseEstimate> estimates)` | `VisionPoseEstimate` | Combines estimates into one. The list must not be empty; a single estimate is returned as it is. |
| `close()` | `void` | Closes every camera and the fused pose telemetry. |

***

## How estimates are fused

`getFusedPose()` takes each camera's newest accepted estimate, drops any more than the time skew older than the newest, and passes the rest to `fuse`:

* **x, y and heading:** each is a weighted average, weighted by the inverse square of that axis's standard deviation, so more trusted cameras count for more. Headings are averaged as unit vectors, so 179° and -179° average to 180°.
* **Height, roll and pitch:** plain averages.
* **Timestamp and tag distance:** weighted like x.
* **Tag IDs:** every tag used by any estimate.
* **Standard deviations:** `1 / sqrt(sum of the weights)` for each axis.
* **Heading-assisted:** only if every fused estimate was.

`getFusedPose()` only covers the last `update()`, so it is empty in a loop where no camera produced an accepted estimate.

***

## Telemetry

Each `update()` publishes the fused pose to `YALL/fusedPose` (`Pose3d`) when there is one.

***

## Read more

* [VisionCamera](vision-camera.md)
* [VisionPoseEstimate](../results/vision-pose-estimate.md)
