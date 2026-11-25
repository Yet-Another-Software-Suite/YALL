# YALL example robot

A simulated kitbot with one camera from each vendor: a Luma P1 running PhotonVision on the front, a
Limelight 4 on the back and a Synapse camera (an Arducam OV9281) on the left. Its wheels slip, so odometry alone drifts;
the cameras pull the pose estimate back to the truth.

## Run it

```
./gradlew simulateJava
```

Drive with a gamepad on port 0 in teleop. The Field widget (`Field` in NetworkTables) shows:

- `Robot`: odometry plus vision
- `Odometry`: odometry alone
- `Truth`: the simulated robot's real pose
- `Fused vision`: the cameras' newest estimates combined

`./gradlew test` drives an 8-second arc and checks that vision keeps the estimate closer to the
truth than odometry alone.

## Basic config

The smallest useful setup is one camera with its position on the robot. Everything else keeps its
default:

```java
VisionCamera camera =
    new PhotonVisionWrapper(
        new PhotonCamera("front"),
        new VisionCameraConfig()
            // The real camera, so simulation sees what it would.
            .withCameraModel(CameraModel.ARDUCAM_OV9281)
            // 12" forward of center, centered, 10" up, tilted 15° up.
            .withRobotToCamera(Inches.of(12), Inches.zero(), Inches.of(10),
                               Degrees.zero(), Degrees.of(-15), Degrees.zero()));

// Every loop:
camera.setRobotOrientation(gyro.getRotation2d());
camera.update();
camera.addVisionMeasurements(poseEstimator);

// In simulationPeriodic:
VisionSim.getDefault().update(simulatedTruePose);
```

[`Robot.java`](src/main/java/first/robot/Robot.java) does the same for three cameras through a
`VisionSystem`, with a heading-assisted Limelight (MegaTag2) and a tighter tag distance limit on
the Synapse camera.
