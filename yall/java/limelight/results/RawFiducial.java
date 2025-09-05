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
   * Tag ambiguity as percent of the image.
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


  /**
   * Creates a new RawFiducial Result
   *
   * @param id           Tag ID
   * @param txnc         X coordinate in image
   * @param tync         Y coordinate in image
   * @param ta           Ambiguity as a percent of the image
   * @param distToCamera Distance to camera in Meters
   * @param distToRobot  Distance to robot in Meters
   * @param ambiguity    ambiguity as a percentage [0,1]
   */
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


  /**
   * Convert the AprilTag class into a string
   */
  public String toString()
  {
    StringBuilder str = new StringBuilder(220);
    str.append(String.format("Tag ID %d%n", id));
    str.append(String.format(" Coordinate in image (%.2f, %.2f)%n", txnc, tync));
    str.append(String.format(" Tag Area %.2f%n", ta));
    str.append(String.format(" Distance to Camera %.2f%n", distToCamera));
    str.append(String.format(" Distance to Robot %.2f%n", distToRobot));
    str.append(String.format(" Ambiguity %.2f%n", ambiguity));
    return str.toString();
  }
}
