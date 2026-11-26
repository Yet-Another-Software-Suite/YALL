package yall.cameras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import yall.results.VisionResult;
import yall.results.VisionTarget;

class SynapseAndVisionResultTest {

  @Test
  void synapseTimestampNormalizesBothNanosecondAndMicrosecondNt4Clocks() {
    double nowSec = 123.456;
    double latencySec = 0.032;

    long nanoTicks = Math.round(nowSec * 1.0e9);
    long microTicks = Math.round(nowSec * 1.0e6);

    assertEquals(
        nowSec - latencySec,
        SynapseWrapper.resolveTimestampSeconds(nanoTicks, nowSec, latencySec),
        1e-6);
    assertEquals(
        nowSec - latencySec,
        SynapseWrapper.resolveTimestampSeconds(microTicks, nowSec, latencySec),
        1e-6);

    // Negative or NaN latency is safely clamped to 0
    assertEquals(
        nowSec, SynapseWrapper.resolveTimestampSeconds(nanoTicks, nowSec, -0.5), 1e-6);
    assertEquals(
        nowSec, SynapseWrapper.resolveTimestampSeconds(nanoTicks, nowSec, Double.NaN), 1e-6);
  }

  @Test
  void visionResultSeparatesAprilTagsAndDetectedObjectsWithoutStreams() {
    var empty = new VisionResult("cam", 10.0, 15.0, List.of());
    assertFalse(empty.hasTargets());
    assertTrue(empty.getBestTarget().isEmpty());
    assertTrue(empty.getAprilTags().isEmpty());
    assertTrue(empty.getDetectedObjects().isEmpty());

    var tag = new VisionTarget(5.0, 2.0, 1.2, 7, -1, 0.0, 0.05, Optional.empty());
    var obj = new VisionTarget(-4.0, -1.0, 3.5, -1, 0, 0.92, Double.NaN, Optional.empty());
    var result = new VisionResult("cam", 10.0, 15.0, List.of(tag, obj));

    assertTrue(result.hasTargets());
    assertEquals(Optional.of(tag), result.getBestTarget());
    assertEquals(List.of(tag), result.getAprilTags());
    assertEquals(List.of(obj), result.getDetectedObjects());
  }

  @Test
  void seededPropertyFuzzSynapseTimestampResolutionAlwaysStaysWithinLatencyOfNow() {
    var rng = new SplittableRandom(20271125L);
    for (int i = 0; i < 1_000; i++) {
      double nowSec = rng.nextDouble(1.0, 10_000.0);
      double latencySec = rng.nextDouble(0.0, 0.15);
      boolean useMicros = rng.nextBoolean();
      long ticks = Math.round(nowSec * (useMicros ? 1.0e6 : 1.0e9));

      double resolved = SynapseWrapper.resolveTimestampSeconds(ticks, nowSec, latencySec);
      assertTrue(Double.isFinite(resolved));
      assertEquals(nowSec - latencySec, resolved, 1e-5);
    }
  }
}
