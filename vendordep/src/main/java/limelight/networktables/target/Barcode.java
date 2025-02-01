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

  @JsonProperty("txp")
  public double tx_pixels;

  @JsonProperty("typ")
  public double ty_pixels;

  @JsonProperty("tx")
  public double tx;

  @JsonProperty("ty")
  public double ty;

  @JsonProperty("tx_nocross")
  public double tx_nocrosshair;

  @JsonProperty("ty_nocross")
  public double ty_nocrosshair;

  @JsonProperty("ta")
  public double ta;

  @JsonProperty("pts")
  public double[][] corners;

  public Barcode()
  {
  }

  public String getFamily()
  {
    return family;
  }
}
