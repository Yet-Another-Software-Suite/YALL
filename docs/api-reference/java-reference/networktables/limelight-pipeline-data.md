# LimelightPipelineData

**Package:** `limelight.networktables`

Pipeline status readouts for a [`Limelight`](../limelight.md), accessed via `limelight.getData().pipelineData`.

***

## Methods

| Method                      | Returns  | Description                                                                                     |
| --------------------------- | -------- | ----------------------------------------------------------------------------------------------- |
| `getProcessingLatency()`    | `double` | Pipeline processing latency contribution, in milliseconds (`tl`).                               |
| `getCaptureLatency()`       | `double` | Capture latency between end-of-exposure and start of the tracking loop, in milliseconds (`cl`). |
| `getCurrentPipelineIndex()` | `double` | Currently active pipeline index, 0-9 (`getpipe`).                                               |
| `getCurrentPipelineType()`  | `String` | Active pipeline type string, e.g. `"retro"`, `"apriltag"` (`getpipetype`).                      |

## See Also

* [LimelightData](limelight-data.md)
* [LimelightSettings#withPipelineIndex](limelight-settings.md)
