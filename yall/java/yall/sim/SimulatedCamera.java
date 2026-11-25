package yall.sim;

import java.util.List;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.simulation.PhotonCameraSim;
import org.photonvision.simulation.SimCameraProperties;
import org.wpilib.framework.RobotBase;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import yall.VisionCameraConfig;
import yall.VisionFrame;
import yall.cameras.PhotonVisionFrames;

/**
 * The simulation of one {@link yall.VisionCamera}, whatever its vendor: a PhotonVision camera
 * simulation in a {@link VisionSim} world, and PhotonVision's pose estimator for its results.
 * Cameras make one for themselves when the robot program runs in simulation.
 */
public class SimulatedCamera implements AutoCloseable {

  private final String name;
  private final VisionCameraConfig config;
  private final PhotonCamera camera;
  private final boolean ownsCamera;
  private final PhotonCameraSim cameraSim;
  private final VisionSim visionSim;
  private final PhotonPoseEstimator poseEstimator;
  private int pipelineIndex;

  private SimulatedCamera(
      String name, VisionCameraConfig config, CameraModel defaultModel, PhotonCamera camera) {
    this.name = name;
    this.config = config;
    ownsCamera = camera == null;
    this.camera = ownsCamera ? new PhotonCamera("YALL-" + name) : camera;
    var robotToCamera = config.getRobotToCamera().orElse(Transform3d.ZERO);
    poseEstimator = new PhotonPoseEstimator(config.getField(), robotToCamera);
    SimCameraProperties properties =
        config.getCameraProperties().orElseGet(defaultModel::getSimCameraProperties);
    cameraSim = new PhotonCameraSim(this.camera, properties, config.getField());
    cameraSim.enableRawStream(config.isSimulatedVideoStreamsEnabled());
    cameraSim.enableProcessedStream(config.isSimulatedVideoStreamsEnabled());
    cameraSim.enableDrawWireframe(
        config.isSimulatedVideoStreamsEnabled() && config.isSimulatedWireframeEnabled());
    visionSim = config.getVisionSim().orElseGet(VisionSim::getDefault);
    visionSim.addCamera(cameraSim, robotToCamera);
  }

  /**
   * Makes the simulation of a camera, if the robot program runs in simulation and the config allows
   * it.
   *
   * @param name Name of the camera.
   * @param config The camera's config.
   * @param defaultModel Camera to simulate when the config sets no camera properties.
   * @param camera PhotonVision camera to simulate, or null to make a private one named {@code
   *     YALL-<name>}.
   * @return The simulation, or null when not simulating.
   */
  public static SimulatedCamera create(
      String name, VisionCameraConfig config, CameraModel defaultModel, PhotonCamera camera) {
    if (!RobotBase.isSimulation() || !config.isSimulationEnabled()) {
      return null;
    }
    return new SimulatedCamera(name, config, defaultModel, camera);
  }

  /**
   * Reads the simulated frames since the last call.
   *
   * @return The frames, oldest first.
   */
  public List<VisionFrame> readFrames() {
    return PhotonVisionFrames.read(name, camera, poseEstimator, config.getPoseEstimationMode());
  }

  /**
   * Gives the simulated pose estimator the robot's heading, for heading-assisted estimates.
   *
   * @param timestampSeconds When the heading was measured.
   * @param orientation Robot orientation on the field.
   */
  public void addHeadingData(double timestampSeconds, Rotation3d orientation) {
    poseEstimator.addHeadingData(timestampSeconds, orientation);
  }

  /**
   * Moves the simulated camera on the robot.
   *
   * @param robotToCamera Transform from the robot origin to the camera.
   */
  public void setRobotToCamera(Transform3d robotToCamera) {
    poseEstimator.setRobotToCameraTransform(robotToCamera);
    visionSim.adjustCamera(cameraSim, robotToCamera);
  }

  /**
   * Sets the simulated pipeline index. The simulation doesn't change with it.
   *
   * @param index Pipeline index.
   */
  public void setPipelineIndex(int index) {
    pipelineIndex = index;
  }

  /**
   * The simulated pipeline index.
   *
   * @return Pipeline index.
   */
  public int getPipelineIndex() {
    return pipelineIndex;
  }

  /**
   * PhotonVision's simulated camera, for settings YALL doesn't cover.
   *
   * @return The simulated camera.
   */
  public PhotonCameraSim getCameraSim() {
    return cameraSim;
  }

  /**
   * The simulated world this camera is in.
   *
   * @return The world.
   */
  public VisionSim getVisionSim() {
    return visionSim;
  }

  /**
   * PhotonVision's pose estimator for the simulated results, for settings YALL doesn't cover.
   *
   * @return The pose estimator.
   */
  public PhotonPoseEstimator getPhotonPoseEstimator() {
    return poseEstimator;
  }

  /** Removes the camera from its world and closes it. */
  @Override
  public void close() {
    visionSim.removeCamera(cameraSim);
    cameraSim.close();
    if (ownsCamera) {
      camera.close();
    }
  }
}
