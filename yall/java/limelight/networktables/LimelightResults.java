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
import limelight.results.HardwareReport;
import limelight.results.IMUResults;
import limelight.results.RewindStats;

/**
 * {@link Limelight} Results object, parsed from a {@link Limelight}'s JSON limelight.results output.
 */
public class LimelightResults
{

  /**
   * Error message, if any.
   */
  public String                error;
  /**
   * Horizontal Offset From Crosshair To Target (LL1: -27 degrees to 27 degrees / LL2: -29.8 to 29.8 degrees).
   */
  @JsonProperty("tx")
  public double                tx;
  /**
   * Vertical Offset From Crosshair To Target (LL1: -20.5 degrees to 20.5 degrees / LL2: -24.85 to 24.85 degrees).
   */
  @JsonProperty("ty")
  public double                ty;
  /**
   * Horizontal Offset From Principal Pixel To Target (degrees).
   */
  @JsonProperty("txnc")
  public double                txnc;
  /**
   * Vertical Offset From Principal Pixel To Target (degrees).
   */
  @JsonProperty("tync")
  public double                tync;
  /**
   * Undistorted, normalized area of target (0-100).
   */
  @JsonProperty("ta")
  public double                ta;
  /**
   * Current pipeline index
   */
  @JsonProperty("pID")
  public double                pipelineID;
  /**
   * Current Pipeline Type e.g. "pipe_color".
   */
  @JsonProperty("pTYPE")
  public String                pipeline_type;
  /**
   * Targeting latency (milliseconds consumed by tracking loop this frame)
   */
  @JsonProperty("tl")
  public double                latency_pipeline;
  /**
   * Capture latency (milliseconds between the end of the exposure of the middle row to the beginning of the tracking
   * loop)
   */
  @JsonProperty("cl")
  public double                latency_capture;
  /**
   * Timestamp in milliseconds from boot (legacy).
   */
  @JsonProperty("ts")
  public double                timestamp_LIMELIGHT_publish;
  /**
   * Timestamp in milliseconds from RIO.
   */
  @JsonProperty("ts_rio")
  public double                timestamp_RIOFPGA_capture;
  /**
   * NetworkTables server time in microseconds (0 if not connected).
   */
  @JsonProperty("ts_nt")
  public double                timestamp_NT;
  /**
   * System wall clock timestamp in microseconds since epoch.
   */
  @JsonProperty("ts_sys")
  public double                timestamp_System;
  /**
   * Frame index (counter starting at 0).
   */
  @JsonProperty("fidx")
  public double                frame_index;
  /**
   * Validity indicator. 1 = valid targets, 0 = no valid targets
   */
  @JsonProperty("v")
  @JsonFormat(shape = Shape.NUMBER)
  public boolean               valid;
  /**
   * Botpose (MegaTag): x,y,z, roll, pitch, yaw (meters, degrees)
   */
  @JsonProperty("botpose")
  public double[]              botpose;
  /**
   * Botpose (MegaTag1, WPI Red driverstation): x,y,z, roll, pitch, yaw (meters, degrees)
   */
  @JsonProperty("botpose_wpired")
  public double[]              botpose_wpired;
  /**
   * Botpose (MegaTag1, WPI Blue driverstation): x,y,z, roll, pitch, yaw (meters, degrees)
   */
  @JsonProperty("botpose_wpiblue")
  public double[]              botpose_wpiblue;
  /**
   * Botpose (MegaTag2): x,y,z, roll, pitch, yaw (meters, degrees).
   */
  @JsonProperty("botpose_orb")
  public double[] botpose_mt2;
  /**
   * Botpose (MegaTag2, WPI Blue driverstation): x,y,z, roll, pitch, yaw (meters, degrees).
   */
  @JsonProperty("botpose_orb_wpiblue")
  public double[] botpose_mt2_blue;
  /**
   * Number of tags used to compute botpose
   */
  @JsonProperty("botpose_tagcount")
  public double                botpose_tagcount;
  /**
   * Max distance between tags used to compute botpose (meters)
   */
  @JsonProperty("botpose_span")
  public double                botpose_span;
  /**
   * Max distance between tags used to compute botpose (meters)
   */
  @JsonProperty("botpose_avgdist")
  public double                botpose_avgdist;
  /**
   * Average area of tags used to compute botpose
   */
  @JsonProperty("botpose_avgarea")
  public double                botpose_avgarea;
  /**
   * Camera pose in robot space [x, y, z, roll, pitch, yaw] (meters, degrees).
   */
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
   * Hardware report
   */
  @JsonProperty("hw")
  public HardwareReport        hardware;
  /**
   * Limelight IMU results
   */
  @JsonProperty("imu")
  public IMUResults            imuResults;
  /**
   * Rewind stats (Limelight 4 only)
   */
  @JsonProperty("rewind")
  public RewindStats           rewindStats;
  /**
   * Image source setting value.
   */
  @JsonProperty("imgsrc")
  public String                imageSource;
  /**
   * Hardware type identifier.
   */
  @JsonProperty("hwtype")
  public String                hardwareType;
  /**
   * 1 if web UI needs refresh, 0 otherwise.
   */
  @JsonProperty("uirefesh")
  public int                   uiRefresh;
  /**
   * 1 if NetworkTables pipeline control is disabled, 0 otherwise.
   */
  @JsonProperty("ignorent")
  public int                   ignoreNetworkTables;
  /**
   * 3D distance from camera to target (or POI) in Meters
   */
  @JsonProperty("tdist")
  public double                targetDistance;
  /**
   * Output data from python SnapScript Pipelines (array of 8 doubles).
   */
  @JsonProperty("PythonOut")
  public double[]              PythonOut;
  /**
   * MT1 Standard Deviation [x, y, z, roll, pitch, yaw] (meters, degrees).
   */
  @JsonProperty("stdev_mt1")
  public double[]              stdev_mt1;
  /**
   * MT2 Standard Deviation [x, y, z, roll, pitch, yaw] (meters, degrees).
   */
  @JsonProperty("stdev_mt2")
  public double[]              stdev_mt2;
  /* Unsupported */
  public double                focus_metric;

  /**
   * Construct a LimelightResults object for JSON Parsing.
   */
  public LimelightResults()
  {
    stdev_mt1 = new double[6];
    stdev_mt2 = new double[6];
    PythonOut = new double[8];
    botpose = new double[6];
    botpose_wpired = new double[6];
    botpose_wpiblue = new double[6];
    botpose_mt2 = new double[6];
    botpose_mt2_blue = new double[6];
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
   * @implNote This only returns MegaTag1 poses.
   */
  public Pose3d getBotPose3d()
  {
    return toPose3D(botpose);
  }

  /**
   * Get the current Megatag2 pose as a {@link Pose3d} object.
   *
   * @return {@link Pose3d} object representing the botpose_orb.
   */
  public Pose3d getMT2Pose3d()
  {
    return toPose3D(botpose_mt2);
  }

  /**
   * Get the current botpose as a {@link Pose3d} object.
   *
   * @param alliance Alliance color to get the botpose for.
   * @return {@link Pose3d} object representing the botpose.
   * @implNote This only returns MegaTag1 poses.
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
