# How do I install YALL?

YALL is a WPILib 2027 vendordep. A camera's vendor library is needed only for the cameras you construct.

## 1. YALL

In WPILib VS Code: **WPILib: Manage Vendor Libraries → Install new library (online)**, then paste:

```
https://Yet-Another-Software-Suite.github.io/YALL/yall.json
```

## 2. PhotonLib

Required for every team, whatever cameras you use: YALL simulates every camera with PhotonVision. Install it from PhotonVision's vendordep URL in the [PhotonVision docs](https://docs.photonvision.org/).

## 3. WPILib's CameraServer and AprilTag vendordeps

PhotonVision's simulation needs both. They ship with WPILib 2027: **WPILib: Manage Vendor Libraries → Install new libraries (offline)**, then pick **CameraServer** and **AprilTag**.

## 4. Your cameras' libraries

| Camera | Vendordep |
| --- | --- |
| PhotonVision | PhotonLib (already installed in step 2) |
| Limelight | LimelightLib 2: `https://limelightvision.github.io/limelightlib-public/LimelightLib-alpha7.json` |
| Synapse | SynapseLib: `https://danpeled.github.io/Synapse/SynapseLib.json` |

## Verify it worked

After a Gradle build, these should resolve:

```java
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.PhotonVisionWrapper;
```

If the robot program fails in simulation with `photontargetingJNI could not be loaded` or `libcscore.so: cannot open shared object file`, the CameraServer vendordep is missing. A `NoClassDefFoundError` for `AprilTagImageGenerator` means the AprilTag vendordep is missing.

Javadocs for every class are at [yet-another-software-suite.github.io/YALL/javadocs](https://yet-another-software-suite.github.io/YALL/javadocs/).

## See Also

* [Basic Setup](../tutorials/basic-setup.md)
