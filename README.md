# Yet Another Limelight Library (YALL)

**Yet Another Limelight Library** is an improved version of the [LimelightHelpers](https://github.com/LimelightVision/limelightlib-wpijava) script released by LimelightVision for use in FIRST Robotics Competition (FRC) teams. This library provides enhanced functionality and additional features for easier integration and control of Limelight vision systems on your robot.

## Features

- **Improved API**: Simplified and intuitive API for controlling the Limelight camera and retrieving vision data.
- **Easy Configuration**: Easy-to-use configuration options to fine-tune the vision system according to your robot’s needs.

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Examples](#examples)
- [Contributing](#contributing)
- [License](#license)

## Installation

### 1. Install the Library via WPILib Vendordep
```
https://Yet-Another-Software-Suite.github.io/YALL/yall.json
```

### 2. Manual Installation

If you prefer to install the library manually, download the latest version from the [releases page](https://github.com/BroncBotz3481/YALL/releases) and add it to your FRC project’s `src/main` directory.

## Usage

Once installed, you can begin using LimelightLib in your code by importing it:

```java
import limelight.Limelight;
```

### Basic Setup

```java
Limelight limelight = new Limelight("limelight");

// Set the limelight to use Pipeline LED control, with the Camera offset of 0, and save.
limelight.getSettings()
         .withLimelightLEDMode(LEDMode.PipelineControl)
         .withCameraOffset(Pose3d.kZero)
         .save();

// Get target data
limelight.getLatestResults().ifPresent((LimelightResults result) -> {
    for (NeuralClassifier object : result.targets_Classifier)
    {
        // Classifier says its a algae.
        if (object.className.equals("algae"))
        {
            // Check pixel location of algae.
            if (object.ty > 2 && object.ty < 1)
            {
              // Algae is valid! do stuff!
            }
        }
    }
});


// Required for megatag2 in periodic() function before fetching pose.
limelight.getSettings()
		 .withRobotOrientation(new Orientation3d(gyro.getRotation3d(),
												 new AngularVelocity3d(DegreesPerSecond.of(gyro.getPitchVelocity()),
																	   DegreesPerSecond.of(gyro.getRollVelocity()),
																	   DegreesPerSecond.of(gyro.getYawVelocity()))))
		 .save();

// Get MegaTag2 pose
Optional<PoseEstimate> visionEstimate = limelight.createPoseEstimator(EstimationMode.MEGATAG2).getPoseEstimate();
// If the pose is present
visionEstimate.ifPresent((PoseEstimate poseEstimate) -> {
  // Add it to the pose estimator.
  poseEstimator.addVisionMeasurement(poseEstimate.pose.toPose2d(), poseEstimate.timestampSeconds);
});

// Alternatively you can do
Optional<PoseEstimate>  BotPose.BLUE_MEGATAG2.get(limelight);
// If the pose is present
visionEstimate.ifPresent((PoseEstimate poseEstimate) -> {
    // Add it to the pose estimator.
    poseEstimator.addVisionMeasurement(poseEstimate.pose.toPose2d(), poseEstimate.timestampSeconds);
    });
```

### Advanced Features

- **Settings**: To easily configure settings you can chain options:

```java
// Set the limelight to use Pipeline LED control, with the Camera offset of 0, and save.
limelight.getSettings()
         .withLimelightLEDMode(LEDMode.PipelineControl)
         .withCameraOffset(Pose3d.kZero);
```

- **Pose Estimates**: To fetch `PoseEstimate` objects you can use this enum:

```java
Optional<PoseEstimate>  BotPose.BLUE_MEGATAG2.get(limelight);
```


## API Documentation

* Java Docs are at [https://Yet-Another-Software-Suite.github.io/YALL/javadocs/](https://yet-another-software-suite.github.io/YALL/javadocs/)

### Limelight

#### `Limelight(String cameraName)`

Constructor for initializing the Limelight object.

## Examples

### Example 1: Classifier Target Tracking

```java
Limelight limelight = new Limelight("limelight");
// Get the results
limelight.getLatestResults().ifPresent((LimelightResults result) -> {
    for (NeuralClassifier object : result.targets_Classifier)
    {
        // Classifier says its a coral.
        if (object.className.equals("coral"))
        {
            // Check pixel location of coral.
            if (object.ty > 2 && object.ty < 1)
            {
            // Coral is valid! do stuff!
            }
        }
    }
});
```

### Example 2: Vision Pose Estimation with MegaTag2

```java
Limelight limelight = new Limelight("limelight");

// Required for megatag2 in periodic() function before fetching pose.
limelight.getSettings()
		 .withRobotOrientation(new Orientation3d(gyro.getRotation3d(),
												 new AngularVelocity3d(DegreesPerSecond.of(gyro.getPitchVelocity()),
																	   DegreesPerSecond.of(gyro.getRollVelocity()),
																	   DegreesPerSecond.of(gyro.getYawVelocity()))))
		 .save();
		 
// Get MegaTag2 pose
Optional<PoseEstimate> visionEstimate = poseEstimator.getPoseEstimate();
// If the pose is present
visionEstimate.ifPresent((PoseEstimate poseEstimate) -> {
    // Add it to the pose estimator.
    poseEstimator.addVisionMeasurement(poseEstimate.pose.toPose2d(), poseEstimate.timestampSeconds);
});
```

## Contributing

We welcome contributions from the FRC community! To contribute to the Yet Another Limelight Library project, please follow these steps:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature-name`).
3. Commit your changes (`git commit -am 'Add new feature'`).
4. Push to the branch (`git push origin feature-name`).
5. Open a pull request to the main repository.

Please make sure your code follows the project's style guidelines and includes tests for new features.

## License

Yet Another Limelight Library is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

Enjoy using Yet Another Limelight Library to enhance your FRC robot’s vision capabilities! For further assistance or to report issues, please visit our [GitHub Issues page](https://github.com/BroncBotz3481/YALL/issues).
