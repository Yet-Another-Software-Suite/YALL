package limelight.networktables;

import edu.wpi.first.units.measure.AngularVelocity;

/**
 * Angular velocity 3d helper class.
 */
public class AngularVelocity3d
{

  /**
   * Angular velocity about the X axis.
   */
  public final AngularVelocity x, roll;
  /**
   * Angular velocity about the Y axis.
   */
  public final AngularVelocity y, pitch;
  /**
   * Angular velocity about the Z axis.
   */
  public final AngularVelocity z, yaw;


  /**
   * Construct a 3d Angular Velocity.
   *
   * @param x Roll; Angular velocity about the X-axis.
   * @param y Pitch; Angular velocity about the Y-axis.
   * @param z Yaw; Angular velocity about the Z-axis.
   */
  public AngularVelocity3d(AngularVelocity x, AngularVelocity y, AngularVelocity z)
  {
    this.x = this.roll = x;
    this.y = this.pitch = y;
    this.z = this.yaw = z;
  }
}
