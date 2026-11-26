# YALL with YAGSL: Commands v2

A swerve robot built with [YAGSL](https://docs.yagsl.com) and [YAMS](https://github.com/Yet-Another-Software-Suite/YAMS),
with two cameras that correct its pose with AprilTags through YALL. It's YAGSL's Commands v2 example with the cameras
added to its swerve drive. Using **Commands v3** instead? See [`../yagsl-commands3`](../yagsl-commands3).

- **Front:** a PhotonVision camera (an Arducam OV9281), solving the pose from the tags alone.
- **Back:** a Limelight 4 using MegaTag2 (`PoseEstimationMode.HEADING_ASSISTED`), which also uses the robot's
  heading.

In simulation both cameras are simulated with PhotonVision's simulator, so the example runs without a camera.

## Run it

```
./gradlew simulateJava
```

Drive with an Xbox controller on port 0 in teleop. The drive's field (`Mechanisms/swerve/field` in NetworkTables)
shows the robot's pose with the cameras' corrections, and `VisionSystemSim-YALL/Sim Field` shows what the
simulated cameras see. Each camera's telemetry is under `YALL/<camera name>`.

`./gradlew test` builds the robot, runs it for two seconds in simulation and checks that the cameras see tags and
agree with where the simulated robot is.

## How the cameras correct the pose

The cameras live in the swerve drive,
[`SwerveDriveSubsystem`](src/main/java/frc/robot/subsystems/swervedrive/SwerveDriveSubsystem.java), in a YALL
`VisionSystem`. Its `periodic()` updates the drive's odometry, then the cameras:

```java
// Keep the Limelight 4 cool: skip frames while disabled, as Limelight recommends.
backLimelight.setThrottle(RobotState.isDisabled() ? 150 : 0); // only sent when it changes
vision.setRobotOrientation(getHeading(), getYawRate());
vision.update();
vision.forEachEstimatedPose(
    estimate -> drive.addVisionMeasurement(estimate.pose2d(), estimate.timestampSeconds(), estimate.stdDevs()));
```

- `getHeading()` is the robot's heading on the field from the drive's odometry, `drive.getRotation()`.
  MegaTag2 needs that heading, not the gyro's own reading: resetting odometry, for an auto's starting pose or with
  the zero gyro button, turns the odometry heading without turning the gyro.
- `getYawRate()` lets YALL's pose filter throw out images taken while the robot spins too fast.
- Every estimate that passed the pose filter goes to the drive's pose estimator, oldest first, each with the time
  its image was taken and its own standard deviations.
- The Limelight 4 skips 150 frames between processed frames while the robot is disabled and none while enabled,
  following [Limelight's thermal management advice](https://docs.limelightvision.io/docs/docs-limelight/getting-started/limelight-4),
  so it doesn't heat up waiting in the queue.

Its `simulationPeriodic()` steps the swerve simulation, then moves the simulated cameras with the simulated robot.

## Make it yours

1. Copy this folder into your robot project, or start from it.
2. Open `build.gradle` and delete the `srcDirs '../../yall/java'` line. It builds YALL from this repository's source;
   your project uses the vendordep instead.
3. Install YALL: **WPILib: Manage Vendor Libraries → Install new libraries (online)** with
   `https://Yet-Another-Software-Suite.github.io/YALL/yall.json`. `photonlib.json`, `LimelightLib-alpha7.json` and
   `SynapseLib.json` are already in `vendordeps/`. Keep the vendor libraries for the cameras you use; PhotonLib stays,
   since YALL simulates with it.
4. In `SwerveDriveSubsystem`, give each camera the name it has on the robot (the PhotonVision camera name in
   `createCameras()`, and the Limelight's hostname in the `backLimelight` field) and its position on the robot in
   `withRobotToCamera(forward, left, up, roll, pitch, yaw)`. Negative pitch tilts the camera up.
5. Set up the swerve drive with [config.yagsl.com](https://config.yagsl.com) and YAGSL's
   [Commands v2 example README](https://github.com/Yet-Another-Software-Suite/YAGSL/tree/main/examples/commands2),
   which covers the driver controls, PathPlanner and live tuning this project keeps.

## Versions

This project uses WPILib 2027.0.0-alpha-7 and the published YAGSL and YAMS vendordeps, like YAGSL's own example.
YAMS is 2026.11.25, which has `SwerveDrive.getRotation()`.
YALL builds from this repository's source.

## Project layout

```
src/main/java/frc/robot/
  Robot.java                                        # runs the scheduler
  RobotContainer.java                               # driver bindings, autos and the swerve drive
  subsystems/swervedrive/SwerveDriveSubsystem.java  # the YAGSL swerve drive and its two YALL cameras
src/main/deploy/swerve/                             # YAGSL's swerve JSON configuration
src/main/deploy/pathplanner/                        # PathPlanner settings, paths and autos
src/test/java/frc/robot/RobotContainerTest.java     # simulation smoke test
```

## Links

- [YALL documentation](https://yall.yassrobotics.com/documentation)
- [AprilTag Pose Estimation tutorial](https://yall.yassrobotics.com/documentation/tutorials/apriltag-pose-estimation)
- [YAGSL documentation](https://docs.yagsl.com)
- [YAMS documentation](https://yams.yassrobotics.com)
