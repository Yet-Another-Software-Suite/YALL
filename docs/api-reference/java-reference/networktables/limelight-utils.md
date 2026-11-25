# LimelightUtils

**Package:** `limelight.networktables`

Static conversion helpers between WPILib geometry/units types and the flat `double[]` arrays the Limelight NetworkTables API uses. Used internally throughout YALL; useful directly if you're reading raw NT entries yourself.

***

## Methods

| Method                                                           | Returns         | Description                                                                                                                        |
| ---------------------------------------------------------------- | --------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `static toPose3D(double[] inData)`                               | `Pose3d`        | Converts a 6-element `[x, y, z, roll, pitch, yaw]` array (meters, degrees) into a `Pose3d`. Empty `Pose3d` if `inData.length < 6`. |
| `static toPose2D(double[] inData)`                               | `Pose2d`        | Same input format; keeps only `x`, `y`, and `yaw`.                                                                                 |
| `static toTranslation3d(double[] translation)`                   | `Translation3d` | Converts a 3-element `[x, y, z]` array (meters) into a `Translation3d`.                                                            |
| `static toOrientation(double[] orientation)`                     | `Orientation3d` | Converts a 6-element `[yaw, yawrate, pitch, pitchrate, roll, rollrate]` array (degrees) into an `Orientation3d`.                   |
| `static orientation3dToArray(Orientation3d orientation)`         | `double[]`      | Inverse of `toOrientation`.                                                                                                        |
| `static pose3dToArray(Pose3d pose)`                              | `double[]`      | Converts a `Pose3d` into `[x, y, z, roll, pitch, yaw]` (meters, degrees).                                                          |
| `static pose2dToArray(Pose2d pose)`                              | `double[]`      | Converts a `Pose2d` into `[x, y, 0, 0, 0, yaw]` (meters, degrees).                                                                 |
| `static translation3dToArray(Translation3d translation)`         | `double[]`      | Converts a `Translation3d` into `[x, y, z]` (meters).                                                                              |
| `static extractArrayEntry(double[] inData, int position)`        | `double`        | Returns `inData[position]`, or `0` if `position` is out of bounds. Used to safely decode variable-length NT arrays.                |
| `static sanitizeName(String name)`                               | `String`        | Returns `name`, or `"limelight"` if `name` is `null`/empty.                                                                        |
| `static getLimelightURLString(String tableName, String request)` | `URL`           | Builds `http://<tableName>.local:5807/<request>` for HTTP calls like snapshot capture.                                             |

## See Also

* [PoseEstimate](pose-estimate.md)
* [Orientation3d](orientation3d.md)
