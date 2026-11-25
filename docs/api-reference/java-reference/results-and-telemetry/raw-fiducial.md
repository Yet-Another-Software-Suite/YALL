# RawFiducial

**Package:** `limelight.results`

Per-tag detail decoded from the raw NetworkTables `rawfiducials` entry, via [`LimelightData.getRawFiducials()`](../networktables/limelight-data.md). Also populated inside [`PoseEstimate.rawFiducials`](../networktables/pose-estimate.md) for the tags that contributed to a pose estimate.

***

## Fields

| Field          | Type     | Description                                            |
| -------------- | -------- | ------------------------------------------------------ |
| `id`           | `int`    | AprilTag ID.                                           |
| `txnc`, `tync` | `double` | Principal-pixel-relative offset in the image.          |
| `ta`           | `double` | Target area as a percentage of the image.              |
| `distToCamera` | `double` | Distance from the tag to the camera, in meters.        |
| `distToRobot`  | `double` | Distance from the tag to the robot, in meters.         |
| `ambiguity`    | `double` | Pose ambiguity, `0` to `1`. Lower is more trustworthy. |

## Usage

`ambiguity` is the standard signal for rejecting unreliable single-tag poses — see [`PoseEstimate.getMinTagAmbiguity()`](../networktables/pose-estimate.md) for a ready-made aggregate.

## See Also

* [PoseEstimate](../networktables/pose-estimate.md)
* [AprilTagFiducial](../target-types/apriltag-fiducial.md)
