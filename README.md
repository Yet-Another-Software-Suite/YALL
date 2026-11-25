# Yet Another Limelight Library (YALL)

YALL gives PhotonVision, Limelight and Synapse cameras one Java API for FRC robots. A camera
object returns real results on the robot and simulated results in simulation, so the same code
runs in both. Every camera is simulated with PhotonVision's camera simulation, whatever its vendor.

YALL builds against WPILib 2027 (alpha 8).

## Features

- **One camera API.** Read targets (yaw, pitch, area, AprilTag ID, object class and confidence),
  robot pose estimates, the pipeline and the connection state the same way for every vendor.
- **Simulation for every vendor.** Cameras give PhotonVision-simulated results in simulation,
  with lens projection, noise and latency.
- **Pose filtering.** Every estimate goes through a `VisionPoseFilter`. Each check in it can be
  turned on, tuned or turned off, and accepted estimates get standard deviations scaled by tag
  distance and tag count.
- **Multi-camera fusion.** `VisionSystem` feeds every camera's estimates to a WPILib pose
  estimator, or combines them into one fused pose.
- **Vendor pass-through.** `getCamera()` returns the vendor's own camera object, and
  `withVendorConfig(...)` passes vendor config objects through, as YAMS does for motor
  controllers.

## Installation

Install these vendordeps:

1. YALL: `https://Yet-Another-Software-Suite.github.io/YALL/yall.json`
2. PhotonLib, which YALL simulates with.
3. WPILib's **CameraServer** and **AprilTag** vendordeps, which ship with WPILib. PhotonVision's
   simulation needs them.
4. The library of each camera vendor you use:
   - Limelight: LimelightLib 2 (`https://limelightvision.github.io/limelightlib-public/LimelightLib-alpha7.json`)
   - Synapse: SynapseLib (`https://danpeled.github.io/Synapse/SynapseLib.json`)

You only need a vendor library for cameras you construct.

## Usage

### Creating cameras

Wrap each vendor's camera object, the way YAMS wraps motor controllers:

```java
VisionCamera front =
    new PhotonVisionWrapper(
        new PhotonCamera("front"),
        new VisionCameraConfig()
            .withRobotToCamera(new Transform3d(0.3, 0, 0.3, new Rotation3d(0, Math.toRadians(-10), 0))));

VisionCamera back =
    new LimelightWrapper(
        new Limelight("limelight-back"),
        new VisionCameraConfig()
            .withRobotToCamera(new Transform3d(-0.3, 0, 0.3, new Rotation3d(0, Math.toRadians(-10), Math.PI)))
            .withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED)); // MegaTag2

VisionCamera side =
    new SynapseWrapper(
        new SynapseCamera("side"),
        new VisionCameraConfig()
            .withRobotToCamera(new Transform3d(0, 0.3, 0.3, new Rotation3d(0, 0, Math.PI / 2))));
```

### Reading targets

Call `update()` once per loop, then read what it found:

```java
front.update();
front.getBestTarget().ifPresent(target -> {
  double yaw = target.yawDegrees();   // positive right
  double pitch = target.pitchDegrees(); // positive up
  int tagId = target.fiducialId();     // -1 if not an AprilTag
});

for (VisionTarget object : front.getLatestResult().map(VisionResult::getDetectedObjects).orElse(List.of())) {
  // object.classId(), object.confidence()
}
```

### Estimating the robot pose

`getEstimatedPose()` returns the newest estimate that passed the pose filter, with its timestamp
and standard deviations:

```java
front.setRobotOrientation(gyro.getRotation3d(), gyro.getYawRate());
front.update();
front.getEstimatedPose().ifPresent(estimate ->
    poseEstimator.addVisionMeasurement(estimate.pose2d(), estimate.timestampSeconds(), estimate.stdDevs()));
```

`PoseEstimationMode.TAGS_ONLY` (the default) solves the pose from the tags alone: Limelight
MegaTag1, PhotonVision's multi-tag result, Synapse's field pose. `HEADING_ASSISTED` uses the gyro
heading from `setRobotOrientation`: Limelight MegaTag2 and PhotonVision's trig solve. Synapse
doesn't support it.

### Several cameras

`VisionSystem` updates every camera and gathers their estimates:

```java
VisionSystem vision = new VisionSystem(front, back, side);

@Override
public void robotPeriodic() {
  vision.setRobotOrientation(gyro.getRotation3d(), gyro.getYawRate());
  vision.update();
  vision.addVisionMeasurements(poseEstimator); // every accepted estimate, oldest first
}
```

`addVisionMeasurements` is the most accurate way to use several cameras: the pose estimator weighs
each estimate at the moment its image was taken. For one combined answer, `getFusedPose()`
averages the cameras' newest estimates, weighting each by its standard deviations. For
drivetrains with their own vision method, use `forEachEstimatedPose(...)`.

### Pose filtering

Every check can be turned on and tuned with `withX(...)`, or turned off with `withoutX()`:

| Check | Default | Rejects an estimate when |
| --- | --- | --- |
| `withFieldBounds(margin)` | On, 0.5 m | the robot would be off the field |
| `withMaxHeightError(height)` | On, 0.5 m | the robot would float above or sink below the floor |
| `withMaxTilt(angle)` | Off | the robot would be rolled or pitched past the limit |
| `withMaxAmbiguity(ambiguity)` | On, 0.2 | a single-tag estimate is too ambiguous |
| `withMinTagCount(count)` | On, 1 | too few tags were used |
| `withMaxTagDistance(single, multi)` | On, 4 m / 8 m | the tags are too far away |
| `withMaxYawRate(rate)` | On, 720 °/s | the robot spun too fast (needs `setRobotOrientation`) |
| `withMaxAge(age)` | On, 0.5 s | the image is too old, or timestamped in the future |
| `withMaxDistanceFromPose(pose, distance)` | Off | the estimate jumps too far from the current pose |

```java
new VisionCameraConfig()
    .withPoseFilter(new VisionPoseFilter()
        .withMaxAmbiguity(0.15)
        .withMaxTilt(Degrees.of(10))
        .withoutMaxYawRate()
        .withStdDevs(VecBuilder.fill(4, 4, 8), VecBuilder.fill(0.5, 0.5, 1), 30));
```

Accepted estimates get standard deviations of `base * (1 + distance² / 30)`, with a smaller base
for multi-tag estimates. `VisionPoseFilter.acceptAll()` starts with every check off.

### Simulation

Cameras join `VisionSim.getDefault()`, a PhotonVision `VisionSystemSim` holding the current field's
AprilTags. Update it with the robot's true pose:

```java
@Override
public void simulationPeriodic() {
  vision.updateSimulation(drivetrainSim.getPose());
}
```

Set each camera's resolution, field of view, frame rate and latency with
`withCameraProperties(...)`, or keep the vendor's default: a Limelight 4 for Limelights, an Arducam
OV9281 for PhotonVision, a 1280x720 USB camera for Synapse. Add game pieces for object detection
with `VisionSim.getDefault().addVisionTargets(...)`.

### Vendor settings

`getCamera()` returns the vendor's camera object (`PhotonCamera`, `Limelight` or `SynapseCamera`)
for settings YALL doesn't cover. `LimelightWrapper` also accepts Limelight's `PoseEstimateConfig`
(Limelight's own filter and standard deviations replace YALL's) and `PipelineConfiguration`
through `withVendorConfig(...)`.

### Telemetry

Each camera publishes its accepted and rejected poses, the last rejection reason, its target count,
latency and connection state under `YALL/<camera name>` in NetworkTables. `VisionSystem` publishes
`YALL/fusedPose`.

## Example

[`example/`](example) is a simulated kitbot with one camera from each vendor. Its wheels slip, so
odometry alone drifts; drive it with a gamepad and compare the `Truth`, `Odometry` and `Robot`
poses on the Field widget. Its test drives an arc and checks that vision keeps the estimate closer
to the truth than odometry.

## API documentation

Javadocs are at [https://Yet-Another-Software-Suite.github.io/YALL/javadocs/](https://yet-another-software-suite.github.io/YALL/javadocs/).

## Contributing

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature-name`).
3. Commit your changes (`git commit -am 'Add new feature'`).
4. Push to the branch (`git push origin feature-name`).
5. Open a pull request to the main repository.

Please include tests for new features. Run them with `./gradlew test` in `vendordep/`.

## Credits

Thanks to the PhotonVision project, whose simulation YALL uses for every camera, and to FRC Team
9738 for the original YALL Python port and the Limelight raycast visualization.

## License

YALL is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
