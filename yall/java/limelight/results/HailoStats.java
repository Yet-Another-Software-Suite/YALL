package limelight.results;

import static edu.wpi.first.units.Units.Watts;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.units.measure.Power;

/**
 * Stats for Hailo accelerator module. (Sold separately, only used on the LL4)
 */
public class HailoStats
{

  /**
   * 1 if Hailo accelerator detected, 0 otherwise
   */
  @JsonProperty("present")
  public int    present;
  /**
   * Hailo device type string
   */
  @JsonProperty("type")
  public String type;
  /**
   * Hailo temperature (Celsius)
   */
  @JsonProperty("temp")
  public double temp;
  /**
   * Hailo power consumption (watts)
   */
  @JsonProperty("power")
  public double power;
  /**
   * Throttle capability enabled
   */
  @JsonProperty("throttle")
  public int    throttle;

  /**
   * Creates a new HailoStats object.
   */
  public HailoStats() {}

  /**
   * Get the power consumption of the Hailo accelerator.
   *
   * @return {@link Power} object representing the power consumption of the Hailo accelerator.
   */
  public Power getPower()
  {
    return Watts.of(power);
  }

  public boolean getEnabled()
  {
    return throttle == 1;
  }
}
