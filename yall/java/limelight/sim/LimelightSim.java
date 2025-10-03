package limelight.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.networktables.NetworkTableEntry;
import limelight.Limelight;
import limelight.LimelightModel;

public class LimelightSim {
    private int m_defaultPipeline = 0;

    private final LimelightModel m_model;
    private final Limelight m_limelight;
    private final NetworkTableEntry m_heartbeatEntry;
    private double m_heartbeat = 0;

    private AprilTagFieldLayout m_tagFieldLayout;
    private List<LimelightPipelineSim> m_pipelines = new ArrayList<>();
    private Pose3d m_cameraToRobotTransform; // will need to be used for checking if the camera can actually see the tag

    private final double HEARTBEAT_LIMIT = 2 * 10e9;

    public LimelightSim(Limelight limelight, LimelightModel model) {
        this(limelight, model, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField));
    }

    public LimelightSim(Limelight limelight, LimelightModel model, AprilTagFieldLayout tagFieldLayout) {
        assert limelight != null : "LimelightSim requires a non-null Limelight instance";
        m_limelight = limelight;

        assert model != null : "LimelightSim requires a non-null LimelightModel instance";
        m_model = model;

        m_tagFieldLayout = tagFieldLayout;

        m_heartbeatEntry = limelight.getNTTable().getEntry("hb");

        withDefaultPipeline(m_defaultPipeline);
    }

    public LimelightSim withPipelines(LimelightPipelineSim... pipelines) {
        m_pipelines = Arrays.asList(pipelines);
        return this;
    }

    public LimelightSim withCameraToRobotOffset(Pose3d offset) {
        m_cameraToRobotTransform = offset;
        m_limelight.getSettings().withCameraOffset(offset);
        return this;
    }

    public LimelightSim withDefaultPipeline(int defaultPipeline) {
        m_defaultPipeline = defaultPipeline;
        m_limelight.getSettings().withPipelineIndex(defaultPipeline);
        m_limelight.getData().pipelineData.getCurrentPipelineIndexEntry().setNumber((double) defaultPipeline);
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

    public void update(Pose2d robotPose) {
        updateHeartbeat();
    }

    private void updateHeartbeat() {
        m_heartbeat++;
        if (m_heartbeat > HEARTBEAT_LIMIT) {
            m_heartbeat = 0;
        }
        m_heartbeatEntry.setNumber(m_heartbeat);
    }
}
