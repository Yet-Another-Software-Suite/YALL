# RawDetection

**Package:** `limelight.results`

Per-detection detail decoded from the raw NetworkTables `rawdetections` entry, via [`LimelightData.getRawDetections()`](../networktables/limelight-data.md). Includes the four corner points of the detection's bounding quadrilateral, which the JSON [`NeuralDetector`](../target-types/neural-detector.md) result does not expose.

***

## Fields

| Field                                                  | Type     | Description                                          |
| ------------------------------------------------------ | -------- | ---------------------------------------------------- |
| `classId`                                              | `int`    | Predicted class ID.                                  |
| `txnc`, `tync`                                         | `double` | Principal-pixel-relative offset in degrees.          |
| `ta`                                                   | `double` | Target area, 0-100% of image.                        |
| `corner0_X` .. `corner3_X`, `corner0_Y` .. `corner3_Y` | `double` | Pixel coordinates of the four bounding-quad corners. |

## See Also

* [NeuralDetector](../target-types/neural-detector.md)
* [LimelightData](../networktables/limelight-data.md)
