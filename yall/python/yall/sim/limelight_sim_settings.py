class LimelightSimSettings:
    """
    Configurable properties describing a simulated Limelight camera, used by LimelightSim.

    The defaults approximate a Limelight 3 running the AprilTag pipeline at 1280x960 and require no
    configuration to get a usable simulation running. Construct one with LimelightSimSettings() and tweak
    only the fields that matter for your camera with the withXXX methods.
    """

    def __init__(self) -> None:
        self.resolutionWidth: float = 1280.0
        self.resolutionHeight: float = 960.0
        self.horizontalFOVDegrees: float = 63.3
        self.verticalFOVDegrees: float = 49.7
        # Edge to edge size of the tag's black square, in meters. 6.5in tags are standard in modern FRC games.
        self.tagSizeMeters: float = 6.5 * 0.0254
        self.maxDetectionRangeMeters: float = 6.0
        self.maxTagIncidenceAngleDegrees: float = 80.0
        self.avgPipelineLatencyMs: float = 40.0
        self.latencyStdDevMs: float = 8.0
        self.avgCaptureLatencyMs: float = 10.0
        self.angleNoiseStdDevDegrees: float = 0.15
        # Baseline standard deviation for a single tag seen at 1 meter. Scaled down as more tags are seen
        # and up with average tag distance.
        self.translationNoiseStdDevMeters: float = 0.03
        self.rotationNoiseStdDevDegrees: float = 1.5
        self.randomSeed: int = 4152026
        self.pipelineType: str = "pipe_fiducial"

    def withResolution(self, widthPixels: float, heightPixels: float) -> "LimelightSimSettings":
        """Set the simulated sensor resolution."""
        self.resolutionWidth = widthPixels
        self.resolutionHeight = heightPixels
        return self

    def withFOV(self, horizontalDegrees: float, verticalDegrees: float) -> "LimelightSimSettings":
        """Set the simulated camera's field of view."""
        self.horizontalFOVDegrees = horizontalDegrees
        self.verticalFOVDegrees = verticalDegrees
        return self

    def withTagSize(self, sizeMeters: float) -> "LimelightSimSettings":
        """Set the physical size of the AprilTags being detected, edge to edge in meters."""
        self.tagSizeMeters = sizeMeters
        return self

    def withMaxDetectionRange(self, rangeMeters: float) -> "LimelightSimSettings":
        """Set the maximum distance a tag may be detected from, in meters."""
        self.maxDetectionRangeMeters = rangeMeters
        return self

    def withMaxTagIncidenceAngle(self, degrees: float) -> "LimelightSimSettings":
        """Set the maximum angle of incidence a tag may be viewed at before it is too edge on to detect."""
        self.maxTagIncidenceAngleDegrees = degrees
        return self

    def withPipelineLatency(self, avgMs: float, stdDevMs: float) -> "LimelightSimSettings":
        """Set the simulated pipeline processing latency ("tl")."""
        self.avgPipelineLatencyMs = avgMs
        self.latencyStdDevMs = stdDevMs
        return self

    def withCaptureLatency(self, avgMs: float) -> "LimelightSimSettings":
        """Set the simulated capture latency ("cl")."""
        self.avgCaptureLatencyMs = avgMs
        return self

    def withAngleNoise(self, stdDevDegrees: float) -> "LimelightSimSettings":
        """Set the noise applied to tx/ty/txnc/tync readings, in degrees."""
        self.angleNoiseStdDevDegrees = stdDevDegrees
        return self

    def withPoseNoise(
        self, translationStdDevMeters: float, rotationStdDevDegrees: float
    ) -> "LimelightSimSettings":
        """Set the noise applied to botpose translation and rotation readings, for a single tag seen at 1 meter."""
        self.translationNoiseStdDevMeters = translationStdDevMeters
        self.rotationNoiseStdDevDegrees = rotationStdDevDegrees
        return self

    def withRandomSeed(self, seed: int) -> "LimelightSimSettings":
        """Set the seed used for the deterministic noise source."""
        self.randomSeed = seed
        return self

    def withPipelineType(self, pipelineType: str) -> "LimelightSimSettings":
        """Set the pipeline type string reported by "getpipetype"."""
        self.pipelineType = pipelineType
        return self

    @staticmethod
    def perfect() -> "LimelightSimSettings":
        """Create LimelightSimSettings with all noise and latency zeroed out, useful for deterministic testing."""
        return (
            LimelightSimSettings()
            .withPipelineLatency(0, 0)
            .withCaptureLatency(0)
            .withAngleNoise(0)
            .withPoseNoise(0, 0)
        )
