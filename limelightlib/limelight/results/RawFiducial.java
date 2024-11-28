package frc.robot.limelight.results;

/**
 * Represents a Limelight Raw Fiducial result from Limelight's NetworkTables output.
 */
public class RawFiducial
{

  public int    id           = 0;
  public double txnc         = 0;
  public double tync         = 0;
  public double ta           = 0;
  public double distToCamera = 0;
  public double distToRobot  = 0;
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
