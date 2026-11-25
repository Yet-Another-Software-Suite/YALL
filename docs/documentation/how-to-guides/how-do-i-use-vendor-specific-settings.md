---
icon: sliders
---

# How do I use vendor-specific settings?

Each camera maker's library can do things the others can't, like a Limelight's LEDs or snapshots. YALL covers what PhotonVision, Limelight and Synapse have in common. For everything else, reach the vendor's library directly:

* **`getCamera()`** returns the vendor's camera object: a `PhotonCamera`, a LimelightLib `Limelight` or a `SynapseCamera`. Each wrapper's `getCamera()` returns its own type, so no cast is needed.
* **`withVendorConfig(...)`** passes a vendor config object to the wrapper, as YAMS does for motor controllers. Each wrapper takes the types listed below and throws `IllegalArgumentException` for anything else.

```java
LimelightWrapper back = new LimelightWrapper(new Limelight("limelight-back"), config);
Limelight limelight = back.getCamera();
```

{% hint style="warning" %}
The wrapper reads the vendor's result queue in `update()`. Don't read it yourself, or the wrapper misses those frames: on a `PhotonCamera`, don't call `getAllUnreadResults()`; on a `Limelight`, don't call `readResultsQueue()`, `readPoseEstimateQueue()` or `readAcceptedPoseEstimates()`. Limelight's `getLatestResults()` is safe.
{% endhint %}

{% hint style="info" %}
In simulation the vendor object isn't simulated: YALL's simulated results come from PhotonVision. Vendor settings still go to the vendor object, but there is no camera behind it, so they have no effect on simulated results.
{% endhint %}

## Limelight

### Pose estimate config

`withVendorConfig` takes LimelightLib's `PoseEstimateConfig`. Limelight's own pose filter and standard deviations then replace YALL's [`VisionPoseFilter`](https://yall.yassrobotics.com/api-reference/java-reference/filter/vision-pose-filter) for that camera. The config applies to MegaTag1 and MegaTag2.

```java
config.withVendorConfig(PoseEstimateConfig.defaultMT2().withMinTagCount(2));
```

Rejected estimates show up in `getRejectedPoses()`, with a reason like `Limelight rejected it (flags 4)`.

### Pipeline override

`withVendorConfig` also takes a `PipelineConfiguration`, which is applied as a pipeline override.

### AprilTag ID filters

Ignore tags you never want to localize off of, or prefer one tag for `tx`/`ty` targeting:

```java
limelight.setFiducialIDFiltersOverride(new int[] {17, 18, 19, 20, 21, 22});
limelight.setPriorityTagIDOverride(7);
```

Clear them with `clearFiducialIDFiltersOverride()` and `clearPriorityTagIDOverride()`.

### Less heat while disabled

`setThrottle(n)` makes the Limelight skip `n` frames between processed ones. LimelightLib suggests 100 to 200 while disabled:

```java
@Override
public void disabledInit() {
  limelight.setThrottle(150);
}

@Override
public void teleopInit() {
  limelight.setThrottle(0);
}
```

Pair it with `limelight.withStaleFrameThreshold(seconds)` so a throttled camera isn't reported as stale: YALL's `isConnected()` uses LimelightLib's `isConnected()`.

### Snapshots and rewind

```java
limelight.triggerSnapshot();          // the Limelight rate-limits this
limelight.setRewindEnabled(true);
limelight.triggerRewindCapture(10);   // the last 10 seconds, up to 165
```

`limelight.getLatestResults().rewind` reports the rewind buffer: `enabled`, `storedSeconds`, `bufferUsage`, `flushing`.

### LEDs, IMU and crop window

```java
limelight.setLEDMode(LEDMode.FORCE_OFF);  // PIPELINE_CONTROL, FORCE_OFF, FORCE_BLINK, FORCE_ON
limelight.setIMUMode(IMUMode.INTERNAL_EXTERNAL_ASSIST);
limelight.setCropWindowOverride(-0.5, 0.5, -0.5, 0.5); // x min, x max, y min, y max, each -1 to 1
```

See [AprilTag Localization Tuning](../tuning/apriltag-and-megatag2-tuning.md) for the IMU modes.

### Raw results

Everything Limelight reports, including classifier, barcode and OCR results that YALL doesn't map to [`VisionTarget`](https://yall.yassrobotics.com/api-reference/java-reference/results/vision-target), is in `limelight.getLatestResults()`.

## PhotonVision

[`PhotonVisionWrapper`](https://yall.yassrobotics.com/api-reference/java-reference/cameras/photonvision-wrapper) takes no vendor configs. Use the `PhotonCamera`:

```java
PhotonCamera photon = front.getCamera();
photon.setDriverMode(true);
photon.setLED(VisionLEDMode.kOff);
photon.takeOutputSnapshot();
```

PhotonVision's pose estimator for the real camera's results is `front.getPhotonPoseEstimator()`. In simulation, the camera's [`SimulatedCamera`](https://yall.yassrobotics.com/api-reference/java-reference/sim/simulated-camera) simulates this same `PhotonCamera`: reach it with `front.getSimulation()`, which has the PhotonVision simulated camera (`getCameraSim()`) and the pose estimator for simulated results (`getPhotonPoseEstimator()`).

## Synapse

[`SynapseWrapper`](https://yall.yassrobotics.com/api-reference/java-reference/cameras/synapse-wrapper) takes no vendor configs. Use the `SynapseCamera`:

```java
SynapseCamera synapse = side.getCamera();
synapse.setSetting(SynapseCamera.Setting.kExposure, 10.0);
synapse.setRecordStatus(true);
```

## Read more

* [Vendor Wrappers & Pass-Through](../understanding/vendor-wrappers-and-pass-through.md)
* [How do I tune the pose filter?](how-do-i-tune-the-pose-filter.md)
