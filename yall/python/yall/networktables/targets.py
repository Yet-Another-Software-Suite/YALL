from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional

from .util import LimelightUtils
from wpimath import geometry


@dataclass
class RetroreflectiveTape:
    """
    Represents a Color/Retroreflective Target Result extracted from JSON Output
    """

    ta: float = 0.0
    tx: float = 0.0
    ty: float = 0.0
    tx_pixels: float = 0.0
    ty_pixels: float = 0.0
    tx_nocrosshair: float = 0.0
    ty_nocrosshair: float = 0.0
    ts: float = 0.0

    cameraPose_TargetSpace: List[float] = field(default_factory=lambda: [0.0] * 6)
    robotPose_FieldSpace: List[float] = field(default_factory=lambda: [0.0] * 6)
    robotPose_TargetSpace: List[float] = field(default_factory=lambda: [0.0] * 6)
    targetPose_CameraSpace: List[float] = field(default_factory=lambda: [0.0] * 6)
    targetPose_RobotSpace: List[float] = field(default_factory=lambda: [0.0] * 6)

    @staticmethod
    def fromJson(data: Dict[str, Any]) -> "RetroreflectiveTape":
        return RetroreflectiveTape(
            ta=data.get("ta", 0.0),
            tx=data.get("tx", 0.0),
            ty=data.get("ty", 0.0),
            tx_pixels=data.get("txp", 0.0),
            ty_pixels=data.get("typ", 0.0),
            tx_nocrosshair=data.get("tx_nocross", 0.0),
            ty_nocrosshair=data.get("ty_nocross", 0.0),
            ts=data.get("ts", 0.0),
            cameraPose_TargetSpace=data.get("t6c_ts", [0.0] * 6),
            robotPose_FieldSpace=data.get("t6r_fs", [0.0] * 6),
            robotPose_TargetSpace=data.get("t6r_ts", [0.0] * 6),
            targetPose_CameraSpace=data.get("t6t_cs", [0.0] * 6),
            targetPose_RobotSpace=data.get("t6t_rs", [0.0] * 6),
        )

    def getCameraPose_TargetSpace(self) -> Optional[geometry.Pose3d]:
        return LimelightUtils.toPose3d(self.cameraPose_TargetSpace)

    def getRobotPose_FieldSpace(self) -> Optional[geometry.Pose3d]:
        return LimelightUtils.toPose3d(self.robotPose_FieldSpace)

    def getRobotPose_TargetSpace(self) -> Optional[geometry.Pose3d]:
        return LimelightUtils.toPose3d(self.robotPose_TargetSpace)

    def getTargetPose_CameraSpace(self) -> Optional[geometry.Pose3d]:
        return LimelightUtils.toPose3d(self.targetPose_CameraSpace)

    def getTargetPose_RobotSpace(self) -> Optional[geometry.Pose3d]:
        return LimelightUtils.toPose3d(self.targetPose_RobotSpace)

    def getCameraPose_TargetSpace2D(self) -> Optional[geometry.Pose2d]:
        return LimelightUtils.toPose2d(self.cameraPose_TargetSpace)

    def getRobotPose_FieldSpace2D(self) -> Optional[geometry.Pose2d]:
        return LimelightUtils.toPose2d(self.robotPose_FieldSpace)

    def getRobotPose_TargetSpace2D(self) -> Optional[geometry.Pose2d]:
        return LimelightUtils.toPose2d(self.robotPose_TargetSpace)

    def getTargetPose_CameraSpace2D(self) -> Optional[geometry.Pose2d]:
        return LimelightUtils.toPose2d(self.targetPose_CameraSpace)

    def getTargetPose_RobotSpace2D(self) -> Optional[geometry.Pose2d]:
        return LimelightUtils.toPose2d(self.targetPose_RobotSpace)


@dataclass
class Barcode:
    """
    Represents a decoded barcode and its metadata.
    """

    family: str = ""
    data: str = ""
    tx_pixels: float = 0.0
    ty_pixels: float = 0.0
    tx: float = 0.0
    ty: float = 0.0
    tx_nocrosshair: float = 0.0
    ty_nocrosshair: float = 0.0
    ta: float = 0.0
    corners: List[List[float]] = field(
        default_factory=lambda: [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]]
    )

    @staticmethod
    def fromJson(data: Dict[str, Any]) -> "Barcode":
        return Barcode(
            family=data.get("fam", ""),
            data=data.get("data", ""),
            tx_pixels=data.get("txp", 0.0),
            ty_pixels=data.get("typ", 0.0),
            tx=data.get("tx", 0.0),
            ty=data.get("ty", 0.0),
            tx_nocrosshair=data.get("tx_nocross", 0.0),
            ty_nocrosshair=data.get("ty_nocross", 0.0),
            ta=data.get("ta", 0.0),
            corners=data.get("pts", [[0.0, 0.0]] * 4),
        )

    def getFamily(self) -> str:
        return self.family


@dataclass
class AprilTagFiducial:
    """
    Represents an AprilTag/Fiducial Target Result extracted from JSON Output
    """

    fiducialID: float = 0.0
    fiducialFamily: str = ""
    ta: float = 0.0
    tx: float = 0.0
    ty: float = 0.0
    tx_pixels: float = 0.0
    ty_pixels: float = 0.0
    tx_nocrosshair: float = 0.0
    ty_nocrosshair: float = 0.0
    ts: float = 0.0
    cameraPose_TargetSpace: List[float] = field(default_factory=lambda: [0.0] * 6)
    robotPose_FieldSpace: List[float] = field(default_factory=lambda: [0.0] * 6)
    robotPose_TargetSpace: List[float] = field(default_factory=lambda: [0.0] * 6)
    targetPose_CameraSpace: List[float] = field(default_factory=lambda: [0.0] * 6)
    targetPose_RobotSpace: List[float] = field(default_factory=lambda: [0.0] * 6)

    @staticmethod
    def fromJson(data: Dict[str, Any]) -> "AprilTagFiducial":
        return AprilTagFiducial(
            fiducialID=data.get("fID", 0.0),
            fiducialFamily=data.get("fam", ""),
            ta=data.get("ta", 0.0),
            tx=data.get("tx", 0.0),
            ty=data.get("ty", 0.0),
            tx_pixels=data.get("txp", 0.0),
            ty_pixels=data.get("typ", 0.0),
            tx_nocrosshair=data.get("tx_nocross", 0.0),
            ty_nocrosshair=data.get("ty_nocross", 0.0),
            ts=data.get("ts", 0.0),
            cameraPose_TargetSpace=data.get("t6c_ts", [0.0] * 6),
            robotPose_FieldSpace=data.get("t6r_fs", [0.0] * 6),
            robotPose_TargetSpace=data.get("t6r_ts", [0.0] * 6),
            targetPose_CameraSpace=data.get("t6t_cs", [0.0] * 6),
            targetPose_RobotSpace=data.get("t6t_rs", [0.0] * 6),
        )

    def getCameraPose_TargetSpace(self) -> Optional[geometry.Pose3d]:
        return LimelightUtils.toPose3d(self.cameraPose_TargetSpace)

    def getRobotPose_FieldSpace(self) -> Optional[geometry.Pose3d]:
        return LimelightUtils.toPose3d(self.robotPose_FieldSpace)

    def getRobotPose_TargetSpace(self) -> Optional[geometry.Pose3d]:
        return LimelightUtils.toPose3d(self.robotPose_TargetSpace)

    def getTargetPose_CameraSpace(self) -> Optional[geometry.Pose3d]:
        return LimelightUtils.toPose3d(self.targetPose_CameraSpace)

    def getTargetPose_RobotSpace(self) -> Optional[geometry.Pose3d]:
        return LimelightUtils.toPose3d(self.targetPose_RobotSpace)

    def getCameraPose_TargetSpace2D(self) -> Optional[geometry.Pose2d]:
        return LimelightUtils.toPose2d(self.cameraPose_TargetSpace)

    def getRobotPose_FieldSpace2D(self) -> Optional[geometry.Pose2d]:
        return LimelightUtils.toPose2d(self.robotPose_FieldSpace)

    def getRobotPose_TargetSpace2D(self) -> Optional[geometry.Pose2d]:
        return LimelightUtils.toPose2d(self.robotPose_TargetSpace)

    def getTargetPose_CameraSpace2D(self) -> Optional[geometry.Pose2d]:
        return LimelightUtils.toPose2d(self.targetPose_CameraSpace)

    def getTargetPose_RobotSpace2D(self) -> Optional[geometry.Pose2d]:
        return LimelightUtils.toPose2d(self.targetPose_RobotSpace)


@dataclass
class NeuralClassifier:
    """
    Represents a Neural Classifier Pipeline Result extracted from JSON Output
    """

    # Neural pipeline class name from Limelight
    className: str = ""

    # Neural pipeline class id from the Limelight
    classID: float = 0.0

    # Confidence that the object is the class
    confidence: float = 0.0

    zone: float = 0.0

    # X position of the object in the image as percent
    tx: float = 0.0

    # X position of the object in the image as pixel
    tx_pixels: float = 0.0

    # Y position of the object in the image as percent
    ty: float = 0.0

    # Y position of the object in the image as pixel
    ty_pixels: float = 0.0

    @staticmethod
    def fromJson(data: Dict[str, Any]) -> "NeuralClassifier":
        return NeuralClassifier(
            className=data.get("class", ""),
            classID=data.get("classID", 0.0),
            confidence=data.get("conf", 0.0),
            zone=data.get("zone", 0.0),
            tx=data.get("tx", 0.0),
            tx_pixels=data.get("txp", 0.0),
            ty=data.get("ty", 0.0),
            ty_pixels=data.get("typ", 0.0),
        )


@dataclass
class NeuralDetector:
    """
    Represents a Neural Detector Pipeline Result extracted from JSON Output
    """

    className: str = ""
    classID: float = 0.0
    confidence: float = 0.0
    ta: float = 0.0
    tx: float = 0.0
    ty: float = 0.0
    tx_pixels: float = 0.0
    ty_pixels: float = 0.0
    tx_nocrosshair: float = 0.0
    ty_nocrosshair: float = 0.0

    @staticmethod
    def fromJson(data: Dict[str, Any]) -> "NeuralDetector":
        return NeuralDetector(
            className=data.get("class", ""),
            classID=data.get("classID", 0.0),
            confidence=data.get("conf", 0.0),
            ta=data.get("ta", 0.0),
            tx=data.get("tx", 0.0),
            ty=data.get("ty", 0.0),
            tx_pixels=data.get("txp", 0.0),
            ty_pixels=data.get("typ", 0.0),
            tx_nocrosshair=data.get("tx_nocross", 0.0),
            ty_nocrosshair=data.get("ty_nocross", 0.0),
        )
