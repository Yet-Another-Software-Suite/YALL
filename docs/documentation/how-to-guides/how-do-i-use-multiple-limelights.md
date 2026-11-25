# How do I use multiple Limelights?

## Give each camera a unique NT name

Rename each camera in its web UI (Settings tab → team number/name) so their NetworkTables tables don't collide, e.g. `limelight-front` and `limelight-back`.

## Create one Limelight object per camera

```java
Limelight front = new Limelight("limelight-front");
Limelight back  = new Limelight("limelight-back");
```

Each `Limelight`, `LimelightSettings`, `LimelightPoseEstimator`, and `PoseEstimate` instance is bound to its own camera. Nothing is shared or global, so there's no special coordination needed beyond keeping the objects around (usually as subsystem fields).

## Fusing pose estimates from multiple cameras

Feed every camera's filtered `PoseEstimate` into the same drivetrain pose estimator. WPILib's pose estimators accept multiple `addVisionMeasurement` calls per loop from different sources:

```java
for (Limelight ll : List.of(front, back)) {
    ll.getSettings().withRobotOrientation(currentOrientation).save();
    ll.createPoseEstimator(EstimationMode.MEGATAG2).getPoseEstimate().ifPresent(estimate -> {
        if (estimate.tagCount > 0 && estimate.getMinTagAmbiguity() < 0.3) {
            poseEstimator.addVisionMeasurement(estimate.pose.toPose2d(), estimate.timestampSeconds);
        }
    });
}
```

Give each camera an accurate, distinct `withCameraOffset`. This is what lets pose estimates from physically different mounting points agree with each other once fused.

## See Also

* [AprilTag Pose Estimation tutorial](../tutorials/apriltag-pose-estimation.md)
* [Pose Estimation & Ambiguity](../understanding/pose-estimation-and-ambiguity.md)
