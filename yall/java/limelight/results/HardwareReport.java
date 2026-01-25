package limelight.results;

import static edu.wpi.first.units.Units.Celsius;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.units.measure.Temperature;

/**
 * Hardware report for the limelight (2026+)
 */
public class HardwareReport
{
  /**
   * Camera sensor id
   */
  @JsonProperty("cid")
  public String cameraId;
  /**
   * CPU usage percentage
   */
  @JsonProperty("cpu")
  public double cpuUsage;
  /**
   * Disk free space (MB)
   */
  @JsonProperty("dfree")
  public double diskFree;
  /**
   * Disk total space (MB)
   */
  @JsonProperty("dtot")
  public double diskTotal;
  /**
   * RAM usage percentage
   */
  @JsonProperty("ram")
  public double ramUsage;
  /**
   * CPU temperature in Celsius
   */
  @JsonProperty("temp")
  public double temperature;
  /**
   * Hailo stats
   */
  @JsonProperty("hailo")
  public HailoStats hailoStats;

  /**
   * Hardware Report Constructor
   */
  public HardwareReport()
  {
  }

  /**
   * Get the CPU temperature in Celsius.
   *
   * @return {@link Temperature} of the CPU.
   */
  public Temperature getTemp()
  {
    return Celsius.of(temperature);
  }

}
