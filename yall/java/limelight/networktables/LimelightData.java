package limelight.networktables;


import static limelight.networktables.LimelightUtils.extractArrayEntry;
import static limelight.networktables.LimelightUtils.toPose3D;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.networktables.DoubleArrayEntry;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.StringArrayEntry;
import edu.wpi.first.wpilibj.DriverStation;
import java.util.Optional;
import limelight.Limelight;
import limelight.results.RawDetection;
import limelight.results.RawFiducial;

/**
 * Data retrieval class for {@link Limelight}
 */
public class LimelightData
{

  /**
   * Target data from limelight.
   */
  public  LimelightTargetData   targetData;
  /**
   * Pipeline data from limelight.
   */
  public  LimelightPipelineData pipelineData;
  /**
   * {@link NetworkTable} for the {@link Limelight}
   */
  protected NetworkTable      limelightTable;
  /**
   * {@link Limelight} to fetch data for.
   */
  private Limelight             limelight;
  /**
   * The limelight.results {@link LimelightResults} JSON data
   */
  protected NetworkTableEntry results;
  /**
   * Raw AprilTag detection from NetworkTables.
   */
  protected NetworkTableEntry rawfiducials;
  /**
   * Raw Neural Detector limelight.results from NetworkTables.
   */
  protected NetworkTableEntry rawDetections;
  /**
   * Neural Clasifier result class name.
   */
  protected NetworkTableEntry classifierClass;
  /**
   * Primary neural detect result class name.
   */
  protected NetworkTableEntry detectorClass;
  /**
   * {@link Pose3d} object representing the camera's position and orientation relative to the robot.
   */
  protected DoubleArrayEntry  camera2RobotPose3d;
  /**
   * Barcodes read by the {@link Limelight}.
   */
  protected StringArrayEntry  barcodeData;
  /**
   * Custom Python script set data for {@link Limelight}.
   */
  protected DoubleArrayEntry  pythonScriptDataSet;
  /**
   * Custom Python script output data for {@link Limelight}.
   */
  protected DoubleArrayEntry  pythonScriptData;
  /**
   * Object mapper for limelight.results JSON.
   */
  protected ObjectMapper      resultsObjectMapper;

  /**
   * Construct the {@link LimelightData} class to retrieve read-only data.
   *
   * @param camera {@link Limelight} to use.
   */
  public LimelightData(Limelight camera)
  {
    resultsObjectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    limelight = camera;
    limelightTable = limelight.getNTTable();
    results = limelightTable.getEntry("json");
    rawfiducials = limelightTable.getEntry("rawfiducials");
    rawDetections = limelightTable.getEntry("rawdetections");
    classifierClass = limelightTable.getEntry("tcclass");
    detectorClass = limelightTable.getEntry("tdclass");
    camera2RobotPose3d = limelightTable.getDoubleArrayTopic("camerapose_robotspace").getEntry(new double[0]);
    barcodeData = limelightTable.getStringArrayTopic("rawbarcodes").getEntry(new String[0]);
    pythonScriptData = limelightTable.getDoubleArrayTopic("llpython").getEntry(new double[0]);
    pythonScriptDataSet = limelightTable.getDoubleArrayTopic("llrobot").getEntry(new double[0]);
    targetData = new LimelightTargetData(camera);
    pipelineData = new LimelightPipelineData(camera);
  }

  /**
   * Get the output of the custom python script running on the {@link Limelight}.
   *
   * @return Output Double Array of the custom python script running on the {@link Limelight}.
   */
  public double[] getPythonData()
  {
    return pythonScriptData.get();
  }

  /**
   * Set the input for the custom python script running on the {@link Limelight}
   *
   * @param outgoingData Double array for custom python script.
   */
  public void setPythonData(double[] outgoingData)
  {
    pythonScriptDataSet.set(outgoingData);
  }

  /**
   * Barcode data read by the {@link Limelight}.
   *
   * @return Barcode data as a string.
   */
  public String[] getBarcodeData()
  {
    return barcodeData.get();
  }

  /**
   * Gets the camera's 3D pose with respect to the robot's coordinate system.
   *
   * @return {@link Pose3d} object representing the camera's position and orientation relative to the robot
   */
  public Pose3d getCamera2Robot()
  {
    return toPose3D(camera2RobotPose3d.get());
  }

  /**
   * Gets the current neural classifier result class name.
   *
   * @return Class name string from classifier pipeline
   */
  public String getClassifierClass()
  {
    return classifierClass.getString("");
  }

  /**
   * Gets the primary neural detector result class name.
   *
   * @return Class name string from detector pipeline
   */
  public String getDetectorClass()
  {
    return detectorClass.getString("");
  }


  /**
   * Get {@link LimelightResults} from NetworkTables.
   * <p>
   * Exists only if LL GUI option "Output &amp; Crosshair - Send JSON over NT?" is Yes
   *
   * @return {@link LimelightResults} if it exists.
   */
  public Optional<LimelightResults> getResults()
  {
    try
    {
      var JSONresult = results.getString("");
      if (JSONresult.length() <= 0)
      {
        return Optional.empty();
      }
      LimelightResults data = resultsObjectMapper.readValue(JSONresult,
                                                            LimelightResults.class); // don't use wrapper class
      return Optional.of(data);
    } catch (Exception e) // catch all the errors - multiple kinds are possible
    {
      System.out.println("lljson error: " + e.getMessage());
      DriverStation.reportError("lljson error: " + e.getMessage(), true);
    }
    return Optional.empty();
  }

  /**
   * Gets the latest raw fiducial/AprilTag detection limelight.results from NetworkTables.
   *
   * @return Array of RawFiducial objects containing detection details
   */
  public RawFiducial[] getRawFiducials()
  {
    var rawFiducialArray = rawfiducials.getDoubleArray(new double[0]);
    int valsPerEntry     = 7;
    if (rawFiducialArray.length % valsPerEntry != 0)
    {
      return new RawFiducial[0];
    }

    int           numFiducials = rawFiducialArray.length / valsPerEntry;
    RawFiducial[] rawFiducials = new RawFiducial[numFiducials];

    for (int i = 0; i < numFiducials; i++)
    {
      int    baseIndex    = i * valsPerEntry;
      int    id           = (int) extractArrayEntry(rawFiducialArray, baseIndex);
      double txnc         = extractArrayEntry(rawFiducialArray, baseIndex + 1);
      double tync         = extractArrayEntry(rawFiducialArray, baseIndex + 2);
      double ta           = extractArrayEntry(rawFiducialArray, baseIndex + 3);
      double distToCamera = extractArrayEntry(rawFiducialArray, baseIndex + 4);
      double distToRobot  = extractArrayEntry(rawFiducialArray, baseIndex + 5);
      double ambiguity    = extractArrayEntry(rawFiducialArray, baseIndex + 6);

      rawFiducials[i] = new RawFiducial(id, txnc, tync, ta, distToCamera, distToRobot, ambiguity);
    }

    return rawFiducials;
  }

  /**
   * Gets the latest raw neural detector limelight.results from NetworkTables
   *
   * @return Array of RawDetection objects containing detection details
   */
  public RawDetection[] getRawDetections()
  {
    var rawDetectionArray = rawDetections.getDoubleArray(new double[0]);
    int valsPerEntry      = 12;
    if (rawDetectionArray.length % valsPerEntry != 0)
    {
      return new RawDetection[0];
    }

    int            numDetections = rawDetectionArray.length / valsPerEntry;
    RawDetection[] rawDetections = new RawDetection[numDetections];

    for (int i = 0; i < numDetections; i++)
    {
      int    baseIndex = i * valsPerEntry; // Starting index for this detection's data
      int    classId   = (int) extractArrayEntry(rawDetectionArray, baseIndex);
      double txnc      = extractArrayEntry(rawDetectionArray, baseIndex + 1);
      double tync      = extractArrayEntry(rawDetectionArray, baseIndex + 2);
      double ta        = extractArrayEntry(rawDetectionArray, baseIndex + 3);
      double corner0_X = extractArrayEntry(rawDetectionArray, baseIndex + 4);
      double corner0_Y = extractArrayEntry(rawDetectionArray, baseIndex + 5);
      double corner1_X = extractArrayEntry(rawDetectionArray, baseIndex + 6);
      double corner1_Y = extractArrayEntry(rawDetectionArray, baseIndex + 7);
      double corner2_X = extractArrayEntry(rawDetectionArray, baseIndex + 8);
      double corner2_Y = extractArrayEntry(rawDetectionArray, baseIndex + 9);
      double corner3_X = extractArrayEntry(rawDetectionArray, baseIndex + 10);
      double corner3_Y = extractArrayEntry(rawDetectionArray, baseIndex + 11);

      rawDetections[i] = new RawDetection(classId,
                                          txnc,
                                          tync,
                                          ta,
                                          corner0_X,
                                          corner0_Y,
                                          corner1_X,
                                          corner1_Y,
                                          corner2_X,
                                          corner2_Y,
                                          corner3_X,
                                          corner3_Y);
    }

    return rawDetections;
  }

  // Example of a JSON deserializer wrapper class that can be customized.
  // Customization not needed for the current impelmentation of this YALL.
  // /**
  //  * ResultsWrapper Class for JSON reading.
  //  */
  // private static class ResultsWrapper
  // {

  //   /**
  //    * "ResultsWrapper" Object for JSON reading.
  //    */
  //   @JsonProperty("resultsWrapper")
  //   public LimelightResults resultsWrapper;

  //   @JsonCreator(mode = JsonCreator.Mode.DELEGATING)

  //   public ResultsWrapper(LimelightResults resultsWrapper)
  //   {
  //     this.resultsWrapper = resultsWrapper;
  //   }
  // }
}
