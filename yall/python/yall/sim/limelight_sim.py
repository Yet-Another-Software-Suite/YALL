import json
import math
import random
import time
from typing import List, Optional

import ntcore
import robotpy_apriltag
import wpilib
from wpimath import geometry

from ..limelight import Limelight
from ..networktables.util import LimelightUtils
from .limelight_sim_settings import LimelightSimSettings


def _clamp(value: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, value))


def _defaultFieldLayout() -> robotpy_apriltag.AprilTagFieldLayout:
    # Newer robotpy-apriltag releases expose kDefaultField, always pointing at the current season's field.
    # Fall back to the newest field known to whatever version is installed so this still works out of the box
    # on older installs.
    fieldEnum = getattr(robotpy_apriltag.AprilTagField, "kDefaultField", None)
    if fieldEnum is None:
        members = [f for f in robotpy_apriltag.AprilTagField if f.name != "kNumFields"]
        fieldEnum = members[-1]
    return robotpy_apriltag.AprilTagFieldLayout.loadField(fieldEnum)


class _TagObservation:
    """A single simulated tag detection."""

    def __init__(
        self,
        id: int,
        pose: geometry.Pose3d,
        ta: float,
        tx: float,
        ty: float,
        distToCamera: float,
        distToRobot: float,
        ambiguity: float,
        apparentWidthPixels: float,
        pixelsPerDegree: float,
    ) -> None:
        self.id = id
        self.pose = pose
        self.ta = ta
        self.tx = tx
        self.ty = ty
        self.distToCamera = distToCamera
        self.distToRobot = distToRobot
        self.ambiguity = ambiguity
        self.apparentWidthPixels = apparentWidthPixels
        self.pixelsPerDegree = pixelsPerDegree


class LimelightSim:
    """
    Simulates a Limelight running the AprilTag/fiducial pipeline, publishing to the same NetworkTables keys a
    real Limelight would populate ("tx", "ty", "ta", "tv", "tid", "botpose*", "rawfiducials", "t2d",
    "targetpose_*"/"camerapose_*"/"botpose_targetspace", "tl"/"cl"/"getpipe"/"getpipetype", "json", ...) so that
    code written against LimelightData, LimelightTargetData and LimelightPoseEstimator behaves the same in
    simulation as on real hardware.

    Minimal usage::

        limelight = Limelight("limelight")
        limelightSim = LimelightSim(limelight)

        # in robotPeriodic() while RobotBase.isSimulation()
        limelightSim.update(drivebase.getPose())

    By default the current season's official AprilTag field is used and the camera is assumed to be mounted at
    the robot's origin. Call withRobotToCameraTransform() and/or withSettings() to refine it.

    The overall approach (a per camera simulation object that is fed the ground truth robot pose every loop and
    projects field targets into a simple pinhole camera model to publish simulated NetworkTables output) is
    modeled after PhotonVision's VisionSystemSim/PhotonCameraSim simulation classes. Credit to the PhotonVision
    project for that design; this implementation is a from scratch, self contained port of the idea onto the
    Limelight NetworkTables schema and does not depend on PhotonVision at runtime.

    Only the fiducial/AprilTag pipeline is simulated (retroreflective, neural classifier/detector and barcode
    pipelines are not). Robot pose and tag geometry are known exactly by the simulator, so rather than
    re-deriving a pose estimate via solvePnP like the real Limelight firmware does, the published botpose
    values are the ground truth robot pose with configurable Gaussian noise applied. This is an approximation,
    not a physically derived uncertainty model.
    """

    def __init__(
        self, limelight: Limelight, settings: Optional[LimelightSimSettings] = None
    ) -> None:
        self.__limelight: Limelight = limelight
        self.__table: ntcore.NetworkTable = limelight.getNTTable()
        self.__settings: LimelightSimSettings = settings or LimelightSimSettings()
        self.__rng: random.Random = random.Random(self.__settings.randomSeed)
        self.__fieldLayout: robotpy_apriltag.AprilTagFieldLayout = _defaultFieldLayout()
        self.__robotToCamera: geometry.Transform3d = geometry.Transform3d()
        self.__heartbeat: int = 0
        self.__frameIndex: int = 0

        self.__tv: ntcore.NetworkTableEntry = self.__table.getEntry("tv")
        self.__tx: ntcore.NetworkTableEntry = self.__table.getEntry("tx")
        self.__ty: ntcore.NetworkTableEntry = self.__table.getEntry("ty")
        self.__txnc: ntcore.NetworkTableEntry = self.__table.getEntry("txnc")
        self.__tync: ntcore.NetworkTableEntry = self.__table.getEntry("tync")
        self.__ta: ntcore.NetworkTableEntry = self.__table.getEntry("ta")
        self.__tid: ntcore.NetworkTableEntry = self.__table.getEntry("tid")
        self.__tl: ntcore.NetworkTableEntry = self.__table.getEntry("tl")
        self.__cl: ntcore.NetworkTableEntry = self.__table.getEntry("cl")
        self.__tdist: ntcore.NetworkTableEntry = self.__table.getEntry("tdist")
        self.__hb: ntcore.NetworkTableEntry = self.__table.getEntry("hb")
        self.__getpipe: ntcore.NetworkTableEntry = self.__table.getEntry("getpipe")
        self.__getpipetype: ntcore.NetworkTableEntry = self.__table.getEntry("getpipetype")
        self.__json: ntcore.NetworkTableEntry = self.__table.getEntry("json")
        self.__pipelineIndex: ntcore.NetworkTableEntry = self.__table.getEntry("pipeline")

        self.__t2d: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic("t2d").getEntry([])
        self.__rawFiducials: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic(
            "rawfiducials"
        ).getEntry([])
        self.__targetPoseRobotSpace: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic(
            "targetpose_robotspace"
        ).getEntry([])
        self.__targetPoseCameraSpace: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic(
            "targetpose_cameraspace"
        ).getEntry([])
        self.__cameraPoseTargetSpace: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic(
            "camerapose_targetspace"
        ).getEntry([])
        self.__botPoseTargetSpace: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic(
            "botpose_targetspace"
        ).getEntry([])
        self.__cameraPoseRobotSpace: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic(
            "camerapose_robotspace"
        ).getEntry([])
        self.__stddevs: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic("stddevs").getEntry([])
        self.__imu: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic("imu").getEntry([0.0] * 10)
        self.__botpose: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic("botpose").getEntry([])
        self.__botposeRed: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic(
            "botpose_wpired"
        ).getEntry([])
        self.__botposeBlue: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic(
            "botpose_wpiblue"
        ).getEntry([])
        self.__botposeOrb: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic(
            "botpose_orb"
        ).getEntry([])
        self.__botposeOrbBlue: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic(
            "botpose_orb_wpiblue"
        ).getEntry([])
        self.__botposeOrbRed: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic(
            "botpose_orb_wpired"
        ).getEntry([])
        self.__robotOrientationSet: ntcore.DoubleArrayEntry = self.__table.getDoubleArrayTopic(
            "robot_orientation_set"
        ).getEntry([])

    def withRobotToCameraTransform(self, robotToCamera: geometry.Transform3d) -> "LimelightSim":
        """
        Set the transform from the robot's origin to the camera's lens. Equivalent in effect to
        LimelightSettings.withCameraOffset() on real hardware.
        """
        self.__robotToCamera = robotToCamera
        return self

    def withAprilTagFieldLayout(
        self, fieldLayout: robotpy_apriltag.AprilTagFieldLayout
    ) -> "LimelightSim":
        """Override the AprilTag field layout used to source simulated targets."""
        self.__fieldLayout = fieldLayout
        return self

    def withSettings(self, settings: LimelightSimSettings) -> "LimelightSim":
        """Set the camera properties/noise/latency configuration."""
        self.__settings = settings
        self.__rng = random.Random(settings.randomSeed)
        return self

    def update(self, robotPose) -> None:
        """
        Update the simulated Limelight with the robot's current pose, projecting visible AprilTags and
        publishing the resulting NetworkTables data. Call this periodically while running in simulation.

        Accepts either a Pose2d or a Pose3d ground truth robot pose, in meters.
        """
        if isinstance(robotPose, geometry.Pose2d):
            robotPose = geometry.Pose3d(robotPose)

        if not wpilib.RobotBase.isSimulation():
            return

        cameraPose = robotPose.transformBy(self.__robotToCamera)

        visible: List[_TagObservation] = []
        for tag in self.__fieldLayout.getTags():
            observation = self.__project(cameraPose, robotPose, tag)
            if observation is not None:
                visible.append(observation)
        visible.sort(key=lambda observation: observation.ta, reverse=True)

        tagCount = len(visible)
        primary = visible[0] if tagCount > 0 else None

        avgTagDist = 0.0
        avgTagArea = 0.0
        for observation in visible:
            avgTagDist += observation.distToCamera
            avgTagArea += observation.ta
        if tagCount > 0:
            avgTagDist /= tagCount
            avgTagArea /= tagCount

        tagSpan = 0.0
        for i in range(len(visible)):
            for j in range(i + 1, len(visible)):
                distance = visible[i].pose.translation().distance(visible[j].pose.translation())
                tagSpan = max(tagSpan, distance)

        tlMs = max(
            0.0, self.__settings.avgPipelineLatencyMs + self.__rng.gauss(0, 1) * self.__settings.latencyStdDevMs
        )
        clMs = self.__settings.avgCaptureLatencyMs
        totalLatencyMs = tlMs + clMs

        translationStdDev = (
            self.__settings.translationNoiseStdDevMeters * (1 + avgTagDist) / math.sqrt(tagCount)
            if tagCount > 0
            else 0.0
        )
        rotationStdDev = self.__settings.rotationNoiseStdDevDegrees / math.sqrt(tagCount) if tagCount > 0 else 0.0

        mt1Pose = self.__addNoise(robotPose, translationStdDev, rotationStdDev) if tagCount > 0 else robotPose
        mt2Pose = self.__addNoise(robotPose, translationStdDev / 2.0, 0.0) if tagCount > 0 else robotPose

        suppliedOrientation = self.__robotOrientationSet.get()
        if len(suppliedOrientation) >= 1:
            groundTruthRotation = mt2Pose.rotation()
            mt2Pose = geometry.Pose3d(
                mt2Pose.translation(),
                geometry.Rotation3d(
                    groundTruthRotation.X(), groundTruthRotation.Y(), math.radians(suppliedOrientation[0])
                ),
            )

        mt1PoseRed = self.__flipToRed(mt1Pose)
        mt2PoseRed = self.__flipToRed(mt2Pose)

        self.__publishScalarEntries(primary, tagCount, tlMs, clMs)
        self.__publishArrayEntries(
            robotPose,
            cameraPose,
            visible,
            primary,
            tagCount,
            tagSpan,
            avgTagDist,
            avgTagArea,
            totalLatencyMs,
            translationStdDev,
            rotationStdDev,
            mt1Pose,
            mt1PoseRed,
            mt2Pose,
            mt2PoseRed,
        )
        self.__publishJson(
            robotPose,
            cameraPose,
            visible,
            tagCount,
            tagSpan,
            avgTagDist,
            avgTagArea,
            tlMs,
            clMs,
            primary,
            mt1Pose,
            mt1PoseRed,
            mt2Pose,
        )

        self.__heartbeat += 1
        self.__frameIndex += 1

    def __publishScalarEntries(
        self, primary: Optional[_TagObservation], tagCount: int, tlMs: float, clMs: float
    ) -> None:
        self.__tv.setDouble(1 if tagCount > 0 else 0)
        self.__tx.setDouble(primary.tx if primary else 0.0)
        self.__ty.setDouble(primary.ty if primary else 0.0)
        self.__txnc.setDouble(primary.tx if primary else 0.0)
        self.__tync.setDouble(primary.ty if primary else 0.0)
        self.__ta.setDouble(primary.ta if primary else 0.0)
        self.__tid.setDouble(primary.id if primary else -1)
        self.__tl.setDouble(tlMs)
        self.__cl.setDouble(clMs)
        self.__tdist.setDouble(primary.distToCamera if primary else 0.0)
        self.__hb.setDouble(self.__heartbeat)
        pipelineIndex = self.__pipelineIndex.getDouble(0)
        self.__getpipe.setDouble(pipelineIndex)
        self.__getpipetype.setString(self.__settings.pipelineType)

    def __publishArrayEntries(
        self,
        robotPose: geometry.Pose3d,
        cameraPose: geometry.Pose3d,
        visible: List[_TagObservation],
        primary: Optional[_TagObservation],
        tagCount: int,
        tagSpan: float,
        avgTagDist: float,
        avgTagArea: float,
        totalLatencyMs: float,
        translationStdDev: float,
        rotationStdDev: float,
        mt1Pose: geometry.Pose3d,
        mt1PoseRed: geometry.Pose3d,
        mt2Pose: geometry.Pose3d,
        mt2PoseRed: geometry.Pose3d,
    ) -> None:
        rawFiducials: List[float] = []
        for observation in visible:
            rawFiducials.extend(
                [
                    observation.id,
                    observation.tx,
                    observation.ty,
                    observation.ta,
                    observation.distToCamera,
                    observation.distToRobot,
                    observation.ambiguity,
                ]
            )
        self.__rawFiducials.set(rawFiducials)

        def botPoseArray(pose: geometry.Pose3d) -> List[float]:
            return self.__botPoseArray(
                pose, totalLatencyMs, tagCount, tagSpan, avgTagDist, avgTagArea, rawFiducials
            )

        self.__botpose.set(botPoseArray(mt1Pose))
        self.__botposeBlue.set(botPoseArray(mt1Pose))
        self.__botposeRed.set(botPoseArray(mt1PoseRed))
        self.__botposeOrb.set(botPoseArray(mt2Pose))
        self.__botposeOrbBlue.set(botPoseArray(mt2Pose))
        self.__botposeOrbRed.set(botPoseArray(mt2PoseRed))

        primaryId = float(primary.id) if primary else -1.0
        primaryWidth = primary.apparentWidthPixels if primary else 0.0
        t2d = [
            1.0 if tagCount > 0 else 0.0,
            float(tagCount),
            totalLatencyMs,
            0.0,
            primary.tx if primary else 0.0,
            primary.ty if primary else 0.0,
            primary.tx if primary else 0.0,
            primary.ty if primary else 0.0,
            primary.ta if primary else 0.0,
            primaryId,
            -1.0,
            -1.0,
            primaryWidth,
            primaryWidth,
            primaryWidth,
            primaryWidth,
            0.0,
        ]
        self.__t2d.set(t2d)

        if primary is not None:
            tagPose = primary.pose
            self.__targetPoseRobotSpace.set(LimelightUtils.pose3dToArray(tagPose.relativeTo(robotPose)))
            self.__targetPoseCameraSpace.set(LimelightUtils.pose3dToArray(tagPose.relativeTo(cameraPose)))
            self.__cameraPoseTargetSpace.set(LimelightUtils.pose3dToArray(cameraPose.relativeTo(tagPose)))
            self.__botPoseTargetSpace.set(LimelightUtils.pose3dToArray(robotPose.relativeTo(tagPose)))
        else:
            self.__targetPoseRobotSpace.set([])
            self.__targetPoseCameraSpace.set([])
            self.__cameraPoseTargetSpace.set([])
            self.__botPoseTargetSpace.set([])

        cameraInRobotSpace = geometry.Pose3d(self.__robotToCamera.translation(), self.__robotToCamera.rotation())
        self.__cameraPoseRobotSpace.set(LimelightUtils.pose3dToArray(cameraInRobotSpace))

        self.__stddevs.set(
            [
                translationStdDev,
                translationStdDev,
                translationStdDev,
                rotationStdDev,
                rotationStdDev,
                rotationStdDev,
                translationStdDev / 2.0,
                translationStdDev / 2.0,
                translationStdDev / 2.0,
                0.0,
                0.0,
                0.0,
            ]
        )

        robotYawDeg = math.degrees(robotPose.rotation().Z())
        robotPitchDeg = math.degrees(robotPose.rotation().Y())
        robotRollDeg = math.degrees(robotPose.rotation().X())
        self.__imu.set([robotYawDeg, robotRollDeg, robotPitchDeg, robotYawDeg, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0])

    def __publishJson(
        self,
        robotPose: geometry.Pose3d,
        cameraPose: geometry.Pose3d,
        visible: List[_TagObservation],
        tagCount: int,
        tagSpan: float,
        avgTagDist: float,
        avgTagArea: float,
        tlMs: float,
        clMs: float,
        primary: Optional[_TagObservation],
        mt1Pose: geometry.Pose3d,
        mt1PoseRed: geometry.Pose3d,
        mt2Pose: geometry.Pose3d,
    ) -> None:
        nowSeconds = wpilib.Timer.getFPGATimestamp()

        fiducials = []
        for observation in visible:
            tagPose = observation.pose
            fiducials.append(
                {
                    "fID": float(observation.id),
                    "fam": "36h11",
                    "ta": observation.ta,
                    "tx": observation.tx,
                    "ty": observation.ty,
                    "txp": observation.tx * observation.pixelsPerDegree,
                    "typ": observation.ty * observation.pixelsPerDegree,
                    "tx_nocross": observation.tx,
                    "ty_nocross": observation.ty,
                    "ts": nowSeconds * 1000.0,
                    "t6c_ts": LimelightUtils.pose3dToArray(cameraPose.relativeTo(tagPose)),
                    "t6r_fs": LimelightUtils.pose3dToArray(robotPose),
                    "t6r_ts": LimelightUtils.pose3dToArray(robotPose.relativeTo(tagPose)),
                    "t6t_cs": LimelightUtils.pose3dToArray(tagPose.relativeTo(cameraPose)),
                    "t6t_rs": LimelightUtils.pose3dToArray(tagPose.relativeTo(robotPose)),
                }
            )

        cameraInRobotSpace = geometry.Pose3d(self.__robotToCamera.translation(), self.__robotToCamera.rotation())

        payload = {
            "error": "",
            "tx": primary.tx if primary else 0.0,
            "ty": primary.ty if primary else 0.0,
            "txnc": primary.tx if primary else 0.0,
            "tync": primary.ty if primary else 0.0,
            "ta": primary.ta if primary else 0.0,
            "pID": self.__pipelineIndex.getDouble(0),
            "pTYPE": self.__settings.pipelineType,
            "tl": tlMs,
            "cl": clMs,
            "ts": nowSeconds * 1000.0,
            "ts_rio": nowSeconds * 1000.0,
            "ts_nt": nowSeconds * 1_000_000.0,
            "ts_sys": time.time() * 1_000_000.0,
            "fidx": float(self.__frameIndex),
            "v": tagCount > 0,
            "botpose": LimelightUtils.pose3dToArray(mt1Pose),
            "botpose_wpiblue": LimelightUtils.pose3dToArray(mt1Pose),
            "botpose_wpired": LimelightUtils.pose3dToArray(mt1PoseRed),
            "botpose_orb": LimelightUtils.pose3dToArray(mt2Pose),
            "botpose_orb_wpiblue": LimelightUtils.pose3dToArray(mt2Pose),
            "botpose_tagcount": float(tagCount),
            "botpose_span": tagSpan,
            "botpose_avgdist": avgTagDist,
            "botpose_avgarea": avgTagArea,
            "t6c_rs": LimelightUtils.pose3dToArray(cameraInRobotSpace),
            "Retro": [],
            "Fiducial": fiducials,
            "Classifier": [],
            "Detector": [],
            "Barcode": [],
            "hw": None,
            "imu": None,
            "rewind": None,
            "imgsrc": None,
            "hwtype": "sim",
            "uirefesh": 0,
            "ignorent": 0,
            "tdist": primary.distToCamera if primary else 0.0,
            "PythonOut": [0.0] * 8,
            "stdev_mt1": [0.0] * 6,
            "stdev_mt2": [0.0] * 6,
        }

        try:
            self.__json.setString(json.dumps(payload))
        except Exception:
            self.__json.setString("")

    def __botPoseArray(
        self,
        pose: geometry.Pose3d,
        latencyMs: float,
        tagCount: int,
        tagSpan: float,
        avgTagDist: float,
        avgTagArea: float,
        rawFiducials: List[float],
    ) -> List[float]:
        """
        Build the flat "botpose*" NT array: [x,y,z,roll,pitch,yaw, latency, tagCount, tagSpan, avgDist, avgArea,
        (id,txnc,tync,ta,distToCamera,distToRobot,ambiguity)*tagCount].
        """
        if tagCount == 0:
            return []
        return (
            list(LimelightUtils.pose3dToArray(pose))
            + [latencyMs, float(tagCount), tagSpan, avgTagDist, avgTagArea]
            + list(rawFiducials)
        )

    def __flipToRed(self, bluePose: geometry.Pose3d) -> geometry.Pose3d:
        """Flip a blue alliance origin pose to the equivalent red alliance origin pose (180 degree rotation about the field center)."""
        rotation = bluePose.rotation()
        return geometry.Pose3d(
            self.__fieldLayout.getFieldLength() - bluePose.X(),
            self.__fieldLayout.getFieldWidth() - bluePose.Y(),
            bluePose.Z(),
            geometry.Rotation3d(rotation.X(), rotation.Y(), rotation.Z() + math.pi),
        )

    def __addNoise(
        self, pose: geometry.Pose3d, translationStdDev: float, rotationStdDevDegrees: float
    ) -> geometry.Pose3d:
        """Apply Gaussian noise to a pose's translation and yaw."""
        if translationStdDev <= 0 and rotationStdDevDegrees <= 0:
            return pose
        x = pose.X() + self.__rng.gauss(0, 1) * translationStdDev
        y = pose.Y() + self.__rng.gauss(0, 1) * translationStdDev
        z = pose.Z() + self.__rng.gauss(0, 1) * translationStdDev * 0.5
        rotation = pose.rotation()
        yaw = rotation.Z() + math.radians(self.__rng.gauss(0, 1) * rotationStdDevDegrees)
        return geometry.Pose3d(x, y, z, geometry.Rotation3d(rotation.X(), rotation.Y(), yaw))

    def __project(
        self, cameraPose: geometry.Pose3d, robotPose: geometry.Pose3d, tag: robotpy_apriltag.AprilTag
    ) -> Optional[_TagObservation]:
        """
        Project a field AprilTag into the simulated camera, returning an observation if it is within the
        configured field of view, range and incidence angle.
        """
        tagPose = tag.pose
        camToTag = geometry.Transform3d(cameraPose, tagPose)
        x = camToTag.X()
        y = camToTag.Y()
        z = camToTag.Z()

        if x <= 0.02:
            return None

        distance = camToTag.translation().norm()
        if distance > self.__settings.maxDetectionRangeMeters:
            return None

        halfHFovDeg = self.__settings.horizontalFOVDegrees / 2.0
        halfVFovDeg = self.__settings.verticalFOVDegrees / 2.0
        # tx positive = target right of crosshair, ty positive = target below crosshair (Limelight convention)
        yawDeg = math.degrees(math.atan2(-y, x))
        pitchDeg = math.degrees(math.atan2(-z, math.hypot(x, y)))
        if abs(yawDeg) > halfHFovDeg or abs(pitchDeg) > halfVFovDeg:
            return None

        tagNormal = geometry.Translation3d(1, 0, 0).rotateBy(tagPose.rotation())
        tagToCam = cameraPose.translation() - tagPose.translation()
        dot = tagNormal.X() * tagToCam.X() + tagNormal.Y() * tagToCam.Y() + tagNormal.Z() * tagToCam.Z()
        incidenceDeg = math.degrees(math.acos(_clamp(dot / tagToCam.norm(), -1.0, 1.0)))
        if incidenceDeg > self.__settings.maxTagIncidenceAngleDegrees:
            return None

        fx = (self.__settings.resolutionWidth / 2.0) / math.tan(math.radians(halfHFovDeg))
        apparentWidthPx = fx * self.__settings.tagSizeMeters * math.cos(math.radians(incidenceDeg)) / x
        areaPx = apparentWidthPx * apparentWidthPx
        ta = _clamp(areaPx / (self.__settings.resolutionWidth * self.__settings.resolutionHeight) * 100.0, 0.0, 100.0)

        noisyYaw = yawDeg + self.__rng.gauss(0, 1) * self.__settings.angleNoiseStdDevDegrees
        noisyPitch = pitchDeg + self.__rng.gauss(0, 1) * self.__settings.angleNoiseStdDevDegrees

        distToRobot = robotPose.translation().distance(tagPose.translation())
        ambiguity = _clamp(self.__rng.random() * 0.05 + (incidenceDeg / 90.0) * 0.15, 0.0, 1.0)
        pixelsPerDegree = (self.__settings.resolutionWidth / 2.0) / halfHFovDeg

        return _TagObservation(
            tag.ID, tagPose, ta, noisyYaw, noisyPitch, distance, distToRobot, ambiguity, apparentWidthPx,
            pixelsPerDegree,
        )
