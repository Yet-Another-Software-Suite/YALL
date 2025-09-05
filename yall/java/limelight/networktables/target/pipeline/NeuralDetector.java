package limelight.networktables.target.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Neural Detector Pipeline Result extracted from JSON Output
 */
public class NeuralDetector
{

  /**
   * Human-readable class name string
   */
  @JsonProperty("class")
  public String className;

  /**
   * ClassID integer
   */
  @JsonProperty("classID")
  public double classID;

  /**
   * Confidence of the predicition
   */
  @JsonProperty("conf")
  public double confidence;

  /**
   * The size of the target as a percentage of the image (0-1)
   */
  @JsonProperty("ta")
  public double ta;

  /**
   * X-coordinate of the center of the target in degrees relative to crosshair. Positive-right, center-zero
   */
  @JsonProperty("tx")
  public double tx;

  /**
   * Y-coordinate of the center of the target in degrees relative to crosshair. Positive-down, center-zero
   */
  @JsonProperty("ty")
  public double ty;

  /**
   * X-coordinate of the center of the target in pixels relative to crosshair. Positive-right, center-zero
   */
  @JsonProperty("txp")
  public double tx_pixels;

  /**
   * Y-coordinate of the center of the target in pixels relative to crosshair. Positive-down, center-zero
   */
  @JsonProperty("typ")
  public double ty_pixels;

  /**
   * 	X-coordinate of the center of the target in degrees relative to principal piexel. Positive-right, center-zero
   */
  @JsonProperty("tx_nocross")
  public double tx_nocrosshair;

  /**
   * Y-coordinate of the center of the target in degrees relative to principal pixel. Positive-right, center-zero
   */
  @JsonProperty("ty_nocross")
  public double ty_nocrosshair;

  /**
   * Creates a new instance of this class.
   */
  public NeuralDetector()
  {
  }
}
