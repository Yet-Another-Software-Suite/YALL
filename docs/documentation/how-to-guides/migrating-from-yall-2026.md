---
icon: route
---

# Migrating from YALL 2026

YALL 2026 was a Limelight-only library: a `limelight` package that read a Limelight's NetworkTables directly, plus a `LimelightSim`. YALL for 2027 is one camera API for PhotonVision, Limelight and Synapse, built on each maker's own library. This page maps the old code to the new.

The old library is kept at the [`pre-rewrite` tag](https://github.com/Yet-Another-Software-Suite/YALL/tree/pre-rewrite) if you need to look something up.

## What changes when you install

{% stepper %}
{% step %}
### Move the project to WPILib 2027

YALL now builds against WPILib 2027 and runs on the SystemCore. WPILib's classes moved to `org.wpilib.*` packages; WPILib's own 2027 docs list the changes.

**Check it:** your project builds on WPILib 2027 before you add YALL.
{% endstep %}

{% step %}
### Install the new vendordeps

Remove the old `yall.json`, then install the new YALL vendordep, PhotonLib, WPILib's **CameraServer** and **AprilTag** vendordeps, and **LimelightLib 2** for your Limelights. [How do I install YALL?](how-do-i-install-yall.md) has the links.

**Check it:** the project builds with the new vendordeps.
{% endstep %}

{% step %}
### Change the imports

Everything moved from the `limelight` package to `yall`, and the Limelight itself is now LimelightLib 2's `com.limelightvision.Limelight`. Replace the old imports with the ones in the table below.

**Check it:** no `import limelight.` lines are left.
{% endstep %}
{% endstepper %}

## Old to new

| YALL 2026 (`limelight` package) | YALL 2027 |
| --- | --- |
| `new Limelight("limelight")` | `new LimelightWrapper(new Limelight("limelight"), config)` ([`LimelightWrapper`](https://yall.yassrobotics.com/api-reference/java-reference/cameras/limelight-wrapper)); `Limelight` is LimelightLib 2's |
| `limelight.getSettings()...save()` | A [`VisionCameraConfig`](https://yall.yassrobotics.com/api-reference/java-reference/yall/vision-camera-config), passed to the wrapper. Settings YALL doesn't cover go to `camera.getCamera()` (LimelightLib 2's `Limelight`) |
| `withCameraOffset(Pose3d)` | `config.withRobotToCamera(forward, left, up, roll, pitch, yaw)`, or `camera.setRobotToCamera(...)` while running |
| `withRobotOrientation(Orientation3d)` every loop | `camera.setRobotOrientation(gyroRotation3d, yawRate)` every loop |
| `withPipelineIndex(index)` | `config.withPipelineIndex(index)`, or `camera.setPipelineIndex(index)` |
| `withLimelightLEDMode(LEDMode)` | `camera.getCamera().setLEDMode(LEDMode.FORCE_OFF)` (LimelightLib 2's `LEDMode`) |
| `withImuMode(ImuMode)`, `withImuAssistAlpha(alpha)` | `camera.getCamera().setIMUMode(...)`, `setIMUAssistAlpha(...)` |
| `withThrottle(throttle)` | `camera.getCamera().setThrottle(throttle)` |
| `withPriorityTagId(id)`, `withAprilTagIdFilter(ids)` | `camera.getCamera().setPriorityTagIDOverride(id)`, `setFiducialIDFiltersOverride(int[])` |
| `withCropWindow(...)`, `withKeystone(...)`, `withFiducialDownscalingOverride(...)`, `withAprilTagOffset(...)` | `camera.getCamera().setCropWindowOverride(...)`, `setKeystoneOverride(...)`, `setFiducialDownscalingOverride(...)`, `setFiducial3DOffsetOverride(...)` |
| `withRewindEnable(...)`, `rewindCapture(seconds)`, `snapshot(name)` | `camera.getCamera().setRewindEnabled(...)`, `triggerRewindCapture(seconds)`, `triggerSnapshot()` |
| `createPoseEstimator(EstimationMode.MEGATAG1)` | `config.withPoseEstimationMode(PoseEstimationMode.TAGS_ONLY)` (the default) |
| `createPoseEstimator(EstimationMode.MEGATAG2)` | `config.withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED)` |
| `poseEstimator.getPoseEstimate()` | `camera.getEstimatedPose()`: the newest estimate that passed the [pose filter](../understanding/pose-estimation-and-filtering.md), or `camera.addVisionMeasurements(poseEstimator)` for all of them |
| `PoseEstimate.pose`, `.timestampSeconds` | [`VisionPoseEstimate`](https://yall.yassrobotics.com/api-reference/java-reference/results/vision-pose-estimate)`.pose()`, `.timestampSeconds()` |
| `.tagCount`, `.avgTagDist`, `.rawFiducials` | `.tagCount()`, `.averageTagDistanceMeters()`, `.tagIds()` |
| `.getMinTagAmbiguity()` and your own trust checks | `.ambiguity()`, and the pose filter's checks do the rejecting for you |
| Your own standard deviation math | `.stdDevs()`, filled in by the pose filter |
| `getLatestResults()` / `LimelightResults` | `camera.getLatestResult()`: a [`VisionResult`](https://yall.yassrobotics.com/api-reference/java-reference/results/vision-result) |
| `AprilTagFiducial`, `NeuralDetector`, `RetroreflectiveTape` | [`VisionTarget`](https://yall.yassrobotics.com/api-reference/java-reference/results/vision-target): `fiducialId()`, `classId()`, `confidence()`, `yawDegrees()`, `pitchDegrees()`, `areaPercent()` |
| `getData().targetData.getHorizontalOffset()` (`tx`) | `camera.getBestTarget().get().yawDegrees()` (positive right, like `tx`) |
| `getData().targetData.getVerticalOffset()` (`ty`) | `.pitchDegrees()`: **positive up**. Limelight's per-target `ty` is positive down, so YALL's pitch is its negative |
| `NeuralClassifier`, barcodes, Python script data, hardware and IMU readings | `camera.getCamera()`: LimelightLib 2's `getLatestResults()`, `getPythonScriptData()`, `getHardwareData()`, `getIMUData()` |
| `LimelightSim` | [`VisionSim`](https://yall.yassrobotics.com/api-reference/java-reference/sim/vision-sim): [`VisionSim.getDefault().update(truePose)`](https://yall.yassrobotics.com/api-reference/java-reference/sim/vision-sim); every camera is simulated, with `config.withCameraModel(CameraModel.LIMELIGHT_4)` for its specs |
| `LimelightSimSettings` | `config.withCameraModel(...)` or `config.withCameraProperties(...)` |
| `LimelightUtils` | WPILib geometry (`Pose3d`, `Transform3d`) and LimelightLib 2's `Limelight.toPose3D(...)` |
| `Limelight.setupPortForwardingUSB(index)` | Not needed on a SystemCore: use `Limelight.SYSTEMCORE_USB0` to `SYSTEMCORE_USB3`. See [How do I use a USB Limelight?](how-do-i-use-a-usb-limelight.md) |

## Before and after

{% tabs %}
{% tab title="YALL 2026" %}
```java
Limelight limelight = new Limelight("limelight");
limelight.getSettings()
    .withLimelightLEDMode(LEDMode.PipelineControl)
    .withCameraOffset(new Pose3d(0.3, 0, 0.25, new Rotation3d(0, Math.toRadians(-15), 0)))
    .save();
LimelightPoseEstimator estimator = limelight.createPoseEstimator(EstimationMode.MEGATAG2);

// Every loop
limelight.getSettings().withRobotOrientation(orientation).save();
estimator.getPoseEstimate().ifPresent(estimate -> {
  if (estimate.tagCount > 0 && estimate.avgTagDist < 4) {
    poseEstimator.addVisionMeasurement(estimate.pose.toPose2d(), estimate.timestampSeconds);
  }
});
```
{% endtab %}

{% tab title="YALL 2027" %}
```java
VisionCamera camera =
    new LimelightWrapper(
        new Limelight("limelight"),
        new VisionCameraConfig()
            .withCameraModel(CameraModel.LIMELIGHT_4)
            .withRobotToCamera(Inches.of(12), Inches.zero(), Inches.of(10),
                               Degrees.zero(), Degrees.of(-15), Degrees.zero())
            .withPoseEstimationMode(PoseEstimationMode.HEADING_ASSISTED)); // MegaTag2
camera.getCamera().setLEDMode(LEDMode.PIPELINE_CONTROL);

// In Robot.robotPeriodic()
camera.setRobotOrientation(gyro.getRotation3d(), gyroYawRate);
camera.update();
camera.addVisionMeasurements(poseEstimator); // filtered, with standard deviations
```
{% endtab %}
{% endtabs %}

## What's different on purpose

* **The pose filter replaces your own checks.** Tag count, tag distance, ambiguity, field bounds and more are checked for you, and each check can be tuned or turned off. See [How do I tune the pose filter?](how-do-i-tune-the-pose-filter.md)
* **Standard deviations come with every estimate**, so the pose estimator knows how much to trust it. Measure your own with [Measuring Standard Deviations](../tutorials/measuring-standard-deviations.md).
* **Simulation uses PhotonVision's camera simulation** for every camera, not a Limelight-only simulator.
* **Python and C++:** see [Python & C++ Support](../understanding/python-and-cpp-support.md) for what each covers.

## Read more

* [Basic Setup](../tutorials/basic-setup.md)
* [How do I use vendor-specific settings?](how-do-i-use-vendor-specific-settings.md)
