# Yet Another Localization Library (YALL)

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
- **Several cameras.** `VisionSystem` updates every camera and hands each accepted estimate to
  your pose estimator, with its own timestamp and standard deviations.
- **Vendor pass-through.** `getCamera()` returns the vendor's own camera object, and
  `withVendorConfig(...)` passes vendor config objects through, as YAMS does for motor
  controllers.

Coming from the Limelight-only YALL 2026? See [MIGRATION.md](MIGRATION.md).

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

## Basic config

One camera feeding a drivetrain's pose estimator. Everything not set here keeps its default: the
current season's field, the default pose filter and `PoseEstimationMode.TAGS_ONLY`.

YALL doesn't use the command framework, so it works the same with Commands v2 (`TimedRobot`) and
Commands v3 (`OpModeRobot`). Put its calls in `Robot.java`: both robot classes have
`robotPeriodic()` and `simulationPeriodic()`. (Commands v3 mechanisms have no
`simulationPeriodic()`, so `Robot.java` is the one place that works for both.)

```java
import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Inches;

import org.photonvision.PhotonCamera;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.PhotonVisionWrapper;
import yall.sim.CameraModel;
import yall.sim.VisionSim;

public class Robot extends TimedRobot { // or OpModeRobot for Commands v3
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
  public void robotPeriodic() {
    // Run the scheduler here as usual (CommandScheduler or Scheduler), then:
    camera.setRobotOrientation(drivetrain.getHeading());
    camera.update();
    camera.addVisionMeasurements(drivetrain.getPoseEstimator());
  }

  @Override
  public void simulationPeriodic() {
    // The simulated cameras see the field from the robot's true pose.
    VisionSim.getDefault().update(drivetrain.getSimulatedPose());
  }
}
```

## Usage

### Creating cameras

Wrap each vendor's camera object, the way YAMS wraps motor controllers:

```java
VisionCamera front =
    new PhotonVisionWrapper(
        new PhotonCamera("front"),
        new VisionCameraConfig()
            // forward, left, up, then roll, pitch, yaw. Negative pitch tilts the camera up.
            .withRobotToCamera(Inches.of(12), Inches.zero(), Inches.of(12),
                               Degrees.zero(), Degrees.of(-10), Degrees.zero()));

VisionCamera back =
    new LimelightWrapper(
        new Limelight("limelight-back"),
        new VisionCameraConfig()
            .withRobotToCamera(Inches.of(-12), Inches.zero(), Inches.of(12),
                               Degrees.zero(), Degrees.of(-10), Degrees.of(180))
            .withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED)); // MegaTag2

VisionCamera side =
    new SynapseWrapper(
        new SynapseCamera("side"),
        new VisionCameraConfig()
            .withRobotToCamera(Inches.zero(), Inches.of(12), Inches.of(12),
                               Degrees.zero(), Degrees.zero(), Degrees.of(90)));
```

The camera offset is measured from the robot's center on the floor, in the same order as
Limelight's camera pose: forward, left, up, then roll, pitch, yaw. A `Transform3d` works too:
`withRobotToCamera(new Transform3d(...))`. For a camera on a turret, call
`camera.setRobotToCamera(...)` with either form as the turret moves.

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

Pass every estimate that passed the pose filter to your drivetrain's pose estimator with
`addVisionMeasurement`, once per loop after `update()`:

```java
front.setRobotOrientation(poseEstimator.getEstimatedPosition().getRotation(), gyro.getYawRate());
front.update();
front.forEachEstimatedPose(estimate ->
    poseEstimator.addVisionMeasurement(estimate.pose2d(), estimate.timestampSeconds(), estimate.stdDevs()));
```

`front.addVisionMeasurements(poseEstimator)` does the same for a WPILib pose estimator. Use the
estimate's own values:

- **`timestampSeconds()`, never the current time.** The pose estimator keeps a short history of
  odometry and applies the estimate where the robot was when the image was taken.
- **`stdDevs()`.** They grow with tag distance and shrink with tag count, so a close, multi-tag
  estimate pulls hard and a far, single-tag one barely moves the pose.
- **Every estimate, not just the newest.** A fast camera can produce more than one frame per loop.
  `getEstimatedPose()` returns only the newest, for checks like "do we see the robot's position?".

`PoseEstimationMode.TAGS_ONLY` (the default) solves the pose from the tags alone: Limelight
MegaTag1, PhotonVision's multi-tag result, Synapse's field pose. `HEADING_ASSISTED` uses the robot
heading from `setRobotOrientation`: Limelight MegaTag2 and PhotonVision's trig solve. Synapse
doesn't support it.

Give `setRobotOrientation` the robot's heading on the field, your pose estimator's rotation, not
the gyro's own reading: resetting the pose estimator (for an auto's starting pose, say) turns its
heading without turning the gyro. `setRobotOrientation(Rotation3d, AngularVelocity)` takes the
gyro's roll and pitch too.

### Several cameras

`VisionSystem` updates every camera and gathers their estimates:

```java
VisionSystem vision = new VisionSystem(front, back, side);

// In Robot.java, for Commands v2 or v3
@Override
public void robotPeriodic() {
  vision.setRobotOrientation(poseEstimator.getEstimatedPosition().getRotation(), gyro.getYawRate());
  vision.update();
  vision.addVisionMeasurements(poseEstimator); // every accepted estimate, oldest first
}
```

Give the pose estimator every camera's estimates one at a time; don't combine them first. Each goes
in with its own timestamp and standard deviations, so the pose estimator applies it against the
odometry from the moment its image was taken and trusts each camera as much as it should. The pose
estimator's pose is the robot's pose. For drivetrains with their own vision method, such as a YAMS
`SwerveDrive`, use `forEachEstimatedPose(...)`:

```java
vision.forEachEstimatedPose(estimate ->
    swerveDrive.addVisionMeasurement(estimate.pose2d(), estimate.timestampSeconds(), estimate.stdDevs()));
```

### Pose filtering

Every check is turned on and tuned with its `withX(...)` method. Pass `null` to turn it off, and read its setting back with `getX()`, which returns an `Optional`:

| Check | Default | Rejects an estimate when |
| --- | --- | --- |
| `withFieldBounds(margin)` | On, 0.5 m | the robot would be off the field |
| `withMaxHeightError(height)` | On, 0.5 m | the robot would float above or sink below the floor |
| `withMaxTilt(angle)` | Off | the robot would be rolled or pitched past the limit |
| `withMaxAmbiguity(ambiguity)` | On, 0.2 | a single-tag estimate is too ambiguous |
| `withMinSingleTagArea(percent)` | Off | a single-tag estimate's tag is too small in the image |
| `withMinTagCount(count)` | On, 1 | too few tags were used |
| `withMaxTagDistance(single, multi)` | On, 4 m / 8 m | the tags are too far away |
| `withMaxYawRate(rate)` | On, 720 °/s | the robot spun too fast (needs `setRobotOrientation`) |
| `withYawRateWindow(halfWindow)` | Off | the robot spun too fast at any point around the picture's time |
| `withMaxAge(age)` | On, 0.5 s | the image is too old, or timestamped in the future |
| `withMaxDistanceFromPose(pose, distance)` | Off | the estimate jumps too far from the current pose |

```java
new VisionCameraConfig()
    .withPoseFilter(new VisionPoseFilter()
        .withMaxAmbiguity(0.15)
        .withMaxTilt(Degrees.of(10))
        .withMaxYawRate(null) // off
        .withStdDevs(VecBuilder.fill(4, 4, 8), VecBuilder.fill(0.5, 0.5, 1), 30));
```

Accepted estimates get standard deviations of `base * (1 + distance² / 30)`, with a smaller base
for multi-tag estimates. `withStdDevScale(...)` and `withHighTagCountBoost(minTags, scale)` multiply
the x and y standard deviations, to trust vision more while lining up or when a guess used many
tags. `withPreEnableAlignment(RobotState::isEnabled, 2, Degrees.of(2))` lets the cameras snap the
pose, heading included, to the field while the robot is disabled: it skips the pose jump check and
needs at least 2 tags. `VisionPoseFilter.acceptAll()` starts with every check off.

### Simulation

Cameras join `VisionSim.getDefault()`, a PhotonVision `VisionSystemSim` holding the current field's
AprilTags. Update it with the robot's true pose from `Robot.simulationPeriodic()`, which both
`TimedRobot` (Commands v2) and `OpModeRobot` (Commands v3) have:

```java
// In Robot.java
@Override
public void simulationPeriodic() {
  vision.updateSimulation(drivetrainSim.getPose());
}
```

Tell each camera what it is with `withCameraModel(...)`, so the simulation uses its published
resolution, field of view and AprilTag frame rate:

| `CameraModel` | Resolution | Field of view (H × V) | AprilTag fps |
| --- | --- | --- | --- |
| `LIMELIGHT_2` | 1280x960 | 62.5° × 48.9° | 6 |
| `LIMELIGHT_2_PLUS` | 1280x960 | 62.5° × 48.9° | 7 |
| `LIMELIGHT_3` | 1280x960 | 62.5° × 48.9° | 20 |
| `LIMELIGHT_3A` | 1280x960 | 54.5° × 42° | 20 |
| `LIMELIGHT_3G` | 1280x800 | 82° × 56.2° | 20 |
| `LIMELIGHT_4` | 1280x800 | 82° × 56.2° | 40 |
| `LUMA_P1` | 1280x800 | 80° × 56° | 60 |
| `LUMA_P2` | 1280x800 | 80° × 56° | 60 |
| `THRIFTYCAM` | 1600x1304 | 60.7° × 51° | 70 |
| `THRIFTIEST_CAM` | 1280x800 | 82° × 56° | 120 |
| `ARDUCAM_OV9281` | 1280x800 | 70° × 47.3° | 100 |
| `ELP_FISHEYE` | 1280x960 | 115° × 99.4° | 20 |

The specs come from Limelight's, Luma's, The Thrifty Bot's, Arducam's, AndyMark's and ELP's published
pages:

- **Frame rates:** Limelights and Lumas run at the AprilTag rate their vendor publishes. USB
  cameras (ThriftyCam, Thriftiest Cam, Arducam) run at their full-resolution capture rate; lower it
  with `withCameraProperties(...)` if your coprocessor tracks AprilTags slower.
- **ThriftyCam:** its stock lens is specified as 71° × 48° on a different image size. On the
  ThriftyCam's sensor, its 4.1 mm focal length gives 60.7° × 51°.
- **Arducam OV9281 (B0332):** Arducam gives only the 70° horizontal field of view, so the vertical
  one follows from the 16:10 sensor. Arducam's manual says 120 fps and resellers say 100.
- **ELP fisheye (AndyMark am-5170):** its 180° fisheye lens can't be simulated, because
  PhotonVision simulates a pinhole lens. The model keeps the fisheye's magnification at the center
  of the image (about 407 px focal length at 1280x960), which a pinhole lens sees as 115° × 99.4°.
- **Latency:** no vendor publishes one, so these are estimates.

Without a model, a Limelight is simulated as a Limelight 4 and the others as an Arducam OV9281. For
other cameras, pass PhotonVision camera properties to `withCameraProperties(...)`. Add game pieces for object detection
with `VisionSim.getDefault().addVisionTargets(...)`.

To test how the robot copes with bad cameras, inject faults into the simulated world. They affect
every camera in it, and the seeds make them repeat exactly in unit tests:

```java
VisionSim sim = VisionSim.getDefault();
sim.injectBlackout(Timer.getMonotonicTimestamp(), Seconds.of(2)); // no frames for 2 seconds
sim.withDropoutProbability(0.1, 2027);                            // drop 10% of frames
sim.withFieldTagJitter(Millimeters.of(13), 2027);                 // tags up to about 1 cm off
sim.clearFaults();                                                // back to normal
```

### Vendor settings

`getCamera()` returns the vendor's camera object (`PhotonCamera`, `Limelight` or `SynapseCamera`)
for settings YALL doesn't cover. `LimelightWrapper` also accepts Limelight's `PoseEstimateConfig`
(Limelight's own filter and standard deviations replace YALL's) and `PipelineConfiguration`
through `withVendorConfig(...)`.

### Telemetry

Each camera publishes under `YALL/<camera name>` in NetworkTables. What it publishes depends on the
verbosity, set with `withTelemetry(new VisionTelemetryConfig().withVerbosity(...))`:

| Verbosity | Publishes |
| --- | --- |
| `LOW` | `connected`, `hasTargets`, `targetCount`, `acceptedPoses` |
| `MID` (default) | also `rejectedPoses`, `rejectionReason` and `latencyMillis` |
| `HIGH` | also the pose filter stats under `stats/` and every target's ID, yaw, pitch and area under `targets/` |

`withField(...)` turns single fields on or off, `withTablePath(...)` moves the table, and
`withTelemetry(false)` turns it off.

`withDataLog()` also logs the same fields to WPILib's `DataLogManager` under `YALL/<camera name>`,
and `withNetworkTables(false)` stops publishing to NetworkTables, such as at competition:

```java
DataLogManager.start();
DataLogManager.logNetworkTables(false); // the cameras log their own values

new VisionCameraConfig()
    .withTelemetry(new VisionTelemetryConfig().withDataLog());
```

### Pose filter stats

Each camera counts what its pose filter accepts and rejects (by check), and measures how spread
out the accepted poses are: `camera.getFilterStats()`, also published under `stats/` at `HIGH`
verbosity (or with `withField(VisionTelemetryField.FILTER_STATS, true)`). Hold the
robot still where it sees tags, set `stats/reset` to true, wait a few seconds, then read
`stats/stdDevs`: that is the camera's real standard deviation at that distance.
`stats/suggestedBaseStdDevs` turns it into the base for `VisionPoseFilter.withStdDevs(...)`.

Add your own checks with `withCustomCheck(name, estimate -> ...)`. The name is the rejection
reason, and the stats count its rejections under it.

### Measuring a turret's angle

A camera on a turret can measure the turret's angle from the tags it sees:
`camera.getCameraYawOnRobot(robotHeading)` is the camera's heading on the field (from the tags
alone) minus the robot's heading. Use it to check or zero the turret's encoder.

## Example

[`examples/kitbot`](examples/kitbot) is a simulated kitbot with one camera from each vendor. Its wheels slip, so
odometry alone drifts; drive it with a gamepad and compare the `Truth`, `Odometry` and `Robot`
poses on the Field widget. Its test drives an arc and checks that vision keeps the estimate closer
to the truth than odometry.

[`examples/yagsl-commands2`](examples/yagsl-commands2) and [`examples/yagsl-commands3`](examples/yagsl-commands3) are
YAGSL swerve robots, on Commands v2 and v3, with a PhotonVision camera on the front and a MegaTag2 Limelight on the
back. Every loop they give the cameras the drive's odometry heading and add each accepted estimate to the drive with
`addVisionMeasurement`. Both run in simulation.

## API documentation

Javadocs are at [https://Yet-Another-Software-Suite.github.io/YALL/javadocs/](https://yet-another-software-suite.github.io/YALL/javadocs/).
The repository keeps a copy in [`docs/javadocs`](docs/javadocs). CI updates it when a change to
the API lands on `main`, and warns on pull requests that change it. To update it yourself, run
`./gradlew updateDocsJavadocs` in `vendordep`.

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
