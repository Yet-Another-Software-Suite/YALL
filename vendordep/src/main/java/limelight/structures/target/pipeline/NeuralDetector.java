package limelight.structures.target.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Represents a Neural Detector Pipeline Result extracted from JSON Output */
public class NeuralDetector {

  @JsonProperty("class")
  public String className;

  @JsonProperty("classID")
  public double classID;

  @JsonProperty("conf")
  public double confidence;

  @JsonProperty("ta")
  public double ta;

  @JsonProperty("tx")
  public double tx;

  @JsonProperty("ty")
  public double ty;

  @JsonProperty("txp")
  public double tx_pixels;

  @JsonProperty("typ")
  public double ty_pixels;

  @JsonProperty("tx_nocross")
  public double tx_nocrosshair;

  @JsonProperty("ty_nocross")
  public double ty_nocrosshair;

  public NeuralDetector() {}
}
