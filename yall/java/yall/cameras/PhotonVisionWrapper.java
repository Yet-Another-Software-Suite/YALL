package yall.cameras;

import java.util.List;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.VisionFrame;
import yall.sim.CameraModel;
import yall.sim.SimulatedCamera;

/**
 * A PhotonVision camera.
 *
 * <p>In simulation, PhotonVision's camera simulation publishes to this {@link PhotonCamera}, so code
 * that uses {@link #getCamera()} directly sees simulated results too. Let this wrapper read the
 * results: {@link PhotonCamera#getAllUnreadResults()} hands each result out only once.
 *
 * <p>Pose estimates come from {@link org.photonvision.PhotonPoseEstimator}: the coprocessor's
 * multi-tag result, falling back to the lowest-ambiguity single tag. With {@link
 * yall.PoseEstimationMode#HEADING_ASSISTED} it tries the PnP distance trig solve first.
 *
 * <p>Without a camera model in the config, it is simulated as a {@link CameraModel#ARDUCAM_OV9281}.
 *
 * <p>No vendor configs are accepted; use {@link #getCamera()} for PhotonVision settings.
 */
public class PhotonVisionWrapper extends VisionCamera {

  private final PhotonCamera camera;
  private final PhotonPoseEstimator poseEstimator;

  /**
   * Wraps a PhotonVision camera.
   *
   * @param camera The PhotonVision camera.
   * @param config The camera's config.
   */
  public PhotonVisionWrapper(PhotonCamera camera, VisionCameraConfig config) {
    super(
        camera.getName(),
        config,
        SimulatedCamera.create(camera.getName(), config, CameraModel.ARDUCAM_OV9281, camera));
    this.camera = camera;
    poseEstimator = new PhotonPoseEstimator(config.getField(), getRobotToCamera());
    config
        .getVendorConfig()
        .ifPresent(
            vendorConfig -> {
              throw new IllegalArgumentException(
                  "PhotonVisionWrapper takes no vendor config, got "
                      + vendorConfig.getClass().getName()
                      + ". Use getCamera() for PhotonVision settings.");
            });
    config.getPipelineIndex().ifPresent(this::setPipelineIndex);
  }

  @Override
  protected List<VisionFrame> readFrames() {
    return PhotonVisionFrames.read(name, camera, poseEstimator, config.getPoseEstimationMode());
  }

  @Override
  protected void applyRobotOrientation(
      double timestampSeconds, Rotation3d orientation, double yawRateDegreesPerSecond) {
    poseEstimator.addHeadingData(timestampSeconds, orientation);
  }

  @Override
  protected void applyRobotToCamera(Transform3d robotToCamera) {
    poseEstimator.setRobotToCameraTransform(robotToCamera);
  }

  @Override
  protected void applyPipelineIndex(int index) {
    camera.setPipelineIndex(index);
  }

  @Override
  protected int readPipelineIndex() {
    return camera.getPipelineIndex();
  }

  @Override
  protected boolean readConnected() {
    return camera.isConnected();
  }

  /**
   * PhotonVision's pose estimator for this camera's real results, for settings YALL doesn't cover.
   * In simulation, the simulation has its own: see {@link SimulatedCamera#getPhotonPoseEstimator()}.
   *
   * @return The pose estimator.
   */
  public PhotonPoseEstimator getPhotonPoseEstimator() {
    return poseEstimator;
  }

  @Override
  public PhotonCamera getCamera() {
    return camera;
  }
}
