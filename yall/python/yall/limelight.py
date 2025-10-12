import json
from enum import Enum
from typing import Final, List, Optional

import ntcore

from yall.networktables.geometry import Orientation3d
from . import results
from wpilib import Alert, DriverStation, RobotBase
from wpimath import geometry

from .deprecated import deprecated
from .networktables import targets
from .networktables.util import LimelightUtils


class PoseEstimate:
    def __init__(self, camera: "Limelight", entryName: str, megaTag2: bool):
        self.pose: geometry.Pose3d = geometry.Pose3d()
        self.timestampSeconds: float = 0
        self.latency: float = 0
        self.tagCount: int = 0
        self.tagSpan: float = 0
        self.avgTagDist: float = 0
        self.avgTagArea: float = 0
        self.rawFiducials: list[results.RawFiducial]
        self.isMegaTag2: Final[bool] = megaTag2
        self.__poseEntryName: Final[str] = entryName
        self.__poseEntry: ntcore.DoubleArrayEntry = (
            camera.getNTTable().getDoubleArrayTopic(self.__poseEntryName).getEntry([])
        )
        self.hasData: bool = False

    def getPoseEstimate(self) -> Optional["PoseEstimate"]:
        tsValue: ntcore.TimestampedDoubleArray = self.__poseEntry.getAtomic()
        poseArray: list[float] = tsValue.value
        timestamp: int = tsValue.time

        if len(poseArray) == 0:
            self.hasData = False
            return None

        pose: Optional[geometry.Pose3d] = LimelightUtils.toPose3d(poseArray)
        latency: float = LimelightUtils.extractArrayEntry(poseArray, 6)
        tagCount: int = int(LimelightUtils.extractArrayEntry(poseArray, 7))
        tagSpan: float = LimelightUtils.extractArrayEntry(poseArray, 8)
        tagDist: float = LimelightUtils.extractArrayEntry(poseArray, 9)
        tagArea: float = LimelightUtils.extractArrayEntry(poseArray, 10)

        adjustedTimestamp = (timestamp / 1_000_000.0) - (latency / 1_000.0)

        rawFiducials: list[results.RawFiducial] = [results.RawFiducial()] * tagCount
        valsPerFiducial: int = 7
        expectedTotalVals = 11 + valsPerFiducial * tagCount

        if len(poseArray) != expectedTotalVals:
            # Don't populate fiducials
            ...
        else:
            for i in range(tagCount):
                baseIndex = 11 + (i * valsPerFiducial)
                id: int = int(poseArray[baseIndex])
                txnc: float = poseArray[baseIndex + 1]
                tync: float = poseArray[baseIndex + 2]
                ta: float = poseArray[baseIndex + 3]
                distToCamera: float = poseArray[baseIndex + 4]
                distToRobot: float = poseArray[baseIndex + 5]
                ambiguity: float = poseArray[baseIndex + 6]
                rawFiducials[i] = results.RawFiducial(
                    id=id,
                    txnc=txnc,
                    tync=tync,
                    ta=ta,
                    distToCamera=distToCamera,
                    distToRobot=distToRobot,
                    ambiguity=ambiguity,
                )
        if pose:
            self.pose = pose
            self.timestampSeconds = adjustedTimestamp
            self.latency = latency
            self.tagCount = tagCount
            self.tagSpan = tagSpan
            self.avgTagDist = tagDist
            self.avgTagArea = tagArea
            self.rawFiducials = rawFiducials
            self.hasData = len(rawFiducials) > 0

            return self
        return None

    def getMinTagAmbiguity(self) -> float:
        if not self.hasData:
            return 1
        minTagAmbiguity: float = float("inf")
        minTagAmbiguity = min(
            map(lambda fiducial: fiducial.ambiguity, self.rawFiducials)
        )
        return minTagAmbiguity

    def getMaxTagAmbiguity(self) -> float:
        if not self.hasData:
            return 1
        maxTagAmbiguity: float = float("inf")
        maxTagAmbiguity = max(
            map(lambda fiducial: fiducial.ambiguity, self.rawFiducials)
        )
        return maxTagAmbiguity

    def getAvgTagAmbiguity(self) -> float:
        if not self.hasData:
            return 1
        ambiguitySum = sum(map(lambda fiducial: fiducial.ambiguity, self.rawFiducials))
        return ambiguitySum / len(self.rawFiducials)

    def __str__(self) -> str:
        if not self.hasData:
            return "No PoseEstimate available."

        result = (
            "Pose Estimate Information:\n"
            + f"Timestamp (Seconds): {self.timestampSeconds:.3f}\n"
            + f"Latency: {self.latency:.3f} ms\n"
            + f"Tag Count: {self.tagCount}\n"
            + f"Average Tag Distance: {self.avgTagDist:.2f} meters\n"
            + f"Average Tag Area: {self.avgTagArea:.2f}% of image\n"
            + f"Is MegaTag2: {self.isMegaTag2}\n\n"
        )

        if self.rawFiducials is None or len(self.rawFiducials) == 0:
            result += "No RawFiducials data available.\n"
            return result

        result += "Raw Fiducials Details:\n"

        for i in range(len(self.rawFiducials)):
            fiducial: results.RawFiducial = self.rawFiducials[i]
            result += f"Fiducial: {i + 1}\n"
            result += str(fiducial)

        return result


class BotPose(Enum):
    """
    BotPose enum for easier decoding.
    """

    # (Not Recommended) The robot's pose in the WPILib Red Alliance Coordinate System.
    RED = ("botpose_wpired", False)

    # (Not Recommended) The robot's pose in the WPILib Red Alliance Coordinate System with MegaTag2.
    RED_MEGATAG2 = ("botpose_orb_wpired", True)

    # (Recommended) The robot's 3D pose in the WPILib Blue Alliance Coordinate System.
    BLUE = ("botpose_wpiblue", False)

    # (Recommended) The robot's 3D pose in the WPILib Blue Alliance Coordinate System with MegaTag2.
    BLUE_MEGATAG2 = ("botpose_orb_wpiblue", True)

    def __init__(self, entryName: str, megatag2: bool):
        """
        Create BotPose enum with given entry names and MegaTag2 state.

        Args:
            entryName (str): Bot Pose entry name for Limelight.
            megatag2 (bool): MegaTag2 reading state.
        """
        self.__entry: Final[str] = entryName
        self.__isMegaTag2: Final[bool] = megatag2
        self.__poseEstimate: Optional["PoseEstimate"] = None

    def get(self, camera: "Limelight") -> Optional["PoseEstimate"]:
        """
        Fetch the PoseEstimate if it exists.

        Args:
            camera (Limelight): The Limelight camera to use.

        Returns:
            Optional[PoseEstimate]: The current PoseEstimate, if available.
        """
        if not self.__poseEstimate:
            estimate: PoseEstimate = PoseEstimate(
                camera, self.__entry, self.__isMegaTag2
            )
            self.__poseEstimate = estimate

        return self.__poseEstimate.getPoseEstimate()


class LimelightTargetData:
    """
    Data class for holding Limelight target information from the NetworkTables.
    """

    def __init__(self, camera: "Limelight"):
        """
        Initialize target data for the given Limelight camera.

        :param camera: The Limelight camera instance to fetch data from.
        """
        self.__limelight: Limelight = camera
        self.__limelightTable: ntcore.NetworkTable = self.__limelight.getNTTable()

        self.__targetValid: ntcore.NetworkTableEntry = self.__limelightTable.getEntry(
            "tv"
        )
        self.__targetColor: ntcore.DoubleArrayEntry = (
            self.__limelightTable.getDoubleArrayTopic("tc").getEntry([0.0])
        )
        self.__fiducialID: ntcore.NetworkTableEntry = self.__limelightTable.getEntry(
            "tid"
        )
        self.__neuralClassID: ntcore.NetworkTableEntry = self.__limelightTable.getEntry(
            "tclass"
        )
        self.__horizontalOffset: ntcore.NetworkTableEntry = (
            self.__limelightTable.getEntry("tx")
        )
        self.__verticalOffset: ntcore.NetworkTableEntry = (
            self.__limelightTable.getEntry("ty")
        )
        self.__horizontalOffsetFromPrincipal: ntcore.NetworkTableEntry = (
            self.__limelightTable.getEntry("txnc")
        )
        self.__verticalOffsetFromPrincipal: ntcore.NetworkTableEntry = (
            self.__limelightTable.getEntry("tync")
        )
        self.__targetArea: ntcore.NetworkTableEntry = self.__limelightTable.getEntry(
            "ta"
        )
        self.__targetMetrics: ntcore.DoubleArrayEntry = (
            self.__limelightTable.getDoubleArrayTopic("t2d").getEntry([0.0])
        )
        self.__target2RobotPose: ntcore.DoubleArrayEntry = (
            self.__limelightTable.getDoubleArrayTopic("targetpose_robotspace").getEntry(
                [0.0]
            )
        )
        self.__target2CameraPose: ntcore.DoubleArrayEntry = (
            self.__limelightTable.getDoubleArrayTopic(
                "targetpose_cameraspace"
            ).getEntry([0.0])
        )
        self.__camera2TargetPose: ntcore.DoubleArrayEntry = (
            self.__limelightTable.getDoubleArrayTopic(
                "camerapose_targetspace"
            ).getEntry([0.0])
        )
        self.__robot2TargetPose: ntcore.DoubleArrayEntry = (
            self.__limelightTable.getDoubleArrayTopic("botpose_targetspace").getEntry(
                [0.0]
            )
        )

    def getAprilTagID(self) -> float:
        """
        Get the current AprilTag ID that is being targeted.

        :return: The AprilTag ID.
        """
        return self.__fiducialID.getDouble(0.0)

    def getNeuralClassID(self) -> str:
        """
        Get the neural class name of the current target.

        :return: Neural class name.
        """
        return self.__neuralClassID.getString("")

    def getTargetColor(self) -> list:
        """
        Get the target color in HSV/RGB format.

        :return: The target color in HSV/RGB format.
        """
        return self.__targetColor.get()

    def getRobotToTarget(self) -> Optional[geometry.Pose3d]:
        """
        Get the robot's 3D pose with respect to the current target.

        :return: Pose3d object representing the robot's position and orientation relative to the target.
        """
        return LimelightUtils.toPose3d(self.__robot2TargetPose.get())

    def getCameraToTarget(self) -> Optional[geometry.Pose3d]:
        """
        Get the camera's 3D pose with respect to the current target.

        :return: Pose3d object representing the camera's position and orientation relative to the target.
        """
        return LimelightUtils.toPose3d(self.__camera2TargetPose.get())

    def getTargetToCamera(self) -> Optional[geometry.Pose3d]:
        """
        Get the target's 3D pose with respect to the camera's coordinate system.

        :return: Pose3d object representing the target's position and orientation relative to the camera.
        """
        return LimelightUtils.toPose3d(self.__target2CameraPose.get())

    def getTargetToRobot(self) -> Optional[geometry.Pose3d]:
        """
        Get the target's 3D pose with respect to the robot's coordinate system.

        :return: Pose3d object representing the target's position and orientation relative to the robot.
        """
        return LimelightUtils.toPose3d(self.__target2RobotPose.get())

    def getTargetStatus(self) -> bool:
        """
        Check if the Limelight has a valid target.

        :return: True if a valid target is present, otherwise False.
        """
        return self.__targetValid.getDouble(0) == 1.0

    def getHorizontalOffset(self) -> float:
        """
        Get the horizontal offset from the crosshair to the target in degrees.

        :return: Horizontal offset angle in degrees.
        """
        return self.__horizontalOffset.getDouble(0)

    def getVerticalOffset(self) -> float:
        """
        Get the vertical offset from the crosshair to the target in degrees.

        :return: Vertical offset angle in degrees.
        """
        return self.__verticalOffset.getDouble(0)

    def getHorizontalOffsetFromPrincipal(self) -> float:
        """
        Get the horizontal offset from the principal pixel/point to the target in degrees.
        This is the most accurate 2D metric if you are using a calibrated camera and don't need adjustable crosshairs.

        :return: Horizontal offset angle in degrees.
        """
        return self.__horizontalOffsetFromPrincipal.getDouble(0)

    def getVerticalOffsetFromPrincipal(self) -> float:
        """
        Get the vertical offset from the principal pixel/point to the target in degrees.
        This is the most accurate 2D metric if you are using a calibrated camera and don't need adjustable crosshairs.

        :return: Vertical offset angle in degrees.
        """
        return self.__verticalOffsetFromPrincipal.getDouble(0)

    def getTargetArea(self) -> float:
        """
        Get the target area as a percentage of the image (0-100%).

        :return: Target area percentage (0-100).
        """
        return self.__targetArea.getDouble(0)

    def getTargetMetrics(self) -> list:
        """
        Get the target metrics array.

        :return: An array containing various target metrics such as validity, count, latency, offsets, etc.
        """
        return self.__targetMetrics.get()

    def getTargetCount(self) -> int:
        """
        Get the number of targets currently detected.

        :return: Number of detected targets.
        """
        t2d = self.getTargetMetrics()
        if len(t2d) == 17:
            return int(t2d[1])
        return 0

    def getClassifierClassIndex(self) -> int:
        """
        Get the classifier class index from the neural classifier pipeline.

        :return: Class index from the classifier pipeline.
        """
        t2d = self.getTargetMetrics()
        if len(t2d) == 17:
            return int(t2d[10])
        return 0

    def getDetectorClassIndex(self) -> int:
        """
        Get the detector class index from the neural detector pipeline.

        :return: Class index from the detector pipeline.
        """
        t2d = self.getTargetMetrics()
        if len(t2d) == 17:
            return int(t2d[11])
        return 0


class LimelightPipelineData:
    """
    Pipeline data for the Limelight camera.

    Attributes:
        __limelight (Limelight): The Limelight camera instance to fetch data from.
        __limelightTable (ntcore.NetworkTable): The network table associated with the Limelight camera.
        __processingLatency (ntcore.NetworkTableEntry): The entry for pipeline processing latency.
        __captureLatency (ntcore.NetworkTableEntry): The entry for pipeline capture latency.
        __pipelineIndex (ntcore.NetworkTableEntry): The entry for the current pipeline index.
        __pipelineType (ntcore.NetworkTableEntry): The entry for the current pipeline type.
    """

    def __init__(self, camera: "Limelight") -> None:
        """
        Constructs the LimelightPipelineData object.

        Args:
            camera (Limelight): The Limelight camera instance to use.
        """
        self.__limelight: Limelight = camera
        self.__limelightTable: ntcore.NetworkTable = self.__limelight.getNTTable()
        self.__processingLatency: ntcore.NetworkTableEntry = (
            self.__limelightTable.getEntry("tl")
        )
        self.__captureLatency: ntcore.NetworkTableEntry = (
            self.__limelightTable.getEntry("cl")
        )
        self.__pipelineIndex: ntcore.NetworkTableEntry = self.__limelightTable.getEntry(
            "getpipe"
        )
        self.__pipelineType: ntcore.NetworkTableEntry = self.__limelightTable.getEntry(
            "getpipetype"
        )

    def getProcessingLatency(self) -> float:
        """
        Gets the pipeline's processing latency contribution.

        Returns:
            float: The pipeline's processing latency in milliseconds.
        """
        return self.__processingLatency.getDouble(0.0)

    def getCaptureLatency(self) -> float:
        """
        Gets the capture latency.

        Returns:
            float: The capture latency in milliseconds.
        """
        return self.__captureLatency.getDouble(0.0)

    def getCurrentPipelineIndex(self) -> float:
        """
        Gets the active pipeline index.

        Returns:
            float: The current pipeline index (0-9).
        """
        return self.__pipelineIndex.getDouble(0)

    def getCurrentPipelineType(self) -> str:
        """
        Gets the current pipeline type.

        Returns:
            str: The pipeline type string (e.g. "retro", "apriltag", etc.).
        """
        return self.__pipelineType.getString("")


class LimelightData:
    """
    Data retrieval class for Limelight.
    """

    def __init__(self, camera: "Limelight") -> None:
        """
        Construct the LimelightData class to retrieve read-only data.

        :param camera: Limelight to use.
        """
        self.limelight: Limelight = camera
        self.limelightTable: ntcore.NetworkTable = self.limelight.getNTTable()
        self.results: ntcore.NetworkTableEntry = self.limelightTable.getEntry("json")
        self.rawfiducials: ntcore.NetworkTableEntry = self.limelightTable.getEntry(
            "rawfiducials"
        )
        self.rawDetections: ntcore.NetworkTableEntry = self.limelightTable.getEntry(
            "rawdetections"
        )
        self.classifierClass: ntcore.NetworkTableEntry = self.limelightTable.getEntry(
            "tcclass"
        )
        self.detectorClass: ntcore.NetworkTableEntry = self.limelightTable.getEntry(
            "tdclass"
        )
        self.camera2RobotPose3d: ntcore.DoubleArrayEntry = (
            self.limelightTable.getDoubleArrayTopic("camerapose_robotspace").getEntry(
                []
            )
        )
        self.barcodeData: ntcore.StringArrayEntry = (
            self.limelightTable.getStringArrayTopic("rawbarcodes").getEntry([])
        )
        self.pythonScriptData: ntcore.DoubleArrayEntry = (
            self.limelightTable.getDoubleArrayTopic("llpython").getEntry([])
        )
        self.pythonScriptDataSet: ntcore.DoubleArrayEntry = (
            self.limelightTable.getDoubleArrayTopic("llrobot").getEntry([])
        )
        self.targetData: LimelightTargetData = LimelightTargetData(camera)
        self.pipelineData: LimelightPipelineData = LimelightPipelineData(camera)

    def getPythonData(self) -> list[float]:
        """
        Get the output of the custom python script running on the Limelight.

        :return: Output Double Array of the custom python script running on the Limelight.
        """
        return self.pythonScriptData.get()

    def setPythonData(self, outgoingData: list[float]) -> None:
        """
        Set the input for the custom python script running on the Limelight.

        :param outgoingData: Double array for custom python script.
        """
        self.pythonScriptDataSet.set(outgoingData)

    def getBarcodeData(self) -> list[str]:
        """
        Barcode data read by the Limelight.

        :return: Barcode data as a string.
        """
        return self.barcodeData.get()

    def getCamera2Robot(self) -> Optional[geometry.Pose3d]:
        """
        Gets the camera's 3D pose with respect to the robot's coordinate system.

        :return: Pose3d object representing the camera's position and orientation relative to the robot.
        """
        return LimelightUtils.toPose3d(self.camera2RobotPose3d.get())

    def getClassifierClass(self) -> str:
        """
        Gets the current neural classifier result class name.

        :return: Class name string from classifier pipeline.
        """
        return self.classifierClass.getString("")

    def getDetectorClass(self) -> str:
        """
        Gets the primary neural detector result class name.

        :return: Class name string from detector pipeline.
        """
        return self.detectorClass.getString("")

    def getResults(self) -> Optional["LimelightResults"]:
        """
        Get LimelightResults from NetworkTables.
        Exists only if LL GUI option "Output & Crosshair - Send JSON over NT?" is Yes.

        :return: LimelightResults if it exists.
        """
        try:
            json_result = self.results.getString("")
            if len(json_result) <= 0:
                return None
            limelight_results: LimelightResults = json.loads(
                json_result, object_hook=LimelightResults.fromJson
            )
            return limelight_results
        except Exception as e:
            print(f"lljson error: {e}")
            # DriverStation.reportError(f"lljson error: {e}", True)
        return None

    def getRawFiducials(self) -> list[results.RawFiducial]:
        """
        Gets the latest raw fiducial/AprilTag detection results from NetworkTables.

        :return: Array of RawFiducial objects containing detection details.
        """
        rawFiducialArray: list[float] = self.rawfiducials.getDoubleArray([])
        valsPerEntry = 7
        if len(rawFiducialArray) % valsPerEntry != 0:
            return []

        numFiducials = len(rawFiducialArray) // valsPerEntry
        rawFiducials = []

        for i in range(numFiducials):
            baseIndex = i * valsPerEntry
            id = int(LimelightUtils.extractArrayEntry(rawFiducialArray, baseIndex))
            txnc = LimelightUtils.extractArrayEntry(rawFiducialArray, baseIndex + 1)
            tync = LimelightUtils.extractArrayEntry(rawFiducialArray, baseIndex + 2)
            ta = LimelightUtils.extractArrayEntry(rawFiducialArray, baseIndex + 3)
            distToCamera = LimelightUtils.extractArrayEntry(
                rawFiducialArray, baseIndex + 4
            )
            distToRobot = LimelightUtils.extractArrayEntry(
                rawFiducialArray, baseIndex + 5
            )
            ambiguity = LimelightUtils.extractArrayEntry(
                rawFiducialArray, baseIndex + 6
            )

            rawFiducials.append(
                results.RawFiducial(
                    id, txnc, tync, ta, distToCamera, distToRobot, ambiguity
                )
            )

        return rawFiducials

    def getRawDetections(self) -> list[results.RawDetection]:
        """
        Gets the latest raw neural detector results from NetworkTables.

        :return: Array of RawDetection objects containing detection details.
        """
        rawDetectionArray: list[float] = self.rawDetections.getDoubleArray([])
        valsPerEntry = 12
        if len(rawDetectionArray) % valsPerEntry != 0:
            return []

        numDetections = len(rawDetectionArray) // valsPerEntry
        rawDetections = []

        for i in range(numDetections):
            baseIndex = i * valsPerEntry
            classId = int(
                LimelightUtils.extractArrayEntry(rawDetectionArray, baseIndex)
            )
            txnc = LimelightUtils.extractArrayEntry(rawDetectionArray, baseIndex + 1)
            tync = LimelightUtils.extractArrayEntry(rawDetectionArray, baseIndex + 2)
            ta = LimelightUtils.extractArrayEntry(rawDetectionArray, baseIndex + 3)
            corner0_X = LimelightUtils.extractArrayEntry(
                rawDetectionArray, baseIndex + 4
            )
            corner0_Y = LimelightUtils.extractArrayEntry(
                rawDetectionArray, baseIndex + 5
            )
            corner1_X = LimelightUtils.extractArrayEntry(
                rawDetectionArray, baseIndex + 6
            )
            corner1_Y = LimelightUtils.extractArrayEntry(
                rawDetectionArray, baseIndex + 7
            )
            corner2_X = LimelightUtils.extractArrayEntry(
                rawDetectionArray, baseIndex + 8
            )
            corner2_Y = LimelightUtils.extractArrayEntry(
                rawDetectionArray, baseIndex + 9
            )
            corner3_X = LimelightUtils.extractArrayEntry(
                rawDetectionArray, baseIndex + 10
            )
            corner3_Y = LimelightUtils.extractArrayEntry(
                rawDetectionArray, baseIndex + 11
            )

            rawDetections.append(
                results.RawDetection(
                    classId,
                    txnc,
                    tync,
                    ta,
                    corner0_X,
                    corner0_Y,
                    corner1_X,
                    corner1_Y,
                    corner2_X,
                    corner2_Y,
                    corner3_X,
                    corner3_Y,
                )
            )

        return rawDetections


class LimelightSettings:
    """
    Settings class to apply configurable options to the Limelight.

    These settings are sent from the roboRIO back to the Limelight to affect the LL.
    One or more chains of ".withXXXX" methods can change the LL settings.
    The action of each ".withXXXX" method is essentially immediate.
    """

    class LEDMode(Enum):
        PipelineControl = 0
        ForceOff = 1
        ForceBlink = 2
        ForceOn = 3

    class ImuMode(Enum):
        ExternalImu = 0  # Use external IMU yaw submitted via {@link withRobotOrientation} for MT2 localization. The internal IMU is ignored entirely.
        SyncInternalImu = 1  # Use external IMU yaw submitted via {@link withRobotOrientation} for MT2 localization. The internal IMU is synced with the external IMU.
        InternalImu = 2  # Use internal IMU for MT2 localization. Ignores external IMU updates from {@link withRobotOrientation}.

    class DownscalingOverride(Enum):
        Pipeline = 0  # Pipeline downscaling, equivalent to 0
        NoDownscale = 1  # No downscaling, equivalent to 1
        HalfDownscale = 1.5  # Half downscaling, equivalent to 1.5
        DoubleDownscale = 2  # Double downscaling, equivalent to 2
        TripleDownscale = 3  # Triple downscaling, equivalent to 3
        QuadrupleDownscale = 4  # Quadruple downscaling, equivalent to 4

    class StreamMode(Enum):
        Standard = 0  # Side by side.
        PictureInPictureMain = 1  # Picture in picture, with secondary in corner
        PictureInPictureSecondary = 2  # Picture in picture, with main in corner

    def __init__(self, camera: "Limelight"):
        """
        Create a LimelightSettings object with all configurable features of a Limelight.
        :param camera: Limelight instance to use.
        """
        self.limelight: Limelight = camera
        self.limelightTable: ntcore.NetworkTable = camera.getNTTable()

        self.ledMode: ntcore.NetworkTableEntry = self.limelightTable.getEntry("ledMode")
        self.pipelineIndex: ntcore.NetworkTableEntry = self.limelightTable.getEntry(
            "pipeline"
        )
        self.priorityTagID: ntcore.NetworkTableEntry = self.limelightTable.getEntry(
            "priorityid"
        )
        self.streamMode: ntcore.NetworkTableEntry = self.limelightTable.getEntry(
            "stream"
        )
        self.cropWindow: ntcore.DoubleArrayEntry = (
            self.limelightTable.getDoubleArrayTopic("crop").getEntry([])
        )
        self.imuMode: ntcore.NetworkTableEntry = self.limelightTable.getEntry(
            "imumode_set"
        )
        self.robotOrientationSet: ntcore.DoubleArrayEntry = (
            self.limelightTable.getDoubleArrayTopic("robot_orientation_set").getEntry(
                []
            )
        )
        self.downscale: ntcore.NetworkTableEntry = self.limelightTable.getEntry(
            "fiducial_downscale_set"
        )
        self.fiducial3DOffset: ntcore.DoubleArrayEntry = (
            self.limelightTable.getDoubleArrayTopic("fiducial_offset_set").getEntry([])
        )
        self.cameraToRobot: ntcore.DoubleArrayEntry = (
            self.limelightTable.getDoubleArrayTopic(
                "camerapose_robotspace_set"
            ).getEntry([])
        )
        self.fiducialIDFiltersOverride: ntcore.DoubleArrayEntry = (
            self.limelightTable.getDoubleArrayTopic("fiducial_id_filters_set").getEntry(
                []
            )
        )

    def withLimelightLEDMode(self, mode: LEDMode) -> "LimelightSettings":
        """Set the Limelight LED mode."""
        self.ledMode.setDouble(mode.value)
        return self

    def withPipelineIndex(self, index: int) -> "LimelightSettings":
        """Set the current pipeline index for the Limelight."""
        self.pipelineIndex.setDouble(index)
        return self

    def withPriorityTagId(self, aprilTagId: int) -> "LimelightSettings":
        """Set the Priority Tag ID for the Limelight."""
        self.priorityTagID.setDouble(aprilTagId)
        return self

    def withStreamMode(self, mode: StreamMode) -> "LimelightSettings":
        """Set the Stream mode based on the StreamMode enum."""
        self.streamMode.setDouble(mode.value)
        return self

    def withCropWindow(
        self, minX: float, maxX: float, minY: float, maxY: float
    ) -> "LimelightSettings":
        """
        Set the crop window for the camera. Values must be between -1 and 1.
        """
        self.cropWindow.set([minX, maxX, minY, maxY])
        return self

    def withImuMode(self, mode: ImuMode) -> "LimelightSettings":
        """Set the IMU Mode for the Limelight."""
        self.imuMode.setDouble(mode.value)
        return self

    def withRobotOrientation(self, orientation: Orientation3d) -> "LimelightSettings":
        """Set the robot orientation used for localization."""
        self.robotOrientationSet.set(LimelightUtils.orientation3dToArray(orientation))
        return self

    def withFiducialDownscalingOverride(
        self, downscalingOverride: DownscalingOverride
    ) -> "LimelightSettings":
        """
        Set the downscaling factor for AprilTag detection.
        Valid values are 0 (pipeline control), 1 (no downscale), 2, 3, 4.
        """
        self.downscale.setDouble(downscalingOverride.value)
        return self

    def withAprilTagOffset(self, offset: geometry.Translation3d) -> "LimelightSettings":
        """
        Set the offset from the AprilTag that is of interest.
        See: https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltag-3d#point-of-interest-tracking
        """
        self.fiducial3DOffset.set(LimelightUtils.translation3dToArray(offset))
        return self

    def withArilTagIdFilter(self, idFilter: List[float]) -> "LimelightSettings":
        """Set the Limelight AprilTag ID filter/override."""
        self.fiducialIDFiltersOverride.set(idFilter)
        return self

    def withCameraToRobot(self, pose: geometry.Pose3d) -> "LimelightSettings":
        """Set the camera pose relative to the robot."""
        self.cameraToRobot.set(LimelightUtils.pose3dToArray(pose))
        return self


class LimelightResults:
    def __init__(self) -> None:
        self.error: Optional[str] = None
        self.pipelineID: float = 0.0
        self.latency_pipeline: float = 0.0
        self.latency_capture: float = 0.0
        self.latency_jsonParse: float = 0.0
        self.timestamp_LIMELIGHT_publish: float = 0.0
        self.timestamp_RIOFPGA_capture: float = 0.0
        self.valid: bool = False
        self.botpose: List[float] = [0.0] * 6
        self.botpose_wpired: List[float] = [0.0] * 6
        self.botpose_wpiblue: List[float] = [0.0] * 6
        self.botpose_tagcount: float = 0.0
        self.botpose_span: float = 0.0
        self.botpose_avgdist: float = 0.0
        self.botpose_avgarea: float = 0.0
        self.camerapose_robotspace: List[float] = [0.0] * 6
        self.targets_Retro: List[targets.RetroreflectiveTape] = []
        self.targets_Fiducials: List[targets.AprilTagFiducial] = []
        self.targets_Classifier: List[targets.NeuralClassifier] = []
        self.targets_Detector: List[targets.NeuralDetector] = []
        self.targets_Barcode: List[targets.Barcode] = []

    @classmethod
    def fromJson(cls, json_data: dict) -> "LimelightResults":
        obj = LimelightResults()
        obj.error = json_data.get("error", None)
        obj.pipelineID = json_data.get("pID", 0.0)
        obj.latency_pipeline = json_data.get("tl", 0.0)
        obj.latency_capture = json_data.get("cl", 0.0)
        obj.latency_jsonParse = json_data.get("latency_jsonParse", 0.0)
        obj.timestamp_LIMELIGHT_publish = json_data.get("ts", 0.0)
        obj.timestamp_RIOFPGA_capture = json_data.get("ts_rio", 0.0)
        obj.valid = json_data.get("v", False)
        obj.botpose = json_data.get("botpose", [0.0] * 6)
        obj.botpose_wpired = json_data.get("botpose_wpired", [0.0] * 6)
        obj.botpose_wpiblue = json_data.get("botpose_wpiblue", [0.0] * 6)
        obj.botpose_tagcount = json_data.get("botpose_tagcount", 0.0)
        obj.botpose_span = json_data.get("botpose_span", 0.0)
        obj.botpose_avgdist = json_data.get("botpose_avgdist", 0.0)
        obj.botpose_avgarea = json_data.get("botpose_avgarea", 0.0)
        obj.camerapose_robotspace = json_data.get("t6c_rs", [0.0] * 6)

        obj.targets_Retro = [
            targets.RetroreflectiveTape.fromJson(x) for x in json_data.get("Retro", [])
        ]
        obj.targets_Fiducials = [
            targets.AprilTagFiducial.fromJson(x) for x in json_data.get("Fiducial", [])
        ]
        obj.targets_Classifier = [
            targets.NeuralClassifier.fromJson(x)
            for x in json_data.get("Classifier", [])
        ]
        obj.targets_Detector = [
            targets.NeuralDetector.fromJson(x) for x in json_data.get("Detector", [])
        ]
        obj.targets_Barcode = [
            targets.Barcode.fromJson(x) for x in json_data.get("Barcode", [])
        ]

        return obj

    def __str__(self) -> str:
        result = []
        result.append("Partial JSON LimelightResults")
        result.append(f"error {self.error}")
        result.append(f"latency_jsonParse {self.latency_jsonParse}")
        result.append(f"pID {self.pipelineID}")
        result.append(f"tl {self.latency_pipeline}")
        result.append(f"cl {self.latency_capture}")
        result.append(f"ts {self.timestamp_LIMELIGHT_publish}")
        result.append(f"ts_rio {self.timestamp_RIOFPGA_capture}")
        result.append(f"v {self.valid}")
        result.append(f"botpose 3d {self.getBotPose3d()}")
        result.append(
            f"botpose_wpired 3d {self.getBotPose3d(DriverStation.Alliance.kRed)}"
        )
        result.append(
            f"botpose_wpiblue 3d {self.getBotPose3d(DriverStation.Alliance.kBlue)}"
        )
        result.append(f"botpose 2d {self.getBotPose2d()}")
        result.append(
            f"botpose_wpired 2d {self.getBotPose2d(DriverStation.Alliance.kRed)}"
        )
        result.append(
            f"botpose_wpiblue 2d {self.getBotPose2d(DriverStation.Alliance.kBlue)}"
        )
        result.append(f"botpose_tagcount {self.botpose_tagcount}")
        result.append(f"botpose_span {self.botpose_span}")
        result.append(f"botpose_avgdist {self.botpose_avgdist}")
        result.append(f"botpose_avgarea {self.botpose_avgarea}")
        result.append(f"t6c_rs {self.camerapose_robotspace}")
        result.append(f"Retro {self.targets_Retro}")
        result.append(f"Fiducial {self.targets_Fiducials}")
        result.append(f"Classifier {self.targets_Classifier}")
        result.append(f"Detector {self.targets_Detector}")
        result.append(f"Barcode {self.targets_Barcode}")

        return "\n".join(result)

    def getBotPose3d(
        self, alliance: Optional[DriverStation.Alliance] = None
    ) -> Optional[geometry.Pose3d]:
        if alliance:
            if alliance == DriverStation.Alliance.kRed:
                return LimelightUtils.toPose3d(self.botpose_wpired)
            else:
                return LimelightUtils.toPose3d(self.botpose_wpiblue)
        else:
            return LimelightUtils.toPose3d(self.botpose)

    def getBotPose2d(
        self, alliance: Optional[DriverStation.Alliance] = None
    ) -> Optional[geometry.Pose2d]:
        if alliance:
            if alliance == DriverStation.Alliance.kRed:
                return LimelightUtils.toPose2d(self.botpose_wpired)
            else:
                return LimelightUtils.toPose2d(self.botpose_wpiblue)
        else:
            return LimelightUtils.toPose2d(self.botpose)


class Limelight:
    def __init__(self, name: str) -> None:
        if not Limelight.isAvailable(name) and not RobotBase.isSimulation():
            raise RuntimeError("Limelight not available")
        self.limelightName: Final[str] = name
        self.__limelightData: LimelightData
        self.__settings: LimelightSettings

    @classmethod
    def isAvailable(cls, limelightName: str) -> bool:
        import time

        for _ in range(15):
            if (
                ntcore.NetworkTableInstance.getDefault()
                .getTable(limelightName)
                .containsKey("getpipe")
            ):
                return True
            try:
                time.sleep(1)  # seconds
            except Exception as e:
                print(e)

        available = "\n".join(
            name
            for name in ntcore.NetworkTableInstance.getDefault()
            .getTable("/")
            .getSubTables()
            if name.startswith("limelight")
        )

        errMsg: str = f"""Your limelight name "{limelightName}" is invalid. Doesn't exist on network (no getpipe key).
        These may be available: {available}"""

        Alert(errMsg, Alert.AlertType.kError).set(True)
        return False

    def createPoseEstimator(self, megatag2: bool) -> "LimelightPoseEstimator":
        return LimelightPoseEstimator(self, megatag2)

    def getSettings(self) -> LimelightSettings:
        return self.__settings

    def getData(self) -> LimelightData:
        return self.__limelightData

    def snapshot(self, snapshotName: str):
        import urllib.error
        import urllib.request
        from concurrent.futures import ThreadPoolExecutor

        def task():
            url = LimelightUtils.getLimelightURLString("limelight", "capturesnapshot")
            if url:
                try:
                    request = urllib.request.Request(url, method="GET")
                    if snapshotName:
                        request.add_header("snapname", snapshotName)

                    with urllib.request.urlopen(request) as response:
                        if response.status == 200:
                            return True
                        else:
                            print("Bad LL Request")
                except urllib.error.URLError as e:
                    print(e.reason)
                return False

        executor = ThreadPoolExecutor()
        executor.submit(task)

    def getLatestResults(self) -> Optional[LimelightResults]:
        self.__limelightData.getResults()

    def flush(self) -> None:
        ntcore.NetworkTableInstance.getDefault().flush()

    def getNTTable(self) -> ntcore.NetworkTable:
        return ntcore.NetworkTableInstance.getDefault().getTable(self.limelightName)


class EstimationMode(Enum):
    MEGATAG1 = 0
    MEGATAG2 = 1


class LimelightPoseEstimator:
    def __init__(self, camera: Limelight, estimationMode: EstimationMode) -> None:
        self.__limelight: Final[Limelight] = camera
        self.__estimationMode = estimationMode
        self.__botpose: ntcore.DoubleArrayEntry = (
            self.__limelight.getNTTable()
            .getDoubleArrayTopic("botpose")
            .getEntry(defaultValue=[])
        )

    @deprecated
    def getBotPose(self) -> Optional[geometry.Pose3d]:
        return LimelightUtils.toPose3d(self.__botpose.get())

    def getAlliancePoseEstimate(self) -> Optional[PoseEstimate]:
        alliance = DriverStation.getAlliance()
        if not alliance:
            return None
        if alliance == DriverStation.Alliance.kRed:
            return (
                BotPose.RED_MEGATAG2.get(self.__limelight)
                if self.__estimationMode == EstimationMode.MEGATAG2
                else BotPose.RED.get(self.__limelight)
            )
        elif alliance == DriverStation.Alliance.kBlue:
            return (
                BotPose.BLUE_MEGATAG2.get(self.__limelight)
                if self.__estimationMode == EstimationMode.MEGATAG2
                else BotPose.BLUE.get(self.__limelight)
            )
        return None

    def getPoseEstimate(self) -> Optional[PoseEstimate]:
        return (
            BotPose.BLUE_MEGATAG2.get(self.__limelight)
            if self.__estimationMode == EstimationMode.MEGATAG2
            else BotPose.BLUE.get(self.__limelight)
        )
