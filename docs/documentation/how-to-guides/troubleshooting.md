---
icon: screwdriver-wrench
---

# Troubleshooting

Find what you see, then try the fixes in order. Most problems show up in NetworkTables under `YALL/<camera name>`: `connected`, `targetCount`, `acceptedPoses`, `rejectedPoses` and `rejectionReason`. See [Telemetry](../understanding/telemetry.md).

## The build fails

* **`PhotonCamera`, `Limelight` or `SynapseCamera` can't be found:** that camera's vendordep is missing. See [How do I install YALL?](how-do-i-install-yall.md)
* **`yall` can't be found:** the YALL vendordep is missing.

## Simulation crashes at startup

* **`photontargetingJNI could not be loaded` or `libcscore.so: cannot open shared object file`:** add WPILib's **CameraServer** vendordep.
* **`NoClassDefFoundError` for `AprilTagImageGenerator`:** add WPILib's **AprilTag** vendordep.

## Simulation shows no targets

* **`simulationPeriodic()` doesn't update the world:** call `VisionSim.getDefault().update(truePose)`, or `vision.updateSimulation(truePose)` for a `VisionSystem`, in `Robot.simulationPeriodic()`. Commands v3 mechanisms have no `simulationPeriodic()`, so put it in `Robot.java`.
* **The robot faces away from every AprilTag:** turn on the [simulated video stream](../tutorials/simulating-cameras.md#see-what-the-camera-sees) with the wireframe to see where the camera points.
* **The camera has simulation turned off:** remove `withSimulation(false)`.

## The real camera shows no targets

* **`connected` is false:** the name in code doesn't match the camera's name, the camera has no power, or it's not on the robot's network.
* **`connected` is true but `targetCount` is 0:** the camera isn't running an AprilTag pipeline, or the picture is too dark or blurry. Tune the camera: PhotonVision [Camera Tuning / Input](https://docs.photonvision.org/en/latest/docs/pipelines/input.html), Limelight [Tracking AprilTags](https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltags), or [Camera Sensor Settings](../tuning/camera-sensor-settings.md).

## Every pose guess is rejected

Read `YALL/<camera name>/rejectionReason`:

| Reason | Usually means | Fix |
| --- | --- | --- |
| `too old` | The camera's clock isn't synced with the robot's, or the camera is very slow | Check the camera's time sync in its own tools, or raise `withMaxAge` |
| `timestamp in the future` | Same clock problem | Same fix |
| `off the field` | Wrong camera offset or wrong field | Measure `withRobotToCamera` again; check `withField` |
| `off the floor` | Wrong camera height or tilt | Measure the up distance and pitch again |
| `ambiguous` | One far tag seen nearly straight on | Normal now and then; see more tags, or use `HEADING_ASSISTED` |
| `tags too far` | Tags farther than 4 m (one tag) or 8 m (several) | Normal at range; raise `withMaxTagDistance` if you trust them |
| `spinning too fast` | The robot spun faster than 720 °/s | Normal; wait for the robot to slow down |
| `too few tags` | You raised `withMinTagCount` | Lower it, or see more tags |
| `no tags` | The guess used no AprilTags | Normal; it's always rejected |
| `tilted` | You turned on `withMaxTilt` and the guess is rolled or pitched | Check the camera's tilt, or raise the limit |
| `too far from the current pose` | You turned on `withMaxDistanceFromPose` before the robot knew where it was | Turn it on only after the pose is right |
| `Limelight rejected it (flags ...)` | A Limelight's own `PoseEstimateConfig` rejected it | See [How do I use vendor-specific settings?](how-do-i-use-vendor-specific-settings.md#pose-estimate-config) |

See [How do I tune the pose filter?](how-do-i-tune-the-pose-filter.md)

## The pose estimate jumps or is off

* **Off by the same amount everywhere:** the camera's forward or left offset is wrong.
* **Off more the farther the tag is:** the camera's tilt (pitch) is wrong, or the camera needs calibrating: [ChArUco Camera Calibration](../tuning/charuco-camera-calibration.md).
* **Jumps when a tag comes into view:** the guesses are trusted too much. Raise the standard deviations in [How do I tune the pose filter?](how-do-i-tune-the-pose-filter.md)
* **Heading-assisted estimates drift:** `setRobotOrientation` isn't called every loop, or the gyro's heading is wrong.

## Left and right are backward

A target right of the crosshair should give a positive yaw, and one above it a positive pitch. If not, the camera is probably mounted upside down or set to a flipped orientation in its own tools. Fix it there.
