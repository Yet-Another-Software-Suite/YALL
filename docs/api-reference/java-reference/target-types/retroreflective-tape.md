# RetroreflectiveTape

**Package:** `limelight.networktables.target`

A single retroreflective/color target detection from the JSON `Retro` array in [`LimelightResults`](../networktables/limelight-results.md). Field and pose accessor layout is identical to [`AprilTagFiducial`](apriltag-fiducial.md), minus tag ID/family.

***

## Fields

| Field                              | Type     | Description                                    |
| ---------------------------------- | -------- | ---------------------------------------------- |
| `ta`                               | `double` | Target area as a percentage of the image, 0-1. |
| `tx`, `ty`                         | `double` | Crosshair-relative offset in degrees.          |
| `tx_pixels`, `ty_pixels`           | `double` | Crosshair-relative offset in pixels.           |
| `tx_nocrosshair`, `ty_nocrosshair` | `double` | Principal-pixel-relative offset in degrees.    |
| `ts`                               | `double` | Timestamp in milliseconds from boot.           |

***

## Methods

| Method                                    | Returns           | Description                         |
| ----------------------------------------- | ----------------- | ----------------------------------- |
| `getCameraPose_TargetSpace()` / `...2D()` | `Pose3d`/`Pose2d` | Camera pose relative to the target. |
| `getRobotPose_FieldSpace()` / `...2D()`   | `Pose3d`/`Pose2d` | Robot pose in field space.          |
| `getRobotPose_TargetSpace()` / `...2D()`  | `Pose3d`/`Pose2d` | Robot pose relative to the target.  |
| `getTargetPose_CameraSpace()` / `...2D()` | `Pose3d`/`Pose2d` | Target pose relative to the camera. |
| `getTargetPose_RobotSpace()` / `...2D()`  | `Pose3d`/`Pose2d` | Target pose relative to the robot.  |

## See Also

* [LimelightResults](../networktables/limelight-results.md)
* [AprilTagFiducial](apriltag-fiducial.md)
