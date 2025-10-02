package limelight.sim;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose3d;
import limelight.Limelight;
import limelight.LimelightModel;

public class LimelightSim {
    private final LimelightModel m_model;
    private final Limelight m_limelight;
    private AprilTagFieldLayout m_tagFieldLayout;

    public LimelightSim(Limelight limelight, LimelightModel model) {
        this(limelight, model, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField));
    }

    public LimelightSim(Limelight limelight, LimelightModel model, AprilTagFieldLayout tagFieldLayout) {
        assert limelight != null : "LimelightSim requires a non-null Limelight instance";
        m_limelight = limelight;

        assert model != null : "LimelightSim requires a non-null LimelightModel instance";
        m_model = model;

        m_tagFieldLayout = tagFieldLayout;
    }

    public LimelightSim withCameraToRobotOffset(Pose3d offset) {
        m_limelight.getSettings().withCameraOffset(offset);
        return this;
    }

    public LimelightSim withApriltagFieldLayout(AprilTagFieldLayout layout) {
        m_tagFieldLayout = layout;
        return this;
    }

    public LimelightModel getModel() {
        return m_model;
    }

    public AprilTagFieldLayout getTagFieldLayout() {
        return m_tagFieldLayout;
    }

    public void update(Pose3d robotPose) {
        // TODO
    }
}
