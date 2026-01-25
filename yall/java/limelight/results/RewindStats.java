package limelight.results;

import static edu.wpi.first.units.Units.Microseconds;
import static edu.wpi.first.units.Units.Seconds;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.units.measure.Time;

/**
 * Rewind buffer stats object (supported hardware only).
 */
public class RewindStats
{

  /**
   * Buffer usage (0.0 to 1.0)
   */
  @JsonProperty("bufferUsage")
  public double bufferUsage;
  /**
   * 1 if rewind recording is enabled, 0 if paused
   */
  @JsonProperty("enabled")
  public int    enabled;
  /**
   * 1 if currently flushing to disk, 0 otherwise
   */
  @JsonProperty("flushing")
  public int    flushing;
  /**
   * Number of frames in buffer
   */
  @JsonProperty("frameCount")
  public int    frameCount;
  /**
   * Rewind has a latency penalty of 500us-1ms. This latency penalty is listed in the results json rewind object as
   * "latpen" in integer microseconds
   */
  @JsonProperty("latpen")
  public int    latencyPenalty;
  /**
   * Seconds of video in buffer
   */
  @JsonProperty("storedSeconds")
  public double storedSeconds;

  /**
   * Creates a new RewindStats object.
   */
  public RewindStats()
  {
  }

  /**
   * Get the latency penalty as a {@link Time} object.
   *
   * @return Latency penalty as a {@link Time} object.
   */
  public Time getLatencyPenalty()
  {
    return Microseconds.of(latencyPenalty);
  }

  /**
   * Get the buffer {@link Time}.
   *
   * @return {@link Time} of the buffer storage.
   */
  public Time getBufferTime()
  {
    return Seconds.of(storedSeconds);
  }

  /**
   * Get the enable status of the rewind buffer.
   *
   * @return True if enabled, false otherwise.
   */
  public boolean getEnabled()
  {
    return enabled == 1;
  }

  /**
   * Is the buffer currently flushing to disk?
   *
   * @return Buffer flushing status.
   */
  public boolean isFlushing()
  {
    return flushing == 1;
  }

  /**
   * Get the number of frames in the buffer.
   *
   * @return Frame count.
   */
  public int getFrameCount()
  {
    return frameCount;
  }
}
