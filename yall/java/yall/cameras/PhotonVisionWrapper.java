package yall.cameras;

import java.util.List;
import org.photonvision.PhotonCamera;
import org.photonvision.simulation.SimCameraProperties;
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.sim.CameraModel;

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
 * <p>No vendor configs are accepted; use {@link #getCamera()} for PhotonVision settings.
 */
public class PhotonVisionWrapper extends VisionCamera {

  private final PhotonCamera camera;

  /**
   * Wraps a PhotonVision camera.
   *
   * @param camera The PhotonVision camera.
   * @param config The camera's config.
   */
  public PhotonVisionWrapper(PhotonCamera camera, VisionCameraConfig config) {
    super(camera.getName(), config, camera);
    this.camera = camera;
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
  protected List<Frame> readFrames() {
    return readPhotonFrames(camera);
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
   * An Arducam B0332, the common OV9281 USB camera for PhotonVision.
   *
   * @return Camera properties.
   */
  @Override
  protected SimCameraProperties defaultCameraProperties() {
    return CameraModel.ARDUCAM_B0332.getSimCameraProperties();
  }

  @Override
  public PhotonCamera getCamera() {
    return camera;
  }
}
