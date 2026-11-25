---
icon: rocket
---

# Why YALL?

## The problem with LimelightHelpers

LimelightVision publishes [`LimelightHelpers`](https://github.com/LimelightVision/limelightlib-wpijava) as a single drop-in `.java` file, a set of static methods keyed by raw NetworkTables entry names (`ledMode`, `pipeline_set`, `botpose_wpiblue`, `rawfiducials`, ...). It's simple, but that simplicity has costs:

* Every call re-fetches an NT entry, so no typed object holds settings or results for you.
* MegaTag1 vs. MegaTag2, alliance color, and coordinate frame are separate helper methods, easy to mix up.
* Results decode into loose arrays and parallel static fields, not classes.
* There's no chaining, since configuring a camera means many independent one-line calls with no obvious grouping.
* AprilTag ambiguity, standard deviations, and per-tag detail live in different places depending on which method you called.

## What YALL replaces it with

YALL models the same underlying NetworkTables API as **one object per camera**, with typed sub-objects for every concern:

```java
Limelight limelight = new Limelight("limelight");

limelight.getSettings()
         .withLimelightLEDMode(LEDMode.PipelineControl)
         .withCameraOffset(cameraOffset)
         .save();

LimelightPoseEstimator poseEstimator = limelight.createPoseEstimator(EstimationMode.MEGATAG2);
```

* **`Limelight`**: one per physical camera, resolves and validates its NT table by name.
* **`LimelightSettings`**: every writable setting as a chainable `.withXXXX()` call.
* **`LimelightPoseEstimator`** / **`PoseEstimate`**: MegaTag1/MegaTag2 pose, alliance-aware, with per-tag ambiguity already decoded for filtering.
* **`LimelightResults`**: the full JSON frame, with typed arrays for every pipeline type instead of parallel loose fields.
* **`LimelightData`**: direct, cheap NetworkTables reads (raw fiducials/detections, barcodes, Python I/O) for callers who don't need the full JSON parse.

## MegaTag2 done correctly

MegaTag2 requires the robot to submit its current heading to the Limelight every loop _before_ reading a pose estimate, a step that's easy to get wrong or forget with raw NetworkTables calls. YALL makes the requirement explicit through the API:

```java
limelight.getSettings()
         .withRobotOrientation(new Orientation3d(gyro.getRotation3d(),
                                                  new AngularVelocity3d(DegreesPerSecond.of(gyro.getPitchVelocity()),
                                                                        DegreesPerSecond.of(gyro.getRollVelocity()),
                                                                        DegreesPerSecond.of(gyro.getYawVelocity()))))
         .save();

Optional<PoseEstimate> visionEstimate = limelight.createPoseEstimator(EstimationMode.MEGATAG2).getPoseEstimate();
```

`PoseEstimate` carries `tagCount`, `avgTagDist`, `avgTagArea`, and per-tag `RawFiducial` ambiguity so you can gate `addVisionMeasurement` calls without hand-rolling the array decoding yourself. See [Pose Estimation & Ambiguity](understanding/pose-estimation-and-ambiguity.md).

## One API, every pipeline type

Whatever pipeline is active (AprilTag, retroreflective, neural classifier, neural detector, or barcode), `LimelightResults` exposes it as a typed array (`targets_Fiducials`, `targets_Retro`, `targets_Classifier`, `targets_Detector`, `targets_Barcode`) instead of a grab-bag of loosely related fields. See [Target & Pipeline Types](understanding/target-and-pipeline-types.md).

## Raw data when you need speed

`getLatestResults()` parses a full JSON payload with Jackson on every call. When a periodic loop only needs tag or detection geometry, `LimelightData.getRawFiducials()` / `getRawDetections()` read the equivalent NetworkTables double-array entries directly, with no JSON parsing at all. See [Results JSON vs. Raw NetworkTables](understanding/results-json-vs-raw-networktables.md).

## Simulation without hardware

`LimelightHelpers` has no simulation story: without a physical camera on the network, every NT entry it reads is empty. YALL ships `LimelightSim`, which projects the current season's AprilTags into a simple camera model from your simulated robot pose and publishes the same NetworkTables keys real hardware would. Code written against `Limelight`, `LimelightData`, and `LimelightPoseEstimator` runs unchanged in desktop simulation. See [Simulating a Limelight](tutorials/simulating-a-limelight.md).

The `Field2d` raycast visualization that makes this easy to watch in action was contributed by FRC Team 9738.

## Java and Python, one model

YALL ships a Python port alongside the Java library with the same class names and (mostly) the same method names, so teams that write robot code in Python aren't stuck hand-rolling their own wrapper. See [Python & C++ Support](understanding/python-and-cpp-support.md).

The Python port was originally written by FRC Team 9738 and merged into YALL for the whole community to use.
