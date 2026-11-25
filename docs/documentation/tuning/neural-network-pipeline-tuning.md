---
icon: brain
---

# Tuning an Object Detection Pipeline

An object detection pipeline runs a trained **model** (a neural network) that has learned to spot game pieces. Most of the tuning is in training the model, not in the pipeline's settings.

* Tuning a PhotonVision camera: [About Object Detection](https://docs.photonvision.org/en/latest/docs/objectDetection/about-object-detection.html). It runs on coprocessors with a neural processor, such as the Orange Pi 5 and Rubik Pi 3, and has confidence and overlap (NMS) settings.
* Tuning a Limelight: [Getting Started with Neural Networks](https://docs.limelightvision.io/docs/docs-limelight/pipeline-neural/getting-started-with-neural-networks), for the Neural Detector and Neural Classifier pipelines.

On a Limelight there are two settings worth adjusting:

## Confidence threshold

Both pipeline types expose a **confidence threshold** slider. Raising it rejects lower-confidence detections/classifications outright, giving fewer false positives, but you'll also start missing genuine borderline detections. There's no universal value; raise it until false positives on your specific game pieces/field elements stop, then back off slightly to keep recall reasonable.

`VisionTarget.confidence()` on detected objects (and LimelightLib's `ClassifierTarget.confidence` for classifiers, through `getCamera().getLatestResults()`) reflects whatever passed this threshold. You can still apply an additional, stricter threshold in robot code if you want different behavior per-mode (for example stricter during autonomous) without touching the pipeline itself.

## Crop window

Both pipeline types support a **crop window**, restricting inference to a sub-region of the frame:

* For a **detector**, this lets you ignore objects outside a zone you care about (for example only the floor in front of an intake).
* For a **classifier**, this reduces how much irrelevant background the model has to reason about, which usually improves classification accuracy directly.

`getCamera().setCropWindowOverride(minX, maxX, minY, maxY)` on a `LimelightWrapper` sets the same crop from robot code if you need it to change dynamically (for example widening it once a game piece should be closer) rather than fixed per-pipeline in the web UI.

## Hardware you need

On a Limelight 1, 2 or 3, neural pipelines need a Google Coral USB accelerator plugged into the Limelight. If detections never show up in `VisionResult.getDetectedObjects()` regardless of threshold, confirm the accelerator is connected before assuming it's a tuning problem.

## Read more

* [Tuning Workflow Overview](tuning-workflow-overview.md)
* [Results Model](../understanding/results-model.md#what-each-vendor-reports): which Limelight results YALL maps to `VisionTarget`; classifier results stay in LimelightLib.
* [Object Detection tutorial](../tutorials/object-detection.md)
