# LimelightSettings & Pipelines

`LimelightSettings` (from `limelight.getSettings()`) is the only way to change how a Limelight behaves from robot code. Every `.withXXXX()` call writes directly to a NetworkTables entry the Limelight watches. There's no local caching or batching, so changes normally apply within a frame or two. `save()` forces an immediate NT flush but rarely matters since individual writes already propagate quickly.

## Settings are write-only

The constructor fetches current values from the Limelight, but `LimelightSettings` exposes no getters. This is intentional: it exists purely as an attachment point for chained `.withXXXX()` calls, not as a settings mirror. If you need to know the _current_ pipeline or LED state, read it back through [`LimelightData`](results-json-vs-raw-networktables.md) / `LimelightPipelineData` instead.

## The settings groups

| Concern                    | Methods                                                                                                                   |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| **LEDs**                   | `withLimelightLEDMode`                                                                                                    |
| **Pipeline**               | `withPipelineIndex`, `withPriorityTagId`, `withAprilTagIdFilter`, `withFiducialDownscalingOverride`, `withAprilTagOffset` |
| **Camera geometry**        | `withCameraOffset`                                                                                                        |
| **Robot state (MegaTag2)** | `withRobotOrientation`, `withImuMode`, `withImuAssistAlpha`                                                               |
| **Stream / crop**          | `withStreamMode`, `withCropWindow`, `withKeystone`                                                                        |
| **Thermal management**     | `withThrottle`                                                                                                            |
| **Rewind (LL4)**           | `withRewindEnable`, `rewindCapture`                                                                                       |

## Pipelines are indices, not objects

YALL doesn't model pipelines as objects. A pipeline is just an index (0-9) configured in the Limelight web UI. `withPipelineIndex(n)` switches which pipeline is active; everything about _what that pipeline does_ (its type, whether AprilTag, retroreflective, classifier, or detector; its crop; its color thresholds) is configured in the Limelight UI itself, not from robot code. Robot code reacts to whatever the active pipeline produces, via [target types](target-and-pipeline-types.md).

## LED mode vs. pipeline LED behavior

`LEDMode.PipelineControl` (the default recommendation) lets each pipeline decide its own LED behavior, configured per-pipeline in the web UI. The `ForceXXX` modes override every pipeline's LED behavior robot-wide, useful for driver signaling (e.g. force-blink LEDs when a game piece is detected) independent of which pipeline is active.

## See Also

* [Basic Setup tutorial](../tutorials/basic-setup.md)
* [How do I choose between MegaTag1 and MegaTag2?](../how-to-guides/how-do-i-choose-megatag1-vs-megatag2.md)
* LimelightSettings reference
