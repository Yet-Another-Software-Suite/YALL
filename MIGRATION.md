# Migrating from YALL 2026

YALL 2027 replaces the Limelight-only `limelight` package with one camera API for PhotonVision,
Limelight and Synapse. The full guide, with an old-to-new table and before and after code, is in
the docs: [Migrating from YALL 2026](https://yall.yassrobotics.com/documentation/how-to-guides/migrating-from-yall-2026)
([source](docs/documentation/how-to-guides/migrating-from-yall-2026.md)).

In short:

1. Move the project to WPILib 2027.
2. Install the new YALL vendordep, PhotonLib, WPILib's CameraServer and AprilTag vendordeps, and
   LimelightLib 2.
3. Wrap each Limelight: `new LimelightWrapper(new Limelight("limelight"), config)`, with the camera
   offset, pose estimation mode (MegaTag1 is `TAGS_ONLY`, MegaTag2 is `HEADING_ASSISTED`) and camera
   model in a `VisionCameraConfig`.
4. Each loop, in `Robot.robotPeriodic()`: `setRobotOrientation(...)`, `update()`, then
   `addVisionMeasurements(poseEstimator)`.
5. Limelight settings YALL doesn't cover go to `camera.getCamera()`, LimelightLib 2's `Limelight`.

The old library is at the [`pre-rewrite` tag](https://github.com/Yet-Another-Software-Suite/YALL/tree/pre-rewrite).
