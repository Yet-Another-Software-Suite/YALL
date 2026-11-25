---
icon: download
---

# How do I install YALL?

YALL is a **vendordep**: a library you add to a WPILib project. You add a few, in this order. You only need a camera maker's library for the cameras you actually use.

{% stepper %}
{% step %}
### Add YALL

In WPILib VS Code, open the command palette and choose **WPILib: Manage Vendor Libraries**, then **Install new library (online)**, and paste:

```
https://Yet-Another-Software-Suite.github.io/YALL/yall.json
```

**Check it:** `vendordeps/yall.json` is in your project.
{% endstep %}

{% step %}
### Add PhotonLib

Every team needs PhotonLib, whatever cameras you use: YALL simulates every camera with PhotonVision. Install it from the vendordep URL in the [PhotonVision docs](https://docs.photonvision.org/).

**Check it:** a `photonlib` JSON file is in `vendordeps/`.
{% endstep %}

{% step %}
### Add WPILib's CameraServer and AprilTag

PhotonVision's simulation needs both. They come with WPILib 2027: **WPILib: Manage Vendor Libraries**, then **Install new libraries (offline)**, then pick **CameraServer** and **AprilTag**.

**Check it:** `CameraServer.json` and `AprilTag.json` are in `vendordeps/`.
{% endstep %}

{% step %}
### Add your cameras' libraries

| Camera | Vendordep |
| --- | --- |
| PhotonVision (including Luma, ThriftyCam and Arducam cameras running PhotonVision) | PhotonLib, already added |
| Limelight | LimelightLib 2: `https://limelightvision.github.io/limelightlib-public/LimelightLib-alpha7.json` |
| Synapse | SynapseLib: `https://danpeled.github.io/Synapse/SynapseLib.json` |

**Check it:** build the project (**WPILib: Build Robot Code**). These imports work:

```java
import yall.VisionCamera;
import yall.VisionCameraConfig;
import yall.cameras.PhotonVisionWrapper;
```
{% endstep %}
{% endstepper %}

## If it did not work

* **Simulation crashes with `photontargetingJNI could not be loaded` or `libcscore.so: cannot open shared object file`:** the CameraServer vendordep is missing.
* **`NoClassDefFoundError` for `AprilTagImageGenerator`:** the AprilTag vendordep is missing.
* **`PhotonCamera`, `Limelight` or `SynapseCamera` can't be found:** that camera's vendordep is missing.

Start using it in [Basic Setup](../tutorials/basic-setup.md). Javadocs for every class are at [YALL Javadocs](https://yet-another-software-suite.github.io/YALL/javadocs/).
