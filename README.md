> WARNING: The following was made by ChatGPT to look pretty, it probably isn't accurate

# LimelightLib

**LimelightLib** is an improved version of the [LimelightHelpers](https://github.com/limelightvision/limelight-helpers) script released by LimelightVision for use in FIRST Robotics Competition (FRC) teams. This library provides enhanced functionality and additional features for easier integration and control of Limelight vision systems on your robot.

## Features

- **Improved API**: Simplified and intuitive API for controlling the Limelight camera and retrieving vision data.
- **Automatic Target Detection**: Built-in functionality for detecting and tracking targets, including field of view and distance estimations.
- **Smoothing & Filtering**: Advanced methods to smooth target data, reducing noise in vision processing.
- **Distance Calculation**: Methods for calculating accurate robot-to-target distances based on Limelight's angle data.
- **Customizable Target Processing**: Easy-to-use configuration options to fine-tune the vision system according to your robot’s needs.

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Examples](#examples)
- [Contributing](#contributing)
- [License](#license)

## Installation

### 1. Install the Library via WPILib Vendordep

### 2. Manual Installation

If you prefer to install the library manually, download the latest version from the [releases page](https://github.com/your-repo/limelightlib/releases) and add it to your FRC project’s `lib` directory.

## Usage

Once installed, you can begin using LimelightLib in your code by importing it:

```java
import com.example.limelightlib.Limelight;
```

### Basic Setup

```java
Limelight limelight = new Limelight();

// Set the default camera mode (Driver or Vision)
limelight.setCameraMode(Limelight.CameraMode.VISION);

// Get target data
boolean hasTarget = limelight.hasTarget();
double targetX = limelight.getTargetX();  // Horizontal angle offset
double targetY = limelight.getTargetY();  // Vertical angle offset
double targetArea = limelight.getTargetArea();  // Area of the target
double distance = limelight.calculateDistance(targetY);  // Calculate distance from target
```

### Advanced Features

- **Smoothing**: To reduce noise and smooth the values of target detection:

```java
limelight.setSmoothing(0.1); // Set smoothing factor for target data
```

- **Distance Calculation**: Estimate distance to the target based on the vertical angle using a pre-configured equation:

```java
double distance = limelight.calculateDistance(targetY);
```

- **Custom Target Processing**: Fine-tune the filtering for detection:

```java
limelight.setTargetProcessingThreshold(5); // Set the threshold for target detection
```

## API Documentation

* Java Docs are at https://broncbotz3481.github.io/LimelightLib/

### Limelight

#### `Limelight(String cameraName)`

Constructor for initializing the Limelight object.

#### `void setCameraMode(CameraMode mode)`

Sets the camera mode of the Limelight (either `VISION` or `DRIVER`).

#### `boolean hasTarget()`

Returns whether or not the Limelight is currently detecting a target.

#### `double getTargetX()`

Gets the horizontal angle offset to the target.

#### `double getTargetY()`

Gets the vertical angle offset to the target.

#### `double getTargetArea()`

Gets the area of the target detected by the Limelight.

#### `double calculateDistance(double targetY)`

Calculates the distance to the target based on the vertical angle.

#### `void setSmoothing(double factor)`

Sets the smoothing factor for the target data. A lower value means smoother data.

#### `void setTargetProcessingThreshold(int threshold)`

Sets the target processing threshold for filtering false positives in detection.

### CameraMode Enum

- **VISION**: Limelight will be in vision processing mode.
- **DRIVER**: Limelight will be in driver camera mode (for field of view).

## Examples

### Example 1: Simple Target Tracking

```java
Limelight limelight = new Limelight();
limelight.setCameraMode(Limelight.CameraMode.VISION);

if (limelight.hasTarget()) {
    double targetX = limelight.getTargetX();
    double targetY = limelight.getTargetY();
    double distance = limelight.calculateDistance(targetY);
    System.out.println("Target X: " + targetX + " Target Y: " + targetY + " Distance: " + distance);
}
```

### Example 2: Smoothing Target Data

```java
Limelight limelight = new Limelight();
limelight.setCameraMode(Limelight.CameraMode.VISION);
limelight.setSmoothing(0.2);  // Apply smoothing to reduce jitter

if (limelight.hasTarget()) {
    double targetX = limelight.getTargetX();
    System.out.println("Smoothed Target X: " + targetX);
}
```

## Contributing

We welcome contributions from the FRC community! To contribute to the LimelightLib project, please follow these steps:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature-name`).
3. Commit your changes (`git commit -am 'Add new feature'`).
4. Push to the branch (`git push origin feature-name`).
5. Open a pull request to the main repository.

Please make sure your code follows the project's style guidelines and includes tests for new features.

## License

LimelightLib is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

Enjoy using LimelightLib to enhance your FRC robot’s vision capabilities! For further assistance or to report issues, please visit our [GitHub Issues page](https://github.com/your-repo/limelightlib/issues).