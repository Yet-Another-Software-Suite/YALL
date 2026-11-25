package yall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import yall.telemetry.TelemetryVerbosity;
import yall.telemetry.VisionTelemetryConfig;
import yall.telemetry.VisionTelemetryField;

class VisionTelemetryConfigTest {

  @Test
  void verbosityTurnsOnItsFieldsAndTheOnesBelow() {
    var low = new VisionTelemetryConfig().withVerbosity(TelemetryVerbosity.LOW);
    assertTrue(low.isEnabled(VisionTelemetryField.ACCEPTED_POSES));
    assertFalse(low.isEnabled(VisionTelemetryField.REJECTED_POSES));
    assertFalse(low.isEnabled(VisionTelemetryField.FILTER_STATS));
    var mid = new VisionTelemetryConfig();
    assertTrue(mid.isEnabled(VisionTelemetryField.REJECTED_POSES));
    assertFalse(mid.isEnabled(VisionTelemetryField.FILTER_STATS));
    var high = new VisionTelemetryConfig().withVerbosity(TelemetryVerbosity.HIGH);
    assertTrue(high.isEnabled(VisionTelemetryField.FILTER_STATS));
    assertTrue(high.isEnabled(VisionTelemetryField.TARGETS));
  }

  @Test
  void fieldsCanBeTurnedOnAndOff() {
    var config =
        new VisionTelemetryConfig()
            .withVerbosity(TelemetryVerbosity.LOW)
            .withField(VisionTelemetryField.TARGETS, true)
            .withField(VisionTelemetryField.CONNECTED, false);
    assertTrue(config.isEnabled(VisionTelemetryField.TARGETS));
    assertFalse(config.isEnabled(VisionTelemetryField.CONNECTED));
  }

  @Test
  void tablePathDefaultsUnderYall() {
    assertEquals("YALL/front", new VisionTelemetryConfig().getTablePath("front"));
    assertEquals(
        "Vision/front",
        new VisionTelemetryConfig().withTablePath("Vision/front").getTablePath("x"));
  }

  @Test
  void cameraConfigTurnsTelemetryOff() {
    assertTrue(new VisionCameraConfig().getTelemetryConfig().isPresent());
    assertFalse(new VisionCameraConfig().withTelemetry(false).getTelemetryConfig().isPresent());
  }
}
