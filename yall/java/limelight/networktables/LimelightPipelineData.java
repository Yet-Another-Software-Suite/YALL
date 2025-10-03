package limelight.networktables;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import limelight.Limelight;

/**
 * Pipeline data for {@link Limelight}.
 */
public class LimelightPipelineData {

  /**
   * {@link NetworkTable} for the {@link Limelight}
   */
  private NetworkTable limelightTable;
  /**
   * {@link Limelight} to fetch data for.
   */
  private Limelight limelight;
  /**
   * Pipeline processing latency contribution.
   */
  private NetworkTableEntry processingLatencyEntry;
  /**
   * Pipeline capture latency.
   */
  private NetworkTableEntry captureLatencyEntry;
  /**
   * Current pipeline index.
   */
  private NetworkTableEntry pipelineIndexEntry;
  /**
   * Current pipeline type
   */
  private NetworkTableEntry pipelineTypeEntry;

  /**
   * Construct data for pipelines.
   *
   * @param camera {@link Limelight} to use.
   */
  public LimelightPipelineData(Limelight camera) {
    limelight = camera;
    limelightTable = limelight.getNTTable();
    processingLatencyEntry = limelightTable.getEntry("tl");
    captureLatencyEntry = limelightTable.getEntry("cl");
    pipelineIndexEntry = limelightTable.getEntry("getpipe");
    pipelineTypeEntry = limelightTable.getEntry("getpipetype");
  }

  /**
   * Gets the pipeline's processing latency contribution.
   *
   * @return Pipeline latency in milliseconds
   */
  public double getProcessingLatencyEntry() {
    return processingLatencyEntry.getDouble(0.0);
  }

  /**
   * Gets the capture latency.
   *
   * @return Capture latency in milliseconds
   */
  public double getCaptureLatencyEntry() {
    return captureLatencyEntry.getDouble(0.0);
  }

  /**
   * Gets the active pipeline index.
   *
   * @return Current pipeline index (0-9)
   */
  public double getCurrentPipelineIndex() {
    return pipelineIndexEntry.getDouble(0);
  }

  /**
   * Gets the current pipeline type.
   *
   * @return Pipeline type string (e.g. "retro", "apriltag", etc)
   */
  public String getCurrentPipelineType() {
    return pipelineTypeEntry.getString("");
  }

  public NetworkTableEntry getCurrentPipelineIndexEntry() {
    return pipelineIndexEntry;
  }
}
