# Barcode

**Package:** `limelight.networktables.target`

A single decoded barcode/QR/DataMatrix detection from the JSON `Barcode` array in [`LimelightResults`](../networktables/limelight-results.md).

***

## Fields

| Field                              | Type         | Description                                                                               |
| ---------------------------------- | ------------ | ----------------------------------------------------------------------------------------- |
| `family`                           | `String`     | Barcode family, e.g. `"QR"`, `"DataMatrix"` (`fam`).                                      |
| `data`                             | `String`     | Decoded content of the barcode.                                                           |
| `ta`                               | `double`     | Target area as a percentage of the image, 0-1.                                            |
| `tx`, `ty`                         | `double`     | Crosshair-relative offset in degrees.                                                     |
| `tx_pixels`, `ty_pixels`           | `double`     | Crosshair-relative offset in pixels.                                                      |
| `tx_nocrosshair`, `ty_nocrosshair` | `double`     | Principal-pixel-relative offset in degrees.                                               |
| `corners`                          | `double[][]` | Pixel-space corner coordinates `[x0,y0,x1,y1,...]`. Must be enabled in the LL output tab. |

***

## Methods

| Method        | Returns  | Description       |
| ------------- | -------- | ----------------- |
| `getFamily()` | `String` | Returns `family`. |

## See Also

* [LimelightResults](../networktables/limelight-results.md)
