package limelight.networktables.target.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Neural Classifier Pipeline Result extracted from JSON Output
 */
public class NeuralClassifier
{

  /**
   * Neural pipeline class name from Limelight.
   */
  @JsonProperty("class")
  public String className;

  /**
   * Neural pipeline class id from the Limelight.
   */
  @JsonProperty("classID")
  public double classID;

  /**
   * Confidence that the object is the class.
   */
  @JsonProperty("conf")
  public double confidence;

  @JsonProperty("zone")
  public double zone;

  /**
   * X position of the object in the image as percent.
   */
  @JsonProperty("tx")
  public double tx;

  /**
   * X position of the object in the image as pixel.
   */
  @JsonProperty("txp")
  public double tx_pixels;

  /**
   * Y position of the object in the image as percent.
   */
  @JsonProperty("ty")
  public double ty;

  /**
   * Y position of the object in the image as pixel
   */
  @JsonProperty("typ")
  public double ty_pixels;

  /**
   * Creates a new NeuralClassifier object.
   */
  public NeuralClassifier()
  {
  }
}
