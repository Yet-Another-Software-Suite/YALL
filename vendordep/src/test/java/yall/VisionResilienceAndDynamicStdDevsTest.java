package yall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wpilib.units.Units.Seconds;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.linalg.VecBuilder;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import yall.filter.PoseFilterStats;
import yall.filter.VisionPoseFilter;
import yall.results.VisionPoseEstimate;

/**
 * Tests for {@link VisionSystem} active multi-camera disconnect failover/alerts/pipeline helpers
 * and {@link VisionPoseFilter} custom std-dev calculators and clamped stats calibration.
 */
class VisionResilienceAndDynamicStdDevsTest {

  private static final int APRILTAG_PIPELINE = 0;
  private static final int GAME_PIECE_PIPELINE = 2;

  private static final class StubCamera extends VisionCamera {
    private final AtomicBoolean connected = new AtomicBoolean(true);
    private int pipelineIndex = GAME_PIECE_PIPELINE;

    StubCamera(String name) {
      super(name, new VisionCameraConfig(), null);
    }

    void setConnected(boolean isConnected) {
      connected.set(isConnected);
    }

    @Override
    protected List<VisionFrame> readFrames() {
      return List.of();
    }

    @Override
    protected void applyPipelineIndex(int index) {
      this.pipelineIndex = index;
    }

    @Override
    protected int readPipelineIndex() {
      return pipelineIndex;
    }

    @Override
    protected boolean readConnected() {
      return connected.get();
    }

    @Override
    protected void applyRobotOrientation(
        double timestampSeconds, Rotation3d orientation, double yawRateDegreesPerSecond) {}

    @Override
    protected void applyRobotToCamera(org.wpilib.math.geometry.Transform3d robotToCamera) {}

    @Override
    public Object getCamera() {
      return this;
    }
  }

  @BeforeAll
  static void initHal() {
    HAL.initialize();
  }

  @AfterAll
  static void shutdownHal() {
    HAL.shutdown();
  }

  @Test
  void disconnectFailoverSwitchesSurvivingCameraPipelineAndRestoresOnReconnect() {
    StubCamera primary = new StubCamera("front");
    StubCamera secondary = new StubCamera("intake");
    primary.setPipelineIndex(APRILTAG_PIPELINE);
    secondary.setPipelineIndex(GAME_PIECE_PIPELINE);

    List<String> events = new ArrayList<>();
    try (VisionSystem system =
        new VisionSystem(primary, secondary)
            .withDisconnectDebounce(Seconds.of(1.5))
            .withDisconnectAlerts(true)
            .onCameraDisconnected(
                cam -> {
                  events.add("down:" + cam.getName());
                  if (cam.getName().equals("front")) {
                    secondary.setPipelineIndex(APRILTAG_PIPELINE);
                  }
                })
            .onCameraReconnected(
                cam -> {
                  events.add("up:" + cam.getName());
                  if (cam.getName().equals("front")) {
                    secondary.setPipelineIndex(GAME_PIECE_PIPELINE);
                  }
                })) {

      system.update(10.0);
      assertEquals(2, system.getConnectedCameraCount());
      assertTrue(system.hasConnectedCamera());
      assertTrue(system.getDisconnectedCameras().isEmpty());

      // Brief 0.5 s glitch (< 1.5 s debounce) must NOT trigger failover.
      primary.setConnected(false);
      system.update(10.5);
      system.update(11.0);
      assertTrue(events.isEmpty());
      assertEquals(GAME_PIECE_PIPELINE, secondary.getPipelineIndex());

      // Recovers before debounce expires: timer resets.
      primary.setConnected(true);
      system.update(11.2);
      assertTrue(events.isEmpty());

      // Sustained disconnect >= 1.5 s triggers failover exactly once.
      primary.setConnected(false);
      system.update(12.0);
      system.update(13.4);
      assertTrue(events.isEmpty());
      system.update(13.5);
      system.update(14.0);
      assertEquals(List.of("down:front"), events);
      assertEquals(APRILTAG_PIPELINE, secondary.getPipelineIndex());
      assertEquals(1, system.getConnectedCameraCount());
      assertEquals(List.of(secondary), system.getConnectedCameras());
      assertEquals(List.of(primary), system.getDisconnectedCameras());

      // Reconnect restores pipeline exactly once.
      primary.setConnected(true);
      system.update(15.0);
      system.update(15.2);
      assertEquals(List.of("down:front", "up:front"), events);
      assertEquals(GAME_PIECE_PIPELINE, secondary.getPipelineIndex());
      assertEquals(2, system.getConnectedCameraCount());
    }
  }

  @Test
  void cameraLookupAndNamedPipelineHelperWorkAndValidateDebounce() {
    StubCamera front = new StubCamera("front");
    try (VisionSystem system = new VisionSystem(front)) {
      assertEquals(Optional.of(front), system.getCamera("front"));
      assertTrue(system.getCamera("missing").isEmpty());
      assertTrue(system.setPipelineIndex("front", 3));
      assertEquals(3, front.getPipelineIndex());
      assertFalse(system.setPipelineIndex("missing", 1));
      assertThrows(
          IllegalArgumentException.class, () -> system.withDisconnectDebounce(Seconds.of(-0.1)));
    }
  }

  @Test
  void customStdDevCalculatorOverridesBuiltInAndFallsBackOnInvalidOutput() {
    VisionPoseFilter filter = new VisionPoseFilter();
    VisionPoseEstimate est =
        VisionPoseEstimate.unfiltered(
            "cam", new Pose3d(4, 3, 0, Rotation3d.ZERO), 10.0, List.of(1, 2, 3), 2.0, 0.04, false);

    // 6328-style d^1.5 / tagCount custom covariance.
    filter.withStdDevCalculator(
        e -> {
          double xy = 0.02 * Math.pow(e.averageTagDistanceMeters(), 1.5) / e.tagCount();
          return VecBuilder.fill(xy, xy, 0.5);
        });
    VisionPoseEstimate weighted = filter.applyStdDevs(est);
    double expectedXy = 0.02 * Math.pow(2.0, 1.5) / 3.0;
    assertEquals(expectedXy, weighted.stdDevs().get(0, 0), 1e-9);
    assertEquals(0.5, weighted.stdDevs().get(2, 0), 1e-9);

    // Invalid (NaN / non-positive) output safely falls back to built-in model.
    filter.withStdDevCalculator(e -> VecBuilder.fill(Double.NaN, 0.2, 0.5));
    VisionPoseEstimate fallback = filter.applyStdDevs(est);
    assertTrue(Double.isFinite(fallback.stdDevs().get(0, 0)));
    assertTrue(fallback.stdDevs().get(0, 0) > 0.0);

    // Null restores default built-in model.
    filter.withStdDevCalculator(null);
    VisionPoseEstimate restored = filter.applyStdDevs(est);
    assertEquals(fallback.stdDevs().get(0, 0), restored.stdDevs().get(0, 0), 1e-9);
  }

  @Test
  void applySuggestedBaseStdDevsEnforcesMinSamplesAndClampingWithFuzz() {
    VisionPoseFilter filter = new VisionPoseFilter();
    PoseFilterStats stats = new PoseFilterStats();
    Matrix<N3, N1> minBase = VecBuilder.fill(0.05, 0.05, 0.1);
    Matrix<N3, N1> maxBase = VecBuilder.fill(1.0, 1.0, 2.0);

    // Too few samples -> false.
    stats.recordAccepted(
        VisionPoseEstimate.unfiltered(
            "cam", new Pose3d(2.0, 2.0, 0, Rotation3d.ZERO), 1.0, List.of(1, 2), 2.0, 0.02, false));
    assertFalse(filter.applySuggestedBaseStdDevs(stats, 20, minBase, maxBase));

    // Seeded stationary samples with small jitter -> updates and clamps into [minBase, maxBase].
    SplittableRandom rng = new SplittableRandom(20270001L);
    for (int i = 0; i < 40; i++) {
      double dx = rng.nextDouble(-0.01, 0.01);
      double dy = rng.nextDouble(-0.01, 0.01);
      double dYaw = rng.nextDouble(-0.005, 0.005);
      stats.recordAccepted(
          VisionPoseEstimate.unfiltered(
              "cam",
              new Pose3d(2.0 + dx, 2.0 + dy, 0, new Rotation3d(0, 0, dYaw)),
              2.0 + i * 0.02,
              List.of(1, 2),
              2.0,
              0.02,
              false));
    }

    assertTrue(filter.applySuggestedBaseStdDevs(stats, 20, minBase, maxBase));
    VisionPoseEstimate probe =
        filter.applyStdDevs(
            VisionPoseEstimate.unfiltered(
                "cam",
                new Pose3d(2.0, 2.0, 0, Rotation3d.ZERO),
                5.0,
                List.of(1, 2),
                0.0,
                0.02,
                false));
    assertTrue(probe.stdDevs().get(0, 0) >= 0.05 && probe.stdDevs().get(0, 0) <= 1.0);
    assertTrue(probe.stdDevs().get(1, 0) >= 0.05 && probe.stdDevs().get(1, 0) <= 1.0);
    assertTrue(probe.stdDevs().get(2, 0) >= 0.1 && probe.stdDevs().get(2, 0) <= 2.0);
  }
}
