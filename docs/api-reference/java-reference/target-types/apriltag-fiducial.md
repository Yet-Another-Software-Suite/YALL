# AprilTagFiducial

**Package:** `limelight.networktables.target`

A single AprilTag detection from the JSON `Fiducial` array in [`LimelightResults`](../networktables/limelight-results.md).

***

## Fields

| Field                              | Type     | Description                                    |
| ---------------------------------- | -------- | ---------------------------------------------- |
| `fiducialID`                       | `double` | Tag ID (`fID`).                                |
| `fiducialFamily`                   | `String` | Tag family, e.g. `"36H11C"` (`fam`).           |
| `ta`                               | `double` | Target area as a percentage of the image, 0-1. |
| `tx`, `ty`                         | `double` | Crosshair-relative offset in degrees.          |
| `tx_pixels`, `ty_pixels`           | `double` | Crosshair-relative offset in pixels.           |
| `tx_nocrosshair`, `ty_nocrosshair` | `double` | Principal-pixel-relative offset in degrees.    |
| `ts`                               | `double` | Timestamp in milliseconds from boot.           |

***

## Methods

Each pose pair below is computed by SolvePnP and exposed as both `Pose3d` and `Pose2d`:

| Method                                    | Returns           | Description                                              |
| ----------------------------------------- | ----------------- | -------------------------------------------------------- |
| `getCameraPose_TargetSpace()` / `...2D()` | `Pose3d`/`Pose2d` | Camera pose relative to the tag.                         |
| `getRobotPose_FieldSpace()` / `...2D()`   | `Pose3d`/`Pose2d` | Robot pose in field space, computed from this tag alone. |
| `getRobotPose_TargetSpace()` / `...2D()`  | `Pose3d`/`Pose2d` | Robot pose relative to the tag.                          |
| `getTargetPose_CameraSpace()` / `...2D()` | `Pose3d`/`Pose2d` | Tag pose relative to the camera.                         |
| `getTargetPose_RobotSpace()` / `...2D()`  | `Pose3d`/`Pose2d` | Tag pose relative to the robot.                          |

## See Also

* [LimelightResults](../networktables/limelight-results.md)
* [RawFiducial](../results-and-telemetry/raw-fiducial.md)
