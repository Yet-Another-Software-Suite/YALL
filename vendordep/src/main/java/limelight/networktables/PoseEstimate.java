package limelight.networktables;


import static limelight.networktables.LimelightUtils.extractArrayEntry;
import static limelight.networktables.LimelightUtils.toPose3D;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.networktables.DoubleArrayEntry;
import edu.wpi.first.networktables.TimestampedDoubleArray;
import java.util.Optional;
import limelight.Limelight;
import limelight.results.RawFiducial;

/**
 * Represents a 3D Pose Estimate.
 */
public class PoseEstimate
{

  /**
   * Is a MegaTag2 reading
   */
  public final  boolean          isMegaTag2;
  /**
   * {@link Limelight} to use.
   */
  private final Limelight        limelight;
  /**
   * {@link Limelight} Pose Entry name to use.
   */
  private final String           poseEntryName;
  /**
   * Bot pose estimate
   */
  public        Pose3d           pose;
  /**
   * NT Timestamp in seconds
   */
  public        double           timestampSeconds;
  /**
   * Total latency in seconds
   */
  public        double           latency;
  /**
   * AprilTag in view count
   */
  public        int              tagCount;
  /**
   * Tag Span in meters
   */
  public        double           tagSpan;
  /**
   * Avg apriltag distance in Meters
   */
  public        double           avgTagDist;
  /**
   * Avg area in percent of image
   */
  public        double           avgTagArea;
  /**
   * AprilTags
   */
  public        RawFiducial[]    rawFiducials;
  /**
   * Does the pose limelight.estimator contain data?
   */
  public        boolean          hasData;
  /**
   * {@link Limelight} Pose Entry NetworkTables.
   */
  private       DoubleArrayEntry poseEntry;

  /**
   * Construct the {@link PoseEstimate} from the limelight entry in NT.
   *
   * @param camera    {@link Limelight} to fetch the data from.
   * @param entryName Pose estimation entry we are interested in.
   * @param megaTag2  Is the data MegaTag2
   */
  public PoseEstimate(Limelight camera, String entryName, boolean megaTag2)
  {
    this.pose = new Pose3d();
    this.timestampSeconds = 0;
    this.latency = 0;
    this.tagCount = 0;
    this.tagSpan = 0;
    this.avgTagDist = 0;
    this.avgTagArea = 0;
    this.rawFiducials = new RawFiducial[]{};
    this.isMegaTag2 = megaTag2;
    poseEntryName = entryName;
    limelight = camera;
    poseEntry = limelight.getNTTable().getDoubleArrayTopic(poseEntryName)
                         .getEntry(new double[0]);
  }


  /**
   * Refresh the {@link PoseEstimate}
   *
   * @return {@link PoseEstimate}
   */
  public PoseEstimate refresh()
  {
    getPoseEstimate();
    return this;
  }

  /**
   * Refresh {@link PoseEstimate} object
   *
   * @return {@link PoseEstimate} for chaining.
   */
  public Optional<PoseEstimate> getPoseEstimate()
  {

    TimestampedDoubleArray tsValue   = poseEntry.getAtomic();
    double[]               poseArray = tsValue.value;
    long                   timestamp = tsValue.timestamp;

    if (poseArray.length == 0)
    {
      hasData = false;
      return Optional.empty();
    }

    var    pose     = toPose3D(poseArray);
    double latency  = extractArrayEntry(poseArray, 6);
    int    tagCount = (int) extractArrayEntry(poseArray, 7);
    double tagSpan  = extractArrayEntry(poseArray, 8);
    double tagDist  = extractArrayEntry(poseArray, 9);
    double tagArea  = extractArrayEntry(poseArray, 10);

    // Convert server timestamp from microseconds to seconds and adjust for latency
    double adjustedTimestamp = (timestamp / 1_000_000.0) - (latency / 1_000.0);

    RawFiducial[] rawFiducials      = new RawFiducial[tagCount];
    int           valsPerFiducial   = 7;
    int           expectedTotalVals = 11 + valsPerFiducial * tagCount;

    if (poseArray.length != expectedTotalVals)
    {
      // Don't populate fiducials
    } else
    {
      for (int i = 0; i < tagCount; i++)
      {
        int    baseIndex    = 11 + (i * valsPerFiducial);
        int    id           = (int) poseArray[baseIndex];
        double txnc         = poseArray[baseIndex + 1];
        double tync         = poseArray[baseIndex + 2];
        double ta           = poseArray[baseIndex + 3];
        double distToCamera = poseArray[baseIndex + 4];
        double distToRobot  = poseArray[baseIndex + 5];
        double ambiguity    = poseArray[baseIndex + 6];
        rawFiducials[i] = new RawFiducial(id, txnc, tync, ta, distToCamera, distToRobot, ambiguity);
      }
    }
    this.pose = pose;
    this.timestampSeconds = adjustedTimestamp;
    this.latency = latency;
    this.tagCount = tagCount;
    this.tagSpan = tagSpan;
    this.avgTagDist = tagDist;
    this.avgTagArea = tagArea;
    this.rawFiducials = rawFiducials;
    hasData = rawFiducials.length > 0;

    return Optional.of(this);
  }

  /**
   * Get the minimum ambiguity from seen AprilTag's
   *
   * @return Min ambiguity from observed tags.
   */
  public double getMinTagAmbiguity()
  {
    if (!hasData)
    {
      return 1;
    }
    double minTagAmbiguity = Double.MAX_VALUE;
    for (RawFiducial tag : rawFiducials)
    {
      minTagAmbiguity = Math.min(minTagAmbiguity, tag.ambiguity);
    }
    return minTagAmbiguity;
  }


  /**
   * Get the maximum ambiguity from seen AprilTag's
   *
   * @return Max ambiguity from observed tags. Returns 1 if none.
   */
  public double getMaxTagAmbiguity()
  {
    if (!hasData)
    {
      return 1;
    }
    double maxTagAmbiguity = 0;
    for (RawFiducial tag : rawFiducials)
    {
      maxTagAmbiguity = Math.max(maxTagAmbiguity, tag.ambiguity);
    }
    return maxTagAmbiguity;
  }

  /**
   * Get the average ambiguity from seen AprilTag's
   *
   * @return Avg ambiguity from observed tags. Returns 1 if no tags.
   */
  public double getAvgTagAmbiguity()
  {
    if (!hasData)
    {
      return 1;
    }
    double ambiguitySum = 0;
    for (RawFiducial tag : rawFiducials)
    {
      ambiguitySum += tag.ambiguity;
    }
    return ambiguitySum / rawFiducials.length;
  }

  /**
   * Prints detailed information about a PoseEstimate to standard output. Includes timestamp, latency, tag count, tag
   * span, average tag distance, average tag area, and detailed information about each detected fiducial.
   */
  public String toString()
  {
    StringBuilder str = new StringBuilder();
    if (!hasData)
    {
      str.append(String.format("No PoseEstimate available.%n"));
      return str.toString();
    }
    str.append(String.format("%nPose Estimate Information:%n"));
    str.append(String.format("Timestamp (Seconds): %.3f%n", timestampSeconds));
    str.append(String.format("Latency: %.3f ms%n", latency));
    str.append(String.format("Tag Count: %d%n", tagCount));
    str.append(String.format("Tag Span: %.2f meters%n", tagSpan));
    str.append(String.format("Average Tag Distance: %.2f meters%n", avgTagDist));
    str.append(String.format("Average Tag Area: %.2f%% of image%n", avgTagArea));
    str.append(String.format("Is MegaTag2: %b%n%n", isMegaTag2));

    if (rawFiducials == null || rawFiducials.length == 0)
    {
      str.append(String.format("No RawFiducials data available.%n"));
      return str.toString();
    }

    str.append(String.format("Raw Fiducials Details:%n"));
    for (int i = 0; i < rawFiducials.length; i++)
    {
      RawFiducial fiducial = rawFiducials[i];
      str.append(String.format("Fiducial #%d:%n", i + 1));
      str.append(fiducial);
    }
    return str.toString();
  }


}
