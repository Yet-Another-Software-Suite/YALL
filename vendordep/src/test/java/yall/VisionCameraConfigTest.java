package yall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Inches;
import static org.wpilib.units.Units.Meters;

import org.junit.jupiter.api.Test;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;

class VisionCameraConfigTest {

  @Test
  void measuredOffsetMatchesTheTransform() {
    var config =
        new VisionCameraConfig()
            .withRobotToCamera(
                Inches.of(12),
                Inches.of(-6),
                Inches.of(10),
                Degrees.of(0),
                Degrees.of(-15),
                Degrees.of(180));
    var expected =
        new Transform3d(
            Inches.of(12).in(Meters),
            Inches.of(-6).in(Meters),
            Inches.of(10).in(Meters),
            new Rotation3d(0, Math.toRadians(-15), Math.PI));
    assertEquals(expected, config.getRobotToCamera().orElseThrow());
  }

  @Test
  void negativePitchPointsTheCameraUp() {
    var transform =
        new VisionCameraConfig()
            .withRobotToCamera(
                Meters.zero(),
                Meters.zero(),
                Meters.zero(),
                Degrees.zero(),
                Degrees.of(-30),
                Degrees.zero())
            .getRobotToCamera()
            .orElseThrow();
    // The camera's forward axis, rotated into robot space, should point upward.
    var forward =
        new org.wpilib.math.geometry.Translation3d(1, 0, 0).rotateBy(transform.getRotation());
    assertEquals(0.5, forward.getZ(), 1e-9);
  }
}
