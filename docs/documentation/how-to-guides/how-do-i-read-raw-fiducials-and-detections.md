# How do I read raw fiducials and detections?

`getLatestResults()` parses a full JSON `limelight.results` payload with Jackson every call. If you only need tag/detection geometry and not the full frame, read the equivalent raw NetworkTables entries directly instead.

## Raw AprilTag detections

```java
for (RawFiducial tag : limelight.getData().getRawFiducials()) {
    // tag.id, tag.txnc, tag.tync, tag.ta
    // tag.distToCamera, tag.distToRobot (meters)
    // tag.ambiguity (0-1, lower is more trustworthy)
}
```

This is the same `RawFiducial` type populated in `PoseEstimate.rawFiducials`, since the pose estimator already uses this path internally.

## Raw neural detections

```java
for (RawDetection detection : limelight.getData().getRawDetections()) {
    // detection.classId, detection.txnc, detection.tync, detection.ta
    // detection.corner0_X .. detection.corner3_Y are the bounding-quad corners in pixels
}
```

`RawDetection` includes the four bounding-box corners, which the JSON `NeuralDetector` result does not expose.

## When to use the JSON path instead

Reach for `getLatestResults()` when you need:

* Multiple pipeline types' results at once (e.g. classifier _and_ fiducial in the same frame)
* Hardware/IMU telemetry (`HardwareReport`, `IMUResults`, `RewindStats`)
* Barcode or retroreflective results, since there's no raw-NT equivalent for those

## See Also

* [Results JSON vs. Raw NetworkTables](../understanding/results-json-vs-raw-networktables.md)
* [Object Detection tutorial](../tutorials/object-detection.md)
