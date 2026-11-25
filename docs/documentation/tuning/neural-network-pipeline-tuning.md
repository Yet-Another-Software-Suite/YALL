# Tuning a Neural Network Pipeline

Neural classifier and detector pipelines expose far fewer tuning knobs than retroreflective or AprilTag pipelines. Most of the "tuning" work is in the model itself (see Limelight's training documentation for classifiers/detectors), not the pipeline settings. In the web UI, there are two levers worth adjusting:

## Confidence threshold

Both pipeline types expose a **confidence threshold** slider. Raising it rejects lower-confidence detections/classifications outright, giving fewer false positives, but you'll also start missing genuine borderline detections. There's no universal value; raise it until false positives on your specific game pieces/field elements stop, then back off slightly to keep recall reasonable.

`VisionTarget.confidence()` on detected objects (and LimelightLib's `ClassifierTarget.confidence` for classifiers, through `getCamera().getLatestResults()`) reflects whatever passed this threshold. You can still apply an additional, stricter threshold in robot code if you want different behavior per-mode (e.g. stricter during autonomous) without touching the pipeline itself.

## Crop window

Both pipeline types support a **crop window**, restricting inference to a sub-region of the frame:

* For a **detector**, this lets you ignore objects outside a zone you care about (e.g. only the field in front of an intake).
* For a **classifier**, this reduces how much irrelevant background the model has to reason about, which usually improves classification accuracy directly.

`getCamera().setCropWindowOverride(minX, maxX, minY, maxY)` on a `LimelightWrapper` sets the same crop from robot code if you need it to change dynamically (e.g. widening the zone once a game piece is expected to be closer) rather than fixed per-pipeline in the web UI.

## Hardware prerequisite

Neural pipelines require a coprocessor (e.g. Google Coral) connected via the Limelight's USB-A port. If detections never show up in `VisionResult.getDetectedObjects()` regardless of threshold, confirm the accelerator is connected before assuming it's a tuning problem.

## See Also

* [Tuning Workflow Overview](tuning-workflow-overview.md)
* [Results Model](../understanding/results-model.md#what-each-vendor-reports): which Limelight results YALL maps to `VisionTarget`; classifier results stay in LimelightLib.
* [Object Detection tutorial](../tutorials/object-detection.md)
