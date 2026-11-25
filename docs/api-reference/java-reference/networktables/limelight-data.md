# LimelightData

**Package:** `limelight.networktables`

Read-only NetworkTables data access for a [`Limelight`](../limelight.md). Fetched via `limelight.getData()`. Holds two sub-objects — `targetData` ([`LimelightTargetData`](limelight-target-data.md)) and `pipelineData` ([`LimelightPipelineData`](limelight-pipeline-data.md)) — plus JSON results parsing and raw NT reads that aren't part of the JSON blob.

***

## Fields

| Field          | Type                    | Description                                                                           |
| -------------- | ----------------------- | ------------------------------------------------------------------------------------- |
| `targetData`   | `LimelightTargetData`   | 2D/3D target metrics. See [LimelightTargetData](limelight-target-data.md).            |
| `pipelineData` | `LimelightPipelineData` | Pipeline latency/index/type. See [LimelightPipelineData](limelight-pipeline-data.md). |

***

## Methods

| Method                                 | Returns                      | Description                                                                                                                                                               |
| -------------------------------------- | ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `getResults()`                         | `Optional<LimelightResults>` | Parses the `json` NT entry into a [`LimelightResults`](limelight-results.md). Empty if the LL GUI's "Send JSON over NT?" option is off or nothing has been published yet. |
| `getRawFiducials()`                    | `RawFiducial[]`              | Decodes the `rawfiducials` double array (7 values per tag) into typed structs.                                                                                            |
| `getRawDetections()`                   | `RawDetection[]`             | Decodes the `rawdetections` double array (12 values per detection) into typed structs.                                                                                    |
| `getCamera2Robot()`                    | `Pose3d`                     | Camera pose relative to the robot, from `camerapose_robotspace`.                                                                                                          |
| `getClassifierClass()`                 | `String`                     | Current neural classifier result class name (`tcclass`).                                                                                                                  |
| `getDetectorClass()`                   | `String`                     | Primary neural detector result class name (`tdclass`).                                                                                                                    |
| `getBarcodeData()`                     | `String[]`                   | Decoded barcode strings (`rawbarcodes`).                                                                                                                                  |
| `getPythonData()`                      | `double[]`                   | Output array from a custom Python SnapScript pipeline (`llpython`).                                                                                                       |
| `setPythonData(double[] outgoingData)` | `void`                       | Writes input data for a custom Python SnapScript pipeline (`llrobot`).                                                                                                    |
| `takeSnapshot()`                       | `void`                       | Increments the `snapshot` counter to trigger a rising-edge snapshot. Rate-limited to once per 10 frames.                                                                  |

***

## Raw NetworkTables vs. JSON Results

`getRawFiducials()` / `getRawDetections()` read individual NT entries directly and are cheaper per-call than `getResults()`, which parses a full JSON payload with Jackson every call. Use the raw accessors in a tight periodic loop when you only need tag/detection geometry; use `getResults()` when you need the full frame — target arrays for every pipeline type, hardware/IMU telemetry, or multiple bot-pose variants at once.

## See Also

* [LimelightResults](limelight-results.md)
* [RawFiducial](../results-and-telemetry/raw-fiducial.md)
* [RawDetection](../results-and-telemetry/raw-detection.md)
* [LimelightTargetData](limelight-target-data.md)
