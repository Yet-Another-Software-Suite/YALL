# YALL vendordep build

Builds the YALL Java library from `../yall/java` against WPILib 2027 and publishes it as a
vendordep.

- `./gradlew build` builds the library, its javadocs and `build/maven/yall.json`.
- `./gradlew test` runs the unit tests, including simulated PhotonVision, Limelight and Synapse
  cameras. The tests are skipped when `build` is in the same command.
- `./gradlew publish` publishes to `build/maven/releases`.

The `vendordeps` folder holds the libraries YALL builds against: PhotonLib, LimelightLib 2,
SynapseLib, and WPILib's CameraServer and AprilTag vendordeps.

See the [main README](../README.md#basic-config) for a basic camera config.
