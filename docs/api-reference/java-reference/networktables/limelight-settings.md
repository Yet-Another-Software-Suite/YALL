# LimelightSettings

**Package:** `limelight.networktables`

Chainable configuration object for a [`Limelight`](../limelight.md). Fetched once via `limelight.getSettings()` and reused — every `.withXXXX()` call writes to NetworkTables and returns `this` for chaining. Most writes take effect on the Limelight immediately; `save()` forces an NT flush but is rarely required.

```java
limelight.getSettings()
         .withLimelightLEDMode(LEDMode.PipelineControl)
         .withCameraOffset(Pose3d.kZero)
         .save();
```

{% hint style="info" %}
Settings are write-only. The constructor fetches current values from the Limelight, but there are no getters — `LimelightSettings` exists purely as an attachment point for `.withXXXX()` calls.
{% endhint %}

***

## Methods

| Method                                                                     | Description                                                                                                                                     |
| -------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `withLimelightLEDMode(LEDMode mode)`                                       | Sets the LED mode.                                                                                                                              |
| `withPipelineIndex(int index)`                                             | Switches the active pipeline (0-9).                                                                                                             |
| `withPriorityTagId(int aprilTagId)`                                        | Sets the AprilTag ID MegaTag should prioritize when multiple tags are visible.                                                                  |
| `withStreamMode(StreamMode mode)`                                          | Sets the camera stream layout (standard / picture-in-picture).                                                                                  |
| `withCropWindow(double minX, double maxX, double minY, double maxY)`       | Sets the processing crop window, `-1` to `1` per axis. The crop window in the web UI must be fully open for this to take effect.                |
| `withImuMode(ImuMode mode)`                                                | Sets how the Limelight 4's internal IMU is fused with externally-submitted robot orientation for MegaTag2.                                      |
| `withImuAssistAlpha(double alpha)`                                         | Convergence strength (default `0.001`) for internal-IMU-assist modes.                                                                           |
| `withRobotOrientation(Orientation3d orientation)`                          | Submits robot heading + angular velocity for MegaTag2. **Required every loop when using `EstimationMode.MEGATAG2`.** Calls `save()` internally. |
| `withFiducialDownscalingOverride(DownscalingOverride downscalingOverride)` | Overrides AprilTag detection downscaling. Higher downscale trades detection range for performance.                                              |
| `withAprilTagOffset(Translation3d offset)`                                 | Sets the 3D point-of-interest offset for the active fiducial pipeline.                                                                          |
| `withAprilTagIdFilter(List<Integer> idFilter)`                             | Restricts MegaTag/fiducial tracking to the given AprilTag IDs.                                                                                  |
| `withCameraOffset(Pose3d offset)`                                          | Sets the camera's pose relative to robot center (meters + `Rotation3d`). Required for accurate MegaTag pose estimation.                         |
| `withKeystone(double horizontal, double vertical)`                         | Keystone perspective correction for the crop window, `-0.95` to `0.95` per axis.                                                                |
| `withThrottle(double throttle)`                                            | Skips `throttle` frames between processed frames. Recommended `100-200` while disabled to reduce thermal output.                                |
| `withRewindEnable(RewindState state)`                                      | Enables or pauses rewind buffer recording (supported hardware only).                                                                            |
| `rewindCapture(double durationSeconds)`                                    | Triggers a rewind capture (max 165s, rate-limited to once per 2s).                                                                              |
| `save()`                                                                   | Flushes the default `NetworkTableInstance` immediately.                                                                                         |

***

## Enums

### LEDMode

`PipelineControl` · `ForceOff` · `ForceBlink` · `ForceOn` · `ForceLeftOn` · `ForceRightOn` · `BounceHalves` · `ForceLeftBlink` · `ForceRightBlink`

### StreamMode

`Standard` (side-by-side) · `PictureInPictureMain` (secondary in corner) · `PictureInPictureSecondary` (main in corner)

### DownscalingOverride

`Pipeline` (0, pipeline-controlled) · `NoDownscale` (1) · `HalfDownscale` (1.5) · `DoubleDownscale` (2) · `TripleDownscale` (3) · `QuadrupleDownscale` (4)

### ImuMode

| Value                       | Behavior                                                                               |
| --------------------------- | -------------------------------------------------------------------------------------- |
| `ExternalImu`               | Uses only the yaw submitted via `withRobotOrientation`. Internal IMU ignored entirely. |
| `SyncInternalImu`           | Uses external yaw; internal IMU is kept synced to it.                                  |
| `InternalImu`               | Uses the internal IMU exclusively; ignores external orientation updates.               |
| `InternalImuMT1Assist`      | Uses the internal IMU, corrected continuously by filtered MegaTag1 yaw estimates.      |
| `InternalImuExternalAssist` | Uses the internal IMU, corrected continuously by the externally-submitted IMU.         |

### RewindState

`DISABLED` · `ENABLED`

## See Also

* [Limelight](../limelight.md)
* [LimelightPoseEstimator](limelight-pose-estimator.md)
* [Orientation3d](orientation3d.md)
