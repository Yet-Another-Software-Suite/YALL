package limelight.networktables;


import static limelight.networktables.LimelightUtils.toPose2D;
import static limelight.networktables.LimelightUtils.toPose3D;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.wpilibj.DriverStation;
import limelight.Limelight;
import limelight.networktables.target.AprilTagFiducial;
import limelight.networktables.target.Barcode;
import limelight.networktables.target.RetroreflectiveTape;
import limelight.networktables.target.pipeline.NeuralClassifier;
import limelight.networktables.target.pipeline.NeuralDetector;

/**
 * {@link Limelight} Results object, parsed from a {@link Limelight}'s JSON limelight.results output.
 */
public class LimelightResults
{

  /**
   * Error message, if any.
   */
  public String error;

  /**
   * Current pipeline index
   */
  @JsonProperty("pID")
  public double pipelineID;

  /**
   * Targeting latency (milliseconds consumed by tracking loop this frame)
   */
  @JsonProperty("tl")
  public double latency_pipeline;

  /**
   * Capture latency (milliseconds between the end of the exposure of the middle row to the beginning of the tracking
   * loop)
   */
  @JsonProperty("cl")
  public double latency_capture;

  public double latency_jsonParse;

  /**
   * Timestamp in milliseconds from boot.
   */
  @JsonProperty("ts")
  public double timestamp_LIMELIGHT_publish;

  @JsonProperty("ts_rio")
  public double timestamp_RIOFPGA_capture;

  /**
   * Validity indicator. 1 = valid targets, 0 = no valid targets
   */
  @JsonProperty("v")
  @JsonFormat(shape = Shape.NUMBER)
  public boolean valid;

  /**
   * Botpose (MegaTag): x,y,z, roll, pitch, yaw (meters, degrees)
   */
  @JsonProperty("botpose")
  public double[] botpose;

  /**
   * Botpose (MegaTag, WPI Red driverstation): x,y,z, roll, pitch, yaw (meters, degrees)
   */
  @JsonProperty("botpose_wpired")
  public double[] botpose_wpired;

  /**
   * Botpose (MegaTag, WPI Blue driverstation): x,y,z, roll, pitch, yaw (meters, degrees)
   */
  @JsonProperty("botpose_wpiblue")
  public double[] botpose_wpiblue;

  /**
   * Number of tags used to compute botpose
   */
  @JsonProperty("botpose_tagcount")
  public double botpose_tagcount;

  /**
   * Max distance between tags used to compute botpose (meters)
   */
  @JsonProperty("botpose_span")
  public double botpose_span;

  /**
   * Max distance between tags used to compute botpose (meters)
   */
  @JsonProperty("botpose_avgdist")
  public double botpose_avgdist;

  /**
   * Average area of tags used to compute botpose
   */
  @JsonProperty("botpose_avgarea")
  public double botpose_avgarea;

  @JsonProperty("t6c_rs")
  public double[]              camerapose_robotspace;
  /**
   * Color/Retroreflective pipeline results array
   */
  @JsonProperty("Retro")
  public RetroreflectiveTape[] targets_Retro;
  /**
   * AprilTag pipeline results array
   */
  @JsonProperty("Fiducial")
  public AprilTagFiducial[]    targets_Fiducials;
  /**
   * Classifier pipeline results array
   */
  @JsonProperty("Classifier")
  public NeuralClassifier[]    targets_Classifier;
  /**
   * Neural Detector pipeline results array
   */
  @JsonProperty("Detector")
  public NeuralDetector[]      targets_Detector;
  /**
   * Barcode pipeline results array
   */
  @JsonProperty("Barcode")
  public Barcode[]             targets_Barcode;

  /**
   * Construct a LimelightResults object for JSON Parsing.
   */
  public LimelightResults()
  {
    botpose = new double[6];
    botpose_wpired = new double[6];
    botpose_wpiblue = new double[6];
    camerapose_robotspace = new double[6];
    targets_Retro = new RetroreflectiveTape[0];
    targets_Fiducials = new AprilTagFiducial[0];
    targets_Classifier = new NeuralClassifier[0];
    targets_Detector = new NeuralDetector[0];
    targets_Barcode = new Barcode[0];

  }

  /**
   * Get the current botpose as a {@link Pose3d} object.
   *
   * @return {@link Pose3d} object representing the botpose.
   */
  public Pose3d getBotPose3d()
  {
    return toPose3D(botpose);
  }

  /**
   * Get the current botpose as a {@link Pose3d} object.
   *
   * @param alliance Alliance color to get the botpose for.
   * @return {@link Pose3d} object representing the botpose.
   */
  public Pose3d getBotPose3d(DriverStation.Alliance alliance)
  {
    if (alliance == DriverStation.Alliance.Red)
    {
      return toPose3D(botpose_wpired);
    } else
    {
      return toPose3D(botpose_wpiblue);
    }
  }

  /**
   * Get the current botpose as a {@link Pose2d} object.
   *
   * @return {@link Pose2d} object representing the botpose.
   */
  public Pose2d getBotPose2d()
  {
    return toPose2D(botpose);
  }

  /**
   * Get the current botpose as a {@link Pose2d} object.
   *
   * @param alliance Alliance color to get the botpose for.
   * @return {@link Pose2d} object representing the botpose.
   */
  public Pose2d getBotPose2d(DriverStation.Alliance alliance)
  {
    if (alliance == DriverStation.Alliance.Red)
    {
      return toPose2D(botpose_wpired);
    } else
    {
      return toPose2D(botpose_wpiblue);
    }
  }

  /**
   * Commonly used but very incomplete set from JSON key from the LL
   */
  public String toString()
  {
    StringBuilder str = new StringBuilder();
    str.append("Partial JSON LimelightResults\n");
    str.append("error " + error + "\n");
    str.append("latency_jsonParse " + latency_jsonParse + "\n");

    str.append("pID " + pipelineID + "\n");
    str.append("tl " + latency_pipeline + "\n");
    str.append("cl " + latency_capture + "\n");
    str.append("ts " + timestamp_LIMELIGHT_publish + "\n");
    str.append("ts_rio " + timestamp_RIOFPGA_capture + "\n");
    str.append("v " + valid + "\n");
    str.append("botpose 3d " + getBotPose3d() + "\n");
    str.append("botpose_wpired 3d " + getBotPose3d(DriverStation.Alliance.Red) + "\n");
    str.append("botpose_wpiblue 3d " + getBotPose3d(DriverStation.Alliance.Blue) + "\n");
    str.append("botpose 2d " + getBotPose2d() + "\n");
    str.append("botpose_wpired 2d " + getBotPose2d(DriverStation.Alliance.Red) + "\n");
    str.append("botpose_wpiblue 2d " + getBotPose2d(DriverStation.Alliance.Blue) + "\n");
    str.append("botpose_tagcount " + botpose_tagcount + "\n");
    str.append("botpose_span " + botpose_span + "\n");
    str.append("botpose_avgdist " + botpose_avgdist + "\n");
    str.append("botpose_avgarea " + botpose_avgarea + "\n");
    str.append("t6c_rs " + camerapose_robotspace + "\n");
    str.append("Retro " + targets_Retro + "\n");
    str.append("Fiducial " + targets_Fiducials + "\n");
    str.append("Classifier " + targets_Classifier + "\n");
    str.append("Detector " + targets_Detector + "\n");
    str.append("Barcode " + targets_Barcode + "\n");

    return str.toString();
  }

}
