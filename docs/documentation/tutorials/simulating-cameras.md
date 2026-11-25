# Simulating Cameras

When the robot program runs in simulation, every YALL camera returns simulated results, whatever its vendor. The same `update()`, `getTargets()` and `getEstimatedPose()` calls you use on the robot work unchanged.

{% hint style="info" %}
YALL simulates every camera with PhotonVision's camera simulation (`VisionSystemSim` and `PhotonCameraSim`). Credit to the PhotonVision project. This is why PhotonLib is a required vendordep even if you only use Limelights.
{% endhint %}

## How it works

* Each camera joins a simulated world, `VisionSim`. Cameras join `VisionSim.getDefault()` unless their config names another one.
* The world holds the field's AprilTags, plus any game pieces you add.
* Every loop, you give the world the robot's **true** pose. Each camera that is due a frame projects the targets through a simulated lens, adds noise and latency, and queues the result.
* The camera's next `update()` reads the queued results, exactly as it reads real ones.

A PhotonVision camera's simulated results go through its own `PhotonCamera`. A Limelight or Synapse camera gets a private simulated PhotonVision camera named `YALL-<camera name>`.

## 1. Update the world every loop

```java
@Override
public void simulationPeriodic() {
  drivetrainSim.update(0.02);
  VisionSim.getDefault().update(drivetrainSim.getPose());
}
```

Pass the simulated robot's true pose, not your pose estimator's. With several cameras in a `VisionSystem`, `vision.updateSimulation(truePose)` updates every world its cameras are in.

## 2. Match the simulated camera to the real one

Tell each camera what it is with `withCameraModel`, so the simulation uses its published resolution,
field of view and frame rate:

```java
config.withCameraModel(CameraModel.LIMELIGHT_3G);
```

| `CameraModel` | Resolution | Field of view (H × V) | fps |
| --- | --- | --- | --- |
| `LIMELIGHT_2` | 1280x960 | 62.5° × 48.9° | 6 |
| `LIMELIGHT_2_PLUS` | 1280x960 | 62.5° × 48.9° | 7 |
| `LIMELIGHT_3` | 1280x960 | 62.5° × 48.9° | 20 |
| `LIMELIGHT_3A` | 1280x960 | 54.5° × 42° | 20 |
| `LIMELIGHT_3G` | 1280x800 | 82° × 56.2° | 20 |
| `LIMELIGHT_4` | 1280x800 | 82° × 56.2° | 40 |
| `LUMA_P1` | 1280x800 | 80° × 56° | 60 |
| `LUMA_P2` | 1280x800 | 80° × 56° | 60 |
| `THRIFTYCAM` | 1600x1304 | 60.7° × 51° | 70 |
| `THRIFTIEST_CAM` | 1280x800 | 82° × 56° | 120 |
| `ARDUCAM_OV9281` | 1280x800 | 70° × 47.3° | 100 |

The specs come from Limelight's, Luma's, The Thrifty Bot's and Arducam's published pages:

* **Frame rates:** Limelights and Lumas run at the AprilTag rate their vendor publishes. USB cameras (ThriftyCam, Thriftiest Cam, Arducam) run at their full-resolution capture rate; your coprocessor may track AprilTags slower.
* **Limelight 3G:** Limelight lists its AprilTag rate at 1280x960, which its 1280x800 sensor can't produce, so it is simulated at 1280x800.
* **ThriftyCam:** its stock lens is specified as 71° × 48° on a different image size. On the ThriftyCam's sensor, the lens's 4.1 mm focal length gives 60.7° × 51°.
* **Arducam OV9281 (B0332):** Arducam gives only the 70° horizontal field of view, so the vertical one follows from the 16:10 sensor. Arducam's manual says 120 fps and resellers say 100.
* **Latency:** no vendor publishes one, so each model uses an estimate. Limelights report their real latency on the robot.

Without a model, a Limelight is simulated as a Limelight 4, and PhotonVision and Synapse cameras as an Arducam OV9281.

For any other camera, set the properties yourself:

```java
config.withCameraProperties(1280, 720, Rotation2d.fromDegrees(70), 30); // width, height, diagonal FOV, FPS
```

or pass a full PhotonVision `SimCameraProperties`, for example with a slower frame rate than the model's:

```java
var properties = CameraModel.ARDUCAM_OV9281.getSimCameraProperties();
properties.setFPS(30); // what your coprocessor tracks AprilTags at
config.withCameraProperties(properties);
```

Synapse also uses these properties on the real robot, to turn pixel positions into angles, so set the model or properties to match the real camera.

## 3. Watch it

* `VisionSim.getDefault().getDebugField()` returns a `Field2d` with the robot, the cameras and the targets they see. PhotonVision also publishes it as `VisionSystemSim-YALL/Sim Field`.
* Each camera publishes its accepted and rejected poses under `YALL/<camera name>`. See [Telemetry](../understanding/telemetry.md).

## Simulated video

Rendering simulated video costs CPU, so it is off by default. Turn it on per camera:

```java
config.withSimulatedVideoStreams(true);
```

## Other fields and worlds

The default world holds the AprilTags of WPILib's `Fields.DEFAULT_FIELD`. For another field, replace the default world before creating cameras, and give the cameras the same field:

```java
Field field = Field.loadField(Fields.FRC_2026_REBUILT_WELDED);
VisionSim.setDefault(new VisionSim("YALL", field));
config.withField(field);
```

Or give one camera its own world with `config.withSimulation(myVisionSim)`.

## Turning simulation off

`config.withSimulation(false)` makes a camera read its vendor library even in simulation, for example to test against a real camera from a laptop.

## Game pieces

See [Object Detection](object-detection.md#simulating-game-pieces).

## Testing in unit tests

Simulation runs in JUnit too. Pause the simulated clock and step it yourself:

```java
HAL.initialize();
SimHooks.pauseTiming();
for (int i = 0; i < 50; i++) {
  SimHooks.stepTiming(0.02);
  vision.updateSimulation(robotPose);
  vision.update();
}
```

The YALL [example robot](https://github.com/Yet-Another-Software-Suite/YALL/tree/main/example) has a test that drives a simulated kitbot and checks that vision keeps its pose estimate closer to the truth than odometry alone.

## See Also

* [Basic Setup](basic-setup.md)
* [Vendor Wrappers & Pass-Through](../understanding/vendor-wrappers-and-pass-through.md)
