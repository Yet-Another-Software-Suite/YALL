package limelight.results;

import limelight.Limelight;

/**
 * Represents a {@link Limelight} Raw Neural Detector result from {@link Limelight}'s NetworkTables output.
 */
public class RawDetection
{

  /**
   * ClassID integer
   */
  public int    classId   = 0;
  /**
   * Horizontal Offset From Principal Pixel To Target (degrees)
   */
  public double txnc      = 0;
  /**
   * Vertical Offset From Principal Pixel To Target (degrees)
   */
  public double tync      = 0;
  /**
   * 	Target Area (0% of image to 100% of image)
   */
  public double ta        = 0;
  public double corner0_X = 0;
  public double corner0_Y = 0;
  public double corner1_X = 0;
  public double corner1_Y = 0;
  public double corner2_X = 0;
  public double corner2_Y = 0;
  public double corner3_X = 0;
  public double corner3_Y = 0;


  public RawDetection(int classId, double txnc, double tync, double ta,
                      double corner0_X, double corner0_Y,
                      double corner1_X, double corner1_Y,
                      double corner2_X, double corner2_Y,
                      double corner3_X, double corner3_Y)
  {
    this.classId = classId;
    this.txnc = txnc;
    this.tync = tync;
    this.ta = ta;
    this.corner0_X = corner0_X;
    this.corner0_Y = corner0_Y;
    this.corner1_X = corner1_X;
    this.corner1_Y = corner1_Y;
    this.corner2_X = corner2_X;
    this.corner2_Y = corner2_Y;
    this.corner3_X = corner3_X;
    this.corner3_Y = corner3_Y;
  }
}
