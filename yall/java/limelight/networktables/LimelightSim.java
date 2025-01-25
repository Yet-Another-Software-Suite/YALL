package limelight.networktables;

import static edu.wpi.first.units.Units.Meters;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.units.measure.Distance;
import java.util.List;
import java.util.function.Supplier;
import limelight.Limelight;
import limelight.networktables.target.AprilTagFiducial;

public class LimelightSim
{

  private Limelight           m_limelight;
  private Supplier<Pose2d>    m_pose;
  private AprilTagFieldLayout m_fieldLayout;
  private NetworkTable        m_limelightTable;
  private Distance            m_limelightViewDistance = Meters.of(1);

  /**
   * Limelight simulation class
   *
   * @param limelight   {@link Limelight} to use.
   * @param odomPose    Supplier of the odom pose.
   * @param fieldLayout {@link AprilTagFieldLayout} to use.
   */
  public LimelightSim(Limelight limelight, Supplier<Pose2d> odomPose, AprilTagFieldLayout fieldLayout)
  {
    m_limelight = limelight;
    m_pose = odomPose;
    m_fieldLayout = fieldLayout;
    m_limelightTable = NetworkTableInstance.getDefault().getTable(limelight.limelightName);

  }

  /**
   * Gets the {@link AprilTag} in view of the Limelight, does this by using a Rectangle bounding box from your current
   * pose.
   *
   * @return List of {@link AprilTag}s in range
   */
  public List<AprilTag> getAprilTagsInView()
  {
    Rectangle2d viewbox = new Rectangle2d(m_pose.get(), m_limelightViewDistance, m_limelightViewDistance);
    // TODO: Improve this to use a triangular cone FOV.
    return m_fieldLayout.getTags().stream()
                        .filter(aprilTag -> viewbox.contains(aprilTag.pose.toPose2d().getTranslation()))
                        .toList();
  }


  public void updateTagKeys()
  {
    LimelightData    data   = m_limelight.getData();
    List<AprilTag>   tags   = getAprilTagsInView();
    LimelightResults llJson = new LimelightResults();
    llJson.botpose = LimelightUtils.pose2dToArray(m_pose.get());
    llJson.valid = true;
    llJson.targets_Fiducials = (AprilTagFiducial[]) (tags.stream().map(aprilTag -> {
      AprilTagFiducial fid = new AprilTagFiducial();
      fid.fiducialID = aprilTag.ID;
      fid.fiducialFamily = "36h11";
      return fid;
    }).toArray());
    try
    {
      data.results.setString(data.resultsObjectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(llJson));
    } catch (JsonProcessingException e)
    {
      throw new RuntimeException(e);
    }
    // TODO: Expand this to implement more of the ll json.

  }


}
