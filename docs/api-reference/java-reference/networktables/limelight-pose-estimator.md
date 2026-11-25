# LimelightPoseEstimator

**Package:** `limelight.networktables`

Fetches field-relative robot pose estimates from a [`Limelight`](../limelight.md), using either the MegaTag1 or MegaTag2 pipeline. Created via `limelight.createPoseEstimator(EstimationMode)`.

```java
LimelightPoseEstimator poseEstimator = limelight.createPoseEstimator(EstimationMode.MEGATAG2);
Optional<PoseEstimate> estimate = poseEstimator.getPoseEstimate();
```

***

## Constructor

```java
LimelightPoseEstimator(Limelight camera, EstimationMode estimationMode)
```

Prefer `limelight.createPoseEstimator(mode)` over calling this directly.

***

## Methods

| Method                        | Returns                  | Description                                                                                                           |
| ----------------------------- | ------------------------ | --------------------------------------------------------------------------------------------------------------------- |
| `getPoseEstimate()`           | `Optional<PoseEstimate>` | Blue-origin pose estimate, using MegaTag1 or MegaTag2 depending on the estimator's `EstimationMode`.                  |
| `getAlliancePoseEstimate()`   | `Optional<PoseEstimate>` | Pose estimate in the coordinate frame matching `DriverStation.getAlliance()`. Empty if alliance color is unavailable. |
| `getBotPose()` _(deprecated)_ | `Pose3d`                 | Legacy MegaTag1 `botpose` entry, not alliance- or origin-aware. Prefer `getPoseEstimate()`.                           |

***

## EstimationMode

| Value      | Description                                                                                                                                                                                                                                                    |
| ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `MEGATAG1` | Estimates pose from a single AprilTag using only visual data. No external state required.                                                                                                                                                                      |
| `MEGATAG2` | Fuses AprilTag detections with robot heading for smoother, more accurate localization — especially at long range or oblique angles. Requires calling [`LimelightSettings.withRobotOrientation`](limelight-settings.md) every loop before reading the estimate. |

***

## BotPose

Internal enum mapping each `EstimationMode`/alliance combination to its NetworkTables entry name. Exposed publicly for direct access without going through a `LimelightPoseEstimator`:

```java
Optional<PoseEstimate> estimate = BotPose.BLUE_MEGATAG2.get(limelight);
```

| Value                           | NT Entry              | MegaTag2 |
| ------------------------------- | --------------------- | -------- |
| `RED`                           | `botpose_wpired`      | No       |
| `RED_MEGATAG2`                  | `botpose_orb_wpired`  | Yes      |
| `BLUE` _(recommended)_          | `botpose_wpiblue`     | No       |
| `BLUE_MEGATAG2` _(recommended)_ | `botpose_orb_wpiblue` | Yes      |

Each `BotPose` value caches one `PoseEstimate` per `Limelight` it's queried with, keyed by camera name + entry, so repeated calls don't allocate.

## See Also

* [PoseEstimate](pose-estimate.md)
* [LimelightSettings#withRobotOrientation](limelight-settings.md)
* [Limelight](../limelight.md)
