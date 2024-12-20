package limelight.structures;


import static limelight.structures.LimelightUtils.toPose2D;
import static limelight.structures.LimelightUtils.toPose3D;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.wpilibj.DriverStation;
import limelight.structures.target.AprilTagFiducial;
import limelight.structures.target.Barcode;
import limelight.structures.target.RetroreflectiveTape;
import limelight.structures.target.pipeline.NeuralClassifier;
import limelight.structures.target.pipeline.NeuralDetector;
import limelight.Limelight;

/**
 * {@link Limelight} Results object, parsed from a {@link Limelight}'s JSON limelight.results output.
 */
public class LimelightResults
{

  public String error;

  @JsonProperty("pID")
  public double pipelineID;

  @JsonProperty("tl")
  public double latency_pipeline;

  @JsonProperty("cl")
  public double latency_capture;

  public double latency_jsonParse;

  @JsonProperty("ts")
  public double timestamp_LIMELIGHT_publish;

  @JsonProperty("ts_rio")
  public double timestamp_RIOFPGA_capture;

  @JsonProperty("v")
  @JsonFormat(shape = Shape.NUMBER)
  public boolean valid;

  @JsonProperty("botpose")
  public double[] botpose;

  @JsonProperty("botpose_wpired")
  public double[] botpose_wpired;

  @JsonProperty("botpose_wpiblue")
  public double[] botpose_wpiblue;

  @JsonProperty("botpose_tagcount")
  public double botpose_tagcount;

  @JsonProperty("botpose_span")
  public double botpose_span;

  @JsonProperty("botpose_avgdist")
  public double botpose_avgdist;

  @JsonProperty("botpose_avgarea")
  public double botpose_avgarea;

  @JsonProperty("t6c_rs")
  public double[]         camerapose_robotspace;
  @JsonProperty("Retro")
  public RetroreflectiveTape[] targets_Retro;
  @JsonProperty("Fiducial")
  public AprilTagFiducial[] targets_Fiducials;
  @JsonProperty("Classifier")
  public NeuralClassifier[] targets_Classifier;
  @JsonProperty("Detector")
  public NeuralDetector[] targets_Detector;
  @JsonProperty("Barcode")
  public Barcode[]        targets_Barcode;

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

  public Pose3d getBotPose3d()
  {
    return toPose3D(botpose);
  }

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

  public Pose2d getBotPose2d()
  {
    return toPose2D(botpose);
  }

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


}
