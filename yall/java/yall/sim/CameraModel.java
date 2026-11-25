package yall.sim;

import org.photonvision.simulation.SimCameraProperties;
import org.wpilib.math.linalg.MatBuilder;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N8;
import org.wpilib.math.util.Nat;

/**
 * Published specs of common FRC vision cameras, for simulating them. Pass one to {@link
 * yall.VisionCameraConfig#withCameraModel(CameraModel)}.
 *
 * <p>Each model simulates its vendor's horizontal and vertical field of view. Smart cameras
 * (Limelight, Luma) run at the AprilTag frame rate their vendor publishes. USB cameras run at their
 * full-resolution capture rate; the coprocessor may track AprilTags slower, so lower it with {@link
 * yall.VisionCameraConfig#withCameraProperties} if yours does. Vendors don't publish fixed latencies,
 * so every model uses an estimate; Limelights report their real latency at runtime.
 *
 * <p>Sources: the Limelight quick-start pages and hardware comparison at
 * https://docs.limelightvision.io/docs/docs-limelight/getting-started/, the Luma P1 docs at
 * https://docs.luma.vision/p1/ and Luma P2 page at https://luma.vision/products/luma-p2, The Thrifty
 * Bot's docs at https://docs.thethriftybot.com/thriftycam and
 * https://docs.thethriftybot.com/thriftiest-cam, Arducam's B0332 quick start guide, AndyMark's am-5170
 * page and ELP's ELP-USB8MP02G-L180 listing.
 */
public enum CameraModel {
  /** Limelight 2: OV5647, 62.5° by 48.9°, AprilTags at about 6 fps at 1280x960. */
  LIMELIGHT_2(1280, 960, 62.5, 48.9, 6, 30),
  /** Limelight 2+: OV5647, 62.5° by 48.9°, AprilTags at about 7 fps at 1280x960. */
  LIMELIGHT_2_PLUS(1280, 960, 62.5, 48.9, 7, 30),
  /** Limelight 3: OV5647, 62.5° by 48.9°, AprilTags at about 20 fps at 1280x960. */
  LIMELIGHT_3(1280, 960, 62.5, 48.9, 20, 25),
  /** Limelight 3A (USB): OV5647, 54.5° by 42°, AprilTags at about 20 fps at 1280x960. */
  LIMELIGHT_3A(1280, 960, 54.5, 42, 20, 25),
  /**
   * Limelight 3G: OV9281 global shutter, 82° by 56.2°, AprilTags at about 20 fps at full
   * resolution. Limelight lists the AprilTag rate at 1280x960, which the 1280x800 sensor can't
   * produce, so this simulates 1280x800.
   */
  LIMELIGHT_3G(1280, 800, 82, 56.2, 20, 25),
  /** Limelight 4: OV9281 global shutter, 82° by 56.2°, AprilTags at about 40 fps at 1280x800. */
  LIMELIGHT_4(1280, 800, 82, 56.2, 40, 20),
  /**
   * Luma P1: OV9281 global shutter running PhotonVision, 80° by 56°, AprilTags at about 60 fps at
   * 1280x800 (decimate 2).
   */
  LUMA_P1(1280, 800, 80, 56, 60, 20),
  /** Luma P2: the same camera and performance as the Luma P1. */
  LUMA_P2(1280, 800, 80, 56, 60, 20),
  /**
   * ThriftyCam with its stock 80° lens: OmniVision OG02B1B global shutter, 1600x1304 at 70 fps
   * (firmware 5.10 or newer). The lens is specified as 71° by 48° on a different image size; on this
   * sensor its 4.1 mm focal length and 3 µm pixels give 60.7° by 51°, which this simulates.
   */
  THRIFTYCAM(1600, 1304, 60.7, 51.0, 70, 30),
  /** Thriftiest Cam: OV9281 global shutter, 1280x800 at 120 fps, 82° by 56°. */
  THRIFTIEST_CAM(1280, 800, 82, 56, 120, 30),
  /**
   * Arducam OV9281 USB camera (B0332), the common PhotonVision camera: 1280x800 at 100 fps, 70°
   * horizontal field of view. Arducam's manual says 120 fps and resellers say 100, so this uses
   * 100. Arducam doesn't give the vertical field of view, so it comes from the 16:10 sensor: 47.3°.
   */
  ARDUCAM_OV9281(1280, 800, 70, 47.3, 100, 30),
  /**
   * AndyMark ELP Fisheye Lens Camera (am-5170, ELP-USB8MP02G-L180): Sony IMX179 color rolling
   * shutter with a 180° fisheye lens, simulated at 1280x960 and 20 fps (ELP's rate for that mode;
   * 3264x2448 runs at 15 fps).
   *
   * <p>PhotonVision simulates a pinhole lens, which can't see 180°. This model keeps the real lens's
   * magnification at the center of the image instead: a fisheye with 180° across 1280 pixels has a
   * focal length of about 407 pixels, and a pinhole lens with that focal length sees 115° by 99.4°.
   * Tags near the center look the size they would on the real camera; tags at the edges of the real
   * camera's view aren't simulated. Calibrate the real camera before trusting its poses.
   */
  ELP_FISHEYE(1280, 960, 115.0, 99.4, 20, 40);

  private final int widthPixels;
  private final int heightPixels;
  private final double horizontalFovDegrees;
  private final double verticalFovDegrees;
  private final double fps;
  private final double averageLatencyMillis;

  CameraModel(
      int widthPixels,
      int heightPixels,
      double horizontalFovDegrees,
      double verticalFovDegrees,
      double fps,
      double averageLatencyMillis) {
    this.widthPixels = widthPixels;
    this.heightPixels = heightPixels;
    this.horizontalFovDegrees = horizontalFovDegrees;
    this.verticalFovDegrees = verticalFovDegrees;
    this.fps = fps;
    this.averageLatencyMillis = averageLatencyMillis;
  }

  /**
   * Image width simulated.
   *
   * @return Width in pixels.
   */
  public int getWidthPixels() {
    return widthPixels;
  }

  /**
   * Image height simulated.
   *
   * @return Height in pixels.
   */
  public int getHeightPixels() {
    return heightPixels;
  }

  /**
   * Horizontal field of view.
   *
   * @return Degrees.
   */
  public double getHorizontalFovDegrees() {
    return horizontalFovDegrees;
  }

  /**
   * Vertical field of view.
   *
   * @return Degrees.
   */
  public double getVerticalFovDegrees() {
    return verticalFovDegrees;
  }

  /**
   * Frames per second simulated.
   *
   * @return Frame rate.
   */
  public double getFps() {
    return fps;
  }

  /**
   * Average latency simulated. An estimate: vendors don't publish one.
   *
   * @return Latency in milliseconds.
   */
  public double getAverageLatencyMillis() {
    return averageLatencyMillis;
  }

  /**
   * PhotonVision camera properties for this model: a distortion-free lens with the published
   * horizontal and vertical field of view, small pixel noise, and the estimated latency.
   *
   * @return New camera properties.
   */
  public SimCameraProperties getSimCameraProperties() {
    double fx = widthPixels / 2.0 / Math.tan(Math.toRadians(horizontalFovDegrees) / 2);
    double fy = heightPixels / 2.0 / Math.tan(Math.toRadians(verticalFovDegrees) / 2);
    var intrinsics =
        MatBuilder.fill(
            Nat.N3(), Nat.N3(), fx, 0, widthPixels / 2.0, 0, fy, heightPixels / 2.0, 0, 0, 1);
    Matrix<N8, N1> distortion = new Matrix<>(Nat.N8(), Nat.N1());
    var properties = new SimCameraProperties();
    properties.setCalibration(widthPixels, heightPixels, intrinsics, distortion);
    properties.setCalibError(0.25, 0.08);
    properties.setFPS(fps);
    properties.setAvgLatencyMs(averageLatencyMillis);
    properties.setLatencyStdDevMs(5);
    return properties;
  }
}
