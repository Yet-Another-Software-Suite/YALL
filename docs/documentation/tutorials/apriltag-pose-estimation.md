# AprilTag Pose Estimation

This tutorial fuses Limelight MegaTag2 vision measurements into a WPILib pose estimator, the way `DrivebaseSubsystem` does in YALL's [example project](https://github.com/BroncBotz3481/YALL/tree/main/example).

## 1. Set up the camera offset once

MegaTag needs to know where the camera sits relative to robot center. Do this at construction time:

```java
Pose3d cameraOffset = new Pose3d(Inches.of(5).in(Meters),
                                  Inches.of(5).in(Meters),
                                  Inches.of(5).in(Meters),
                                  Rotation3d.kZero);

limelight = new Limelight("limelight");
limelight.getSettings()
         .withLimelightLEDMode(LEDMode.PipelineControl)
         .withCameraOffset(cameraOffset)
         .save();

poseEstimator = limelight.createPoseEstimator(EstimationMode.MEGATAG2);
```

## 2. Submit robot orientation every loop

MegaTag2 fuses AprilTag detections with your robot's current heading. This only works if you tell the Limelight your heading **every periodic loop, before reading a pose estimate**:

```java
@Override
public void periodic() {
    limelight.getSettings()
             .withRobotOrientation(new Orientation3d(gyro.getRotation3d(),
                                                      new AngularVelocity3d(DegreesPerSecond.of(0),
                                                                            DegreesPerSecond.of(0),
                                                                            DegreesPerSecond.of(0))))
             .save();
    // ...
}
```

{% hint style="warning" %}
Skip this step and MegaTag2 pose estimates will be inaccurate or stale. This is the single most common mistake when adopting MegaTag2. See [How do I choose between MegaTag1 and MegaTag2?](../how-to-guides/how-do-i-choose-megatag1-vs-megatag2.md)
{% endhint %}

## 3. Read and filter the pose estimate

```java
Optional<PoseEstimate> visionEstimate = poseEstimator.getPoseEstimate();
visionEstimate.ifPresent((PoseEstimate poseEstimate) -> {
    // Reject long-range or ambiguous reads before fusing.
    if (poseEstimate.avgTagDist < 4
        && poseEstimate.tagCount > 0
        && poseEstimate.getMinTagAmbiguity() < 0.3) {
        poseEstimator.addVisionMeasurement(poseEstimate.pose.toPose2d(), poseEstimate.timestampSeconds);
    }
});
```

`poseEstimator` above is your WPILib `SwerveDrivePoseEstimator` / `DifferentialDrivePoseEstimator`, a different object than YALL's `LimelightPoseEstimator`. Filtering on tag count, distance, and ambiguity before calling `addVisionMeasurement` keeps a single noisy or distant reading from corrupting your odometry.

## Alternative: the BotPose enum directly

If you don't need a `LimelightPoseEstimator` instance around, `BotPose` gives the same data directly:

```java
Optional<PoseEstimate> estimate = BotPose.BLUE_MEGATAG2.get(limelight);
```

Or fetch the alliance-relative estimate without checking `DriverStation` yourself:

```java
Optional<PoseEstimate> estimate = poseEstimator.getAlliancePoseEstimate();
```

## Next steps

* [Pose Estimation & Ambiguity](../understanding/pose-estimation-and-ambiguity.md): what each `PoseEstimate` field means and how to tune your filter thresholds.
* [How do I mount a Limelight on a moving mechanism?](../how-to-guides/how-do-i-mount-a-limelight-on-a-moving-mechanism.md): updating `withCameraOffset` dynamically for a turret.
* [How do I filter AprilTags by ID?](../how-to-guides/how-do-i-filter-apriltags-by-id.md)
* [Simulating a Limelight](simulating-a-limelight.md): exercise this fusion code in desktop simulation without hardware.
* [AprilTag & MegaTag2 Localization Tuning](../tuning/apriltag-and-megatag2-tuning.md): IMU modes, assist alpha, and the setup mistakes that silently break MegaTag2.
* [ChArUco Camera Calibration](../tuning/charuco-camera-calibration.md): improves the accuracy of every pose this tutorial produces.
