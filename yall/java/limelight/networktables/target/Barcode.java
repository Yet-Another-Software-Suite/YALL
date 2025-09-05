package limelight.networktables.target;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Barcode Target Result extracted from JSON Output
 */
public class Barcode
{

  /**
   * Barcode family type (e.g. "QR", "DataMatrix", etc.)
   */
  @JsonProperty("fam")
  public String family;

  /**
   * Gets the decoded data content of the barcode
   */
  @JsonProperty("data")
  public String data;

  /**
   * X-coordinate of the center of the target in pixels relative to crosshair. Positive-right, center-zero
   */
  @JsonProperty("txp")
  public double tx_pixels;

  /**
   * 	Y-coordinate of the center of the target in pixels relative to crosshair. Positive-down, center-zero
   */
  @JsonProperty("typ")
  public double ty_pixels;

  /**
   * 	X-coordinate of the center of the target in degrees relative to crosshair. Positive-right, center-zero
   */
  @JsonProperty("tx")
  public double tx;

  /**
   * 	Y-coordinate of the center of the target in degrees relative to crosshair. Positive-down, center-zero
   */
  @JsonProperty("ty")
  public double ty;

  /**
   * X-coordinate of the center of the target in degrees relative to principal piexel. Positive-right, center-zero
   */
  @JsonProperty("tx_nocross")
  public double tx_nocrosshair;

  /**
   * Y-coordinate of the center of the target in degrees relative to principal pixel. Positive-right, center-zero
   */
  @JsonProperty("ty_nocross")
  public double ty_nocrosshair;

  /**
   * The size of the target as a percentage of the image (0-1)
   */
  @JsonProperty("ta")
  public double ta;

  /**
   * Corners array (pixels) [x0,y0,x1,y1.....]. Must be enabled in output tab
   */
  @JsonProperty("pts")
  public double[][] corners;

  /**
   * Creates a new Barcode Result
   */
  public Barcode()
  {
  }

  /**
   * Gets the barcode family type.
   *
   * @return Barcode family type (e.g. "QR", "DataMatrix", etc.)
   */
  public String getFamily()
  {
    return family;
  }
}
