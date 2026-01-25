package limelight.networktables;

import edu.wpi.first.units.measure.AngularAcceleration;

/**
 * Angular acceleration 3d helper class.
 */
public class AngularAcceleration3d
{

  /**
   * Angular acceleration about the X axis.
   */
  public final AngularAcceleration x, roll;
  /**
   * Angular acceleration about the Y axis.
   */
  public final AngularAcceleration y, pitch;
  /**
   * Angular acceleration about the Z axis.
   */
  public final AngularAcceleration z, yaw;


  /**
   * Construct a 3d Angular Acceleration.
   *
   * @param x Roll; Angular acceleration about the X-axis.
   * @param y Pitch; Angular acceleration about the Y-axis.
   * @param z Yaw; Angular acceleration about the Z-axis.
   */
  public AngularAcceleration3d(AngularAcceleration x, AngularAcceleration y, AngularAcceleration z)
  {
    this.x = this.roll = x;
    this.y = this.pitch = y;
    this.z = this.yaw = z;
  }
}
