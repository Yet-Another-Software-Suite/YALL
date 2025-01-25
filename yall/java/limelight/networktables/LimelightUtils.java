package limelight.networktables;


import static edu.wpi.first.units.Units.Degrees;
import static edu.wpi.first.units.Units.DegreesPerSecond;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility classes to convert WPILib data to LimelightLib expected values.
 */
public class LimelightUtils
{

  /**
   * Sanitize the {@link limelight.Limelight} name
   *
   * @param name Limelight name
   * @return {@link limelight.Limelight#limelightName} or "limelight"
   */
  public static final String sanitizeName(String name)
  {
    if (name == "" || name == null)
    {
      return "limelight";
    }
    return name;
  }

  /**
   * Get the URL for the limelight.
   *
   * @param tableName {@link limelight.Limelight#limelightName}
   * @param request   URI to request from {@link limelight.Limelight}
   * @return {@link URL} to request for {@link limelight.Limelight}
   */
  public static URL getLimelightURLString(String tableName, String request)
  {
    String urlString = "http://" + sanitizeName(tableName) + ".local:5807/" + request;
    URL    url;
    try
    {
      url = new URL(urlString);
      return url;
    } catch (MalformedURLException e)
    {
      System.err.println("bad LL URL");
    }
    return null;
  }

  /**
   * Takes a 6-length array of pose data and converts it to a {@link Pose3d} object. Array format: [x, y, z, roll,
   * pitch, yaw] where angles are in degrees.
   *
   * @param inData Array containing pose data [x, y, z, roll, pitch, yaw]
   * @return {@link Pose3d} object representing the pose, or empty {@link Pose3d} if invalid data
   */
  public static Pose3d toPose3D(double[] inData)
  {
    if (inData.length < 6)
    {
      //System.err.println("Bad LL 3D Pose Data!");
      return new Pose3d();
    }
    return new Pose3d(
        new Translation3d(inData[0], inData[1], inData[2]),
        new Rotation3d(Units.degreesToRadians(inData[3]), Units.degreesToRadians(inData[4]),
                       Units.degreesToRadians(inData[5])));
  }


  /**
   * Takes a 6-length array of {@link Pose2d} data and converts it to a {@link Pose2d} object. Uses only x, y, and yaw
   * components, ignoring z, roll, and pitch. Array format: [x, y, z, roll, pitch, yaw] where angles are in degrees.
   *
   * @param inData Array containing pose data [x, y, z, roll, pitch, yaw]
   * @return {@link Pose2d} object representing the pose, or empty {@link Pose2d} if invalid data
   */
  public static Pose2d toPose2D(double[] inData)
  {
    if (inData.length < 6)
    {
      //System.err.println("Bad LL 2D Pose Data!");
      return new Pose2d();
    }
    Translation2d tran2d = new Translation2d(inData[0], inData[1]);
    Rotation2d    r2d    = new Rotation2d(Units.degreesToRadians(inData[5]));
    return new Pose2d(tran2d, r2d);
  }

  /**
   * Takes a 3-length array of {@link Translation3d} data and converts it into a {@link Translation3d}. Array format:
   * [x, y, z]
   *
   * @param translation Array containing translation data [x, y, z] in Meters.
   * @return {@link Translation3d} to set.
   */
  public static Translation3d toTranslation3d(double[] translation)
  {
    return new Translation3d(translation[0], translation[1], translation[2]);
  }

  /**
   * Takes a 6-length array of {@link Orientation3d} data and converts it into a {@link Orientation3d}. Array format:
   * [yaw,yawrate,pitch,pitchrate,roll,rollrate]
   *
   * @param orientation Array containing {@link Orientation3d} [yaw,yawrate,pitch,pitchrate,roll,rollrate] in Degrees.
   * @return {@link Orientation3d}
   */
  public static Orientation3d toOrientation(double[] orientation)
  {
    return new Orientation3d(new Rotation3d(Degrees.of(orientation[4]),
                                            Degrees.of(orientation[2]),
                                            Degrees.of(orientation[0])),
                             DegreesPerSecond.of(orientation[1]),
                             DegreesPerSecond.of(orientation[3]),
                             DegreesPerSecond.of(orientation[5]));
  }

  /**
   * Converts an {@link Orientation3d} to a 6-length array of [yaw,yawrate,pitch,pitchrate,roll,rollrate] in Degrees.
   *
   * @param orientation {@link Orientation3d} to convert.
   * @return Array [yaw,yawrate,pitch,pitchrate,roll,rollrate] in Degrees.
   */
  public static double[] orientation3dToArray(Orientation3d orientation)
  {
    return new double[]{orientation.orientation.getMeasureZ().in(Degrees), orientation.angularVelocity.yaw.in(
        DegreesPerSecond),
                        orientation.orientation.getMeasureY().in(Degrees), orientation.angularVelocity.pitch.in(
        DegreesPerSecond),
                        orientation.orientation.getMeasureX().in(Degrees), orientation.angularVelocity.roll.in(
        DegreesPerSecond)};
  }

  /**
   * Converts a {@link Pose3d} object to an array of doubles in the format [x, y, z, roll, pitch, yaw]. Translation
   * components are in meters, rotation components are in degrees.
   *
   * @param pose The {@link Pose3d} object to convert
   * @return A 6-element array containing [x, y, z, roll, pitch, yaw]
   */
  public static double[] pose3dToArray(Pose3d pose)
  {
    double[] result = new double[6];
    result[0] = pose.getTranslation().getX();
    result[1] = pose.getTranslation().getY();
    result[2] = pose.getTranslation().getZ();
    result[3] = Units.radiansToDegrees(pose.getRotation().getX());
    result[4] = Units.radiansToDegrees(pose.getRotation().getY());
    result[5] = Units.radiansToDegrees(pose.getRotation().getZ());
    return result;
  }

  /**
   * Converts a {@link Pose2d} object to an array of doubles in the format [x, y, z, roll, pitch, yaw]. Translation
   * components are in meters, rotation components are in degrees. Note: z, roll, and pitch will be 0 since
   * {@link Pose2d} only contains x, y, and yaw.
   *
   * @param pose The {@link Pose2d} object to convert
   * @return A 6-element array containing [x, y, 0, 0, 0, yaw]
   */
  public static double[] pose2dToArray(Pose2d pose)
  {
    double[] result = new double[6];
    result[0] = pose.getTranslation().getX();
    result[1] = pose.getTranslation().getY();
    result[2] = 0;
    result[3] = Units.radiansToDegrees(0);
    result[4] = Units.radiansToDegrees(0);
    result[5] = Units.radiansToDegrees(pose.getRotation().getRadians());
    return result;
  }

  /**
   * Converts a {@link Translation3d} object to an array of doubles in format [x, y, z]. Measurements are in Meters.
   *
   * @param translation {@link Translation3d} to convert.
   * @return Double array containing [x, y, z]
   */
  public static double[] translation3dToArray(Translation3d translation)
  {
    return new double[]{translation.getX(), translation.getY(), translation.getZ()};
  }

  /**
   * Return a double from a double array if it exists, else return 0.
   *
   * @param inData   Double array to extract from
   * @param position Position to read
   * @return 0 if data isn't present, else the double.
   */
  public static double extractArrayEntry(double[] inData, int position)
  {
    if (inData.length < position + 1)
    {
      return 0;
    }
    return inData[position];
  }


}
