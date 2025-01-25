package limelight.networktables;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.units.measure.AngularVelocity;

/**
 * Orientation3d of the robot for {@link limelight.Limelight}
 */
public class Orientation3d
{

  /**
   * Current orientation of the robot.
   */
  public Rotation3d        orientation;
  public AngularVelocity3d angularVelocity;

  /**
   * Create the robot orientation based off of the given attributes
   *
   * @param orientation {@link Rotation3d} of the robot.
   * @param yaw         {@link AngularVelocity} about the yaw/z-axis
   * @param pitch       {@link AngularVelocity} about the pitch/y-axis
   * @param roll        {@link AngularVelocity} about the roll/x-axis
   */
  public Orientation3d(Rotation3d orientation, AngularVelocity yaw, AngularVelocity pitch, AngularVelocity roll)
  {
    this.orientation = orientation;
    this.angularVelocity = new AngularVelocity3d(roll, pitch, yaw);
  }

  /**
   * Create the robot orientation based off the given attributes.
   *
   * @param orientation     {@link Rotation3d} of the robot.
   * @param angularVelocity {@link AngularVelocity3d} of the robot.
   */
  public Orientation3d(Rotation3d orientation, AngularVelocity3d angularVelocity)
  {
    this.orientation = orientation;
    this.angularVelocity = angularVelocity;
  }
}
