# Results JSON vs. Raw NetworkTables

YALL gives you two different paths to the same underlying camera data, and picking the right one for your use case matters for loop timing.

## The JSON path: `getLatestResults()`

`limelight.getLatestResults()` (equivalently `limelight.getData().getResults()`) reads the `json` NetworkTables entry and deserializes it with Jackson into a `LimelightResults` object: one object holding **everything** the Limelight published for that frame, including every target type array, hardware/IMU telemetry, every bot-pose variant, latency figures, and more.

This only works if the Limelight GUI's **Output & Crosshair → Send JSON over NT?** option is enabled. If it's off, `getLatestResults()` always returns `Optional.empty()`.

Reach for this when you need:

* Multiple target/pipeline types at once
* Hardware telemetry (`HardwareReport`, `IMUResults`, `RewindStats`, `HailoStats`)
* Barcode or retroreflective results (no raw-NT equivalent exists for these)

Cost: a full JSON parse on every call. Fine for most periodic loops, but avoid calling it more than once per loop iteration. Cache the `Optional<LimelightResults>` locally if multiple parts of your code need it in the same cycle.

## The raw path: `LimelightData` direct reads

`LimelightData` (from `limelight.getData()`) also exposes several values as direct NetworkTables reads, bypassing JSON entirely:

| Method                                        | Backing NT entry        |
| --------------------------------------------- | ----------------------- |
| `getRawFiducials()`                           | `rawfiducials`          |
| `getRawDetections()`                          | `rawdetections`         |
| `getBarcodeData()`                            | `rawbarcodes`           |
| `getCamera2Robot()`                           | `camerapose_robotspace` |
| `getClassifierClass()` / `getDetectorClass()` | `tcclass` / `tdclass`   |

`LimelightTargetData` (`limelight.getData().targetData`) goes further: `tx`, `ty`, target area, target count, and every relative pose (`robot2Target`, `camera2Target`, etc.) are all direct scalar/array NT reads, with no JSON involved at all.

Reach for these when:

* You only need tag/detection geometry, not the full frame
* You're polling every loop and want to avoid repeated JSON parsing
* You need bounding-box corners (`RawDetection`), which the JSON `NeuralDetector` result doesn't include

## Rule of thumb

Raw NetworkTables reads for tight, latency-sensitive loops (aiming, MegaTag pose fusion). JSON results when you need the full picture or a target type only the JSON exposes.

## See Also

* [How do I read raw fiducials and detections?](../how-to-guides/how-do-i-read-raw-fiducials-and-detections.md)
* LimelightData reference
* LimelightResults reference
