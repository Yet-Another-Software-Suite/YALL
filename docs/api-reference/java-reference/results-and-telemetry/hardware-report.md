# HardwareReport

**Package:** `limelight.results`

System-level hardware telemetry (2026+ firmware), from the JSON `hw` object in [`LimelightResults`](../networktables/limelight-results.md).

***

## Fields

| Field         | Type         | Description                                                |
| ------------- | ------------ | ---------------------------------------------------------- |
| `cameraId`    | `String`     | Camera sensor ID (`cid`).                                  |
| `cpuUsage`    | `double`     | CPU usage percentage.                                      |
| `diskFree`    | `double`     | Free disk space, in MB.                                    |
| `diskTotal`   | `double`     | Total disk space, in MB.                                   |
| `ramUsage`    | `double`     | RAM usage percentage.                                      |
| `temperature` | `double`     | CPU temperature, in Celsius.                               |
| `hailoStats`  | `HailoStats` | Hailo accelerator stats. See [HailoStats](hailo-stats.md). |

***

## Methods

| Method      | Returns       | Description                              |
| ----------- | ------------- | ---------------------------------------- |
| `getTemp()` | `Temperature` | `temperature` as a WPILib `Temperature`. |

## See Also

* [HailoStats](hailo-stats.md)
* [LimelightResults](../networktables/limelight-results.md)
