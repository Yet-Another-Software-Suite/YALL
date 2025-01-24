package limelight.structures;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Results Wrapper Class for JSON reading
 */
public class LimelightResultsJson
{

  /**
   * "Results" Object for JSON reading.
   */
  @JsonProperty("Results")
  public LimelightResults results;

  public LimelightResultsJson()
  {
    results = new LimelightResults();
  }
}
