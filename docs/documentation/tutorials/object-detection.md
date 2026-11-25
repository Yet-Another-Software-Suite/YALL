# Object Detection

This tutorial reads neural classifier results, the pipeline type used for game-piece detection (e.g. spotting a piece on the field before intaking it).

## 1. Switch to a classifier or detector pipeline

Configure the correct pipeline index in the Limelight web UI, or switch it from code:

```java
limelight.getSettings().withPipelineIndex(1).save();
```

## 2. Read results from the JSON payload

`getLatestResults()` returns the full frame, including whichever target arrays are populated for the active pipeline:

```java
limelight.getLatestResults().ifPresent((LimelightResults result) -> {
    for (NeuralClassifier object : result.targets_Classifier) {
        if (object.className.equals("algae") && object.confidence > 0.7) {
            // valid detection: object.tx / object.ty give position in the image
        }
    }
});
```

`targets_Classifier` is empty unless the active pipeline is a neural classifier pipeline. Check `result.pipeline_type` if you're unsure which pipeline produced a frame.

## 3. Neural Detector: bounding boxes instead of whole-frame labels

If your pipeline is a **detector** (bounding boxes) rather than a **classifier** (whole-frame/zone label), read `targets_Detector` instead. It's the same `LimelightResults` object, just a different array:

```java
limelight.getLatestResults().ifPresent((LimelightResults result) -> {
    for (NeuralDetector object : result.targets_Detector) {
        if (object.className.equals("coral")) {
            // object.tx / object.ty are crosshair-relative degrees, like an AprilTag result
            // object.ta is the bounding box area as a percentage of the image
        }
    }
});
```

## 4. Faster path: raw detections without JSON parsing

If you only need position and don't need `className`/`confidence`, `LimelightData.getRawDetections()` decodes the `rawdetections` NetworkTables entry directly, including the four bounding-box corners, which the JSON `NeuralDetector` result doesn't expose:

```java
for (RawDetection detection : limelight.getData().getRawDetections()) {
    // detection.classId, detection.txnc, detection.tync, detection.ta
    // detection.corner0_X .. detection.corner3_Y (pixel coordinates)
}
```

## Next steps

* [Target & Pipeline Types](../understanding/target-and-pipeline-types.md): every target type and what pipeline produces it.
* [Results JSON vs. Raw NetworkTables](../understanding/results-json-vs-raw-networktables.md): when to use `getLatestResults()` vs. the raw accessors.
* [Tuning a Neural Network Pipeline](../tuning/neural-network-pipeline-tuning.md): confidence threshold and crop window.
