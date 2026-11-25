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

`addVisionMeasurements(PoseEstimator)` is the most accurate way to use several cameras: the pose estimator weighs each estimate at the moment its image was taken. Give the system your drivetrain's pose estimator with `withPoseEstimator(...)` and `update()` does it for you. `getFusedPose()` combines the cameras' newest estimates into one pose, for code that wants a single answer, with the same math as WPILib's pose estimators.

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
| `withFusionStateStdDevs(Matrix<N3, N1> stateStdDevs)` | `VisionSystem` | Sets how much `getFusedPose()` trusts the pose it has so far against each new estimate, as a WPILib pose estimator's state standard deviations do: x (m), y (m) and heading (rad). Defaults to 0.1, 0.1, 0.1, WPILib's default for swerve drives. |
| `withPoseEstimator(PoseEstimator<?> poseEstimator)` | `VisionSystem` | Gives the system the drivetrain's pose estimator. `update()` then adds every accepted estimate to it, and `getFusedPose()` returns its pose. `null` stops. |
| `update()` | `void` | Updates every camera, then adds their accepted estimates to the pose estimator, if one was given. Call once per loop. |
| `updateSimulation(Pose2d robotPose)` | `void` | Updates each simulated world the cameras are in, once, with the robot's true pose. Call once per loop from `simulationPeriodic`. |
| `setRobotOrientation(Rotation3d orientation, AngularVelocity yawRate)` | `void` | Gives every camera the robot's orientation from the gyro. |
| `setRobotOrientation(Rotation2d heading)` | `void` | Gives every camera the robot's heading from the gyro, without a yaw rate. |
| `getCameras()` | `List<VisionCamera>` | The cameras in this system. |
| `getEstimatedPoses()` | `List<VisionPoseEstimate>` | Every accepted estimate from every camera in the last `update()`, oldest first. |
| `forEachEstimatedPose(Consumer<VisionPoseEstimate> consumer)` | `void` | Passes every accepted estimate from the last `update()` to a consumer, oldest first. Use it for drivetrains with their own vision measurement method. |
| `addVisionMeasurements(PoseEstimator<?> poseEstimator)` | `void` | Adds every accepted estimate from the last `update()` to a WPILib pose estimator, oldest first, with its timestamp and standard deviations. |
| `getFusedPose()` | `Optional<VisionPoseEstimate>` | Combines each camera's newest accepted estimate from the last `update()` into one pose, named `"fused"`. With a pose estimator, its pose is the pose estimator's. Empty if no camera has one. |
| `static fuse(List<VisionPoseEstimate> estimates)` | [`VisionPoseEstimate`](../results/vision-pose-estimate.md) | Combines estimates into one, with state standard deviations of 0.1 m, 0.1 m, 0.1 rad. The list must not be empty; a single estimate is returned as it is. |
| `static fuse(List<VisionPoseEstimate> estimates, Matrix<N3, N1> stateStdDevs)` | [`VisionPoseEstimate`](../results/vision-pose-estimate.md) | Combines estimates into one, with the given state standard deviations. |
| `close()` | `void` | Closes every camera and the fused pose telemetry. |

***

## How estimates are fused

`getFusedPose()` takes each camera's newest accepted estimate, drops any more than the time skew older than the newest, and passes the rest to `fuse`. `fuse` uses the same math a WPILib pose estimator uses for a vision measurement:

* **Start:** the oldest estimate.
* **Each next estimate:** moves the fused pose toward itself by a gain on each axis, `q / (q + sqrt(q * r))`. `q` is the squared state standard deviation and `r` is the estimate's squared standard deviation. The move is a `Transform2d` from the fused pose to the estimate, scaled by the gains, exactly as WPILib applies it, so headings fuse correctly across ±180°.
* **Height, roll and pitch:** plain averages.
* **Timestamp:** the newest estimate's.
* **Tag distance:** weighted by the inverse square of each estimate's x standard deviation.
* **Tag IDs:** every tag used by any estimate.
* **Standard deviations:** those of independent measurements combined, `1 / sqrt(sum(1 / stdDev²))` on each axis.
* **Heading-assisted:** only if every fused estimate was.

A small gain means the estimate barely moves the pose: one far tag, with a large standard deviation, moves it only a little. `getFusedPose()` only covers the last `update()`, so it is empty in a loop where no camera produced an accepted estimate.

***

## Telemetry

Each `update()` publishes the fused pose to `YALL/fusedPose` (`Pose3d`) when there is one.

***

## Read more

* [VisionCamera](vision-camera.md)
* [VisionPoseEstimate](../results/vision-pose-estimate.md)
