package frc.robot.limelight.structures.target.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Neural Classifier Pipeline Result extracted from JSON Output
 */
public class NeuralClassifier
{

  @JsonProperty("class")
  public String className;

  @JsonProperty("classID")
  public double classID;

  @JsonProperty("conf")
  public double confidence;

  @JsonProperty("zone")
  public double zone;

  @JsonProperty("tx")
  public double tx;

  @JsonProperty("txp")
  public double tx_pixels;

  @JsonProperty("ty")
  public double ty;

  @JsonProperty("typ")
  public double ty_pixels;

  public NeuralClassifier()
  {
  }
}
