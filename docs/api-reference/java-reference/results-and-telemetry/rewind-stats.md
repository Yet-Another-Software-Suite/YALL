# RewindStats

**Package:** `limelight.results`

Rewind buffer status, from the JSON `rewind` object in [`LimelightResults`](../networktables/limelight-results.md). Supported hardware only (Limelight 4).

***

## Fields

| Field            | Type     | Description                                                                       |
| ---------------- | -------- | --------------------------------------------------------------------------------- |
| `bufferUsage`    | `double` | Buffer usage fraction, `0.0` to `1.0`.                                            |
| `enabled`        | `int`    | `1` if rewind recording is enabled, `0` if paused.                                |
| `flushing`       | `int`    | `1` if the buffer is currently flushing to disk.                                  |
| `frameCount`     | `int`    | Number of frames currently in the buffer.                                         |
| `latencyPenalty` | `int`    | Latency penalty added by rewind recording, in microseconds (typically 500µs-1ms). |
| `storedSeconds`  | `double` | Seconds of video currently held in the buffer.                                    |

***

## Methods

| Method                | Returns   | Description                          |
| --------------------- | --------- | ------------------------------------ |
| `getLatencyPenalty()` | `Time`    | `latencyPenalty` as a WPILib `Time`. |
| `getBufferTime()`     | `Time`    | `storedSeconds` as a WPILib `Time`.  |
| `getEnabled()`        | `boolean` | `true` if `enabled == 1`.            |
| `isFlushing()`        | `boolean` | `true` if `flushing == 1`.           |
| `getFrameCount()`     | `int`     | Returns `frameCount`.                |

## See Also

* [Limelight#rewindCapture](../limelight.md)
* [LimelightSettings#withRewindEnable](../networktables/limelight-settings.md)
