package limelight.results;

import static edu.wpi.first.units.Units.Degrees;
import static edu.wpi.first.units.Units.DegreesPerSecond;
import static edu.wpi.first.units.Units.DegreesPerSecondPerSecond;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.math.Pair;
import edu.wpi.first.math.geometry.Quaternion;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.units.measure.Angle;
import limelight.networktables.AngularAcceleration3d;
import limelight.networktables.AngularVelocity3d;
import limelight.networktables.Orientation3d;

/**
 * IMU results
 */
public class IMUResults
{

  /**
   * Raw IMU array [roll, pitch, yaw, roll_rate, pitch_rate, yaw_rate, accel_x, accel_y, accel_z] (degrees, degrees/s,
   * degrees/s^2)
   */
  @JsonProperty("data")
  public double[] data;
  /**
   * Orientation quaternion [w, x, y, z]
   */
  @JsonProperty("quat")
  public double[] quaternion;
  /**
   * Final fused yaw (degrees)
   */
  @JsonProperty("yaw")
  public double   yaw = 0;

  /**
   * IMU Results Constructor
   */
  public IMUResults()
  {
    quaternion = new double[4];
    data = new double[9];
  }

  /**
   * Get the {@link Quaternion} from the {@link IMUResults}
   *
   * @return {@link Quaternion} as {@link Rotation3d}
   */
  public Rotation3d Quaternion()
  {
    return new Rotation3d(new Quaternion(quaternion[0], quaternion[1], quaternion[2], quaternion[3]));
  }

  /**
   * Get the IMU data as a {@link Pair} of {@link Orientation3d} and {@link AngularAcceleration3d}
   *
   * @return Pair of {@link Orientation3d} and {@link AngularAcceleration3d}
   */
  public Pair<Orientation3d, AngularAcceleration3d> getIMUData()
  {
    return Pair.of(new Orientation3d(new Rotation3d(Degrees.of(data[0]), Degrees.of(data[1]), Degrees.of(data[2])),
                                     new AngularVelocity3d(DegreesPerSecond.of(data[3]),
                                                           DegreesPerSecond.of(data[4]),
                                                           DegreesPerSecond.of(data[5]))),
                   new AngularAcceleration3d(DegreesPerSecondPerSecond.of(data[6]),
                                             DegreesPerSecondPerSecond.of(data[7]),
                                             DegreesPerSecondPerSecond.of(data[8])));
  }

  /**
   * Get the fused yaw from the IMU data
   *
   * @return Fused yaw as {@link Angle}
   */
  public Angle getFusedYaw()
  {
    return Degrees.of(yaw);
  }
}
