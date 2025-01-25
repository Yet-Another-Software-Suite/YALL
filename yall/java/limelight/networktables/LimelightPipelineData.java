package limelight.networktables;


import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import limelight.Limelight;

/**
 * Pipeline data for {@link Limelight}.
 */
public class LimelightPipelineData
{

  /**
   * {@link NetworkTable} for the {@link Limelight}
   */
  private NetworkTable      limelightTable;
  /**
   * {@link Limelight} to fetch data for.
   */
  private Limelight         limelight;
  /**
   * Pipeline processing latency contribution.
   */
  private NetworkTableEntry processingLatency;
  /**
   * Pipeline capture latency.
   */
  private NetworkTableEntry captureLatency;
  /**
   * Current pipeline index.
   */
  private NetworkTableEntry pipelineIndex;
  /**
   * Current pipeline type
   */
  private NetworkTableEntry pipelineType;

  /**
   * Construct data for pipelines.
   *
   * @param camera {@link Limelight} to use.
   */
  public LimelightPipelineData(Limelight camera)
  {
    limelight = camera;
    limelightTable = limelight.getNTTable();
    processingLatency = limelightTable.getEntry("tl");
    captureLatency = limelightTable.getEntry("cl");
    pipelineIndex = limelightTable.getEntry("getpipe");
    pipelineType = limelightTable.getEntry("getpipetype");
  }


  /**
   * Gets the pipeline's processing latency contribution.
   *
   * @return Pipeline latency in milliseconds
   */
  public double getProcessingLatency()
  {
    return processingLatency.getDouble(0.0);
  }

  /**
   * Gets the capture latency.
   *
   * @return Capture latency in milliseconds
   */
  public double getCaptureLatency()
  {
    return captureLatency.getDouble(0.0);
  }


  /**
   * Gets the active pipeline index.
   *
   * @return Current pipeline index (0-9)
   */
  public double getCurrentPipelineIndex()
  {
    return pipelineIndex.getDouble(0);
  }


  /**
   * Gets the current pipeline type.
   *
   * @return Pipeline type string (e.g. "retro", "apriltag", etc)
   */
  public String getCurrentPipelineType()
  {
    return pipelineType.getString("");
  }

}
