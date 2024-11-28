//LimelightHelpers v1.10 (REQUIRES LLOS 2024.9.1 OR LATER)

package frc.robot.limelight;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * LimelightHelpers provides static methods and classes for interfacing with Limelight vision cameras in FRC. This
 * library supports all Limelight features including AprilTag tracking, Neural Networks, and standard
 * color/retroreflective tracking.
 */
public class LimelightHelpers
{


  public static final String sanitizeName(String name)
  {
    if (name == "" || name == null)
    {
      return "limelight";
    }
    return name;
  }

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

}