package limelight.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class LimelightSimSettingsTest
{

  @Test
  void defaultsApproximateALimelight3()
  {
    LimelightSimSettings settings = new LimelightSimSettings();

    assertEquals(1280, settings.resolutionWidth);
    assertEquals(960, settings.resolutionHeight);
    assertEquals(63.3, settings.horizontalFOV.getDegrees(), 1e-9);
    assertEquals(49.7, settings.verticalFOV.getDegrees(), 1e-9);
    assertEquals("pipe_fiducial", settings.pipelineType);
  }

  @Test
  void withResolutionUpdatesBothAxes()
  {
    LimelightSimSettings settings = new LimelightSimSettings().withResolution(640, 480);

    assertEquals(640, settings.resolutionWidth);
    assertEquals(480, settings.resolutionHeight);
  }

  @Test
  void withFOVUpdatesBothAxes()
  {
    LimelightSimSettings settings = new LimelightSimSettings().withFOV(82.9, 56.0);

    assertEquals(82.9, settings.horizontalFOV.getDegrees(), 1e-9);
    assertEquals(56.0, settings.verticalFOV.getDegrees(), 1e-9);
  }

  @Test
  void fluentSettersReturnSameInstanceForChaining()
  {
    LimelightSimSettings settings = new LimelightSimSettings();

    assertSame(settings, settings.withResolution(640, 480));
    assertSame(settings, settings.withTagSize(0.2));
    assertSame(settings, settings.withMaxDetectionRange(4.0));
    assertSame(settings, settings.withMaxTagIncidenceAngle(70));
    assertSame(settings, settings.withPipelineLatency(20, 2));
    assertSame(settings, settings.withCaptureLatency(5));
    assertSame(settings, settings.withAngleNoise(0.1));
    assertSame(settings, settings.withPoseNoise(0.01, 0.5));
    assertSame(settings, settings.withRandomSeed(42));
    assertSame(settings, settings.withPipelineType("pipe_neural"));
  }

  @Test
  void perfectZeroesLatencyAndNoise()
  {
    LimelightSimSettings settings = LimelightSimSettings.perfect();

    assertEquals(0, settings.avgPipelineLatencyMs);
    assertEquals(0, settings.latencyStdDevMs);
    assertEquals(0, settings.avgCaptureLatencyMs);
    assertEquals(0, settings.angleNoiseStdDevDegrees);
    assertEquals(0, settings.translationNoiseStdDevMeters);
    assertEquals(0, settings.rotationNoiseStdDevDegrees);
  }

}
