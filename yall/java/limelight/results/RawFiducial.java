package limelight.results;

import limelight.Limelight;

/**
 * Represents a {@link Limelight} Raw Fiducial result from {@link Limelight}'s NetworkTables output.
 */
public class RawFiducial
{

  /**
   * AprilTag ID
   */
  public int    id           = 0;
  /**
   * Tag X coordinate in the image.
   */
  public double txnc         = 0;
  /**
   * Tag Y coordinate in the image.
   */
  public double tync         = 0;
  /**
   * Tag area as percent of the image.
   */
  public double ta           = 0;
  /**
   * Distance to camera in Meters
   */
  public double distToCamera = 0;
  /**
   * Distance to robot in Meters
   */
  public double distToRobot  = 0;
  /**
   * Ambiguity as a percentage [0,1]
   */
  public double ambiguity    = 0;


  public RawFiducial(int id, double txnc, double tync, double ta, double distToCamera, double distToRobot,
                     double ambiguity)
  {
    this.id = id;
    this.txnc = txnc;
    this.tync = tync;
    this.ta = ta;
    this.distToCamera = distToCamera;
    this.distToRobot = distToRobot;
    this.ambiguity = ambiguity;
  }
}
