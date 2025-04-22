from typing import Final, List, Optional
import math
from urllib.parse import urlunparse
from wpimath import geometry
from wpimath import units
from wpimath.units import radians_per_second
from limelight import Limelight
import ntcore
import results


class AngularVelocity3d:
    """
    A helper class to represent the 3D angular velocity components (roll, pitch, yaw).
    """

    def __init__(
        self,
        roll: radians_per_second,
        pitch: radians_per_second,
        yaw: radians_per_second,
    ):
        self.roll = roll
        self.pitch = pitch
        self.yaw = yaw


class Orientation3d:
    """
    Orientation3d of the robot, representing its current orientation and angular velocities
    along the roll, pitch, and yaw axes.
    """

    def initFromComponents(
        self,
        orientation: geometry.Rotation3d,
        yaw: radians_per_second,
        pitch: radians_per_second,
        roll: radians_per_second,
    ):
        """
        Create the robot orientation based on individual angular velocity components.

        Args:
            orientation (Rotation3d): The robot's orientation.
            yaw (AngularVelocity): Angular velocity around the yaw/z-axis.
            pitch (AngularVelocity): Angular velocity around the pitch/y-axis.
            roll (AngularVelocity): Angular velocity around the roll/x-axis.
        """
        self.orientation = orientation
        self.angularVelocity = AngularVelocity3d(roll, pitch, yaw)

    def initFromAngularVelocity(
        self, orientation: geometry.Rotation3d, angularVelocity: AngularVelocity3d
    ):
        """
        Create the robot orientation based on a single AngularVelocity3d object.

        Args:
            orientation (Rotation3d): The robot's orientation.
            angularVelocity (AngularVelocity3d): Angular velocities around the roll, pitch, and yaw axes.
        """
        self.orientation = orientation
        self.angularVelocity = angularVelocity


class LimelightUtils:
    """
    Utility classes to convert WPILib data to LimelightLib expected values.
    """

    @staticmethod
    def sanitizeName(name: Optional[str]) -> str:
        """
        Sanitize the Limelight name.

        Args:
            name (Optional[str]): Limelight name.

        Returns:
            str: The sanitized Limelight name or "limelight".
        """
        if not name:
            return "limelight"
        return name

    @staticmethod
    def getLimelightURLString(tableName: str, request: str) -> Optional[str]:
        """
        Get the URL for the Limelight.

        Args:
            tableName (str): Limelight name.
            request (str): URI to request from Limelight.

        Returns:
            Optional[str]: URL to request for Limelight, or None if URL is invalid.
        """
        sanitizedName = LimelightUtils.sanitizeName(tableName)
        urlString = f"http://{sanitizedName}.local:5807/{request}"
        try:
            url = urlunparse(
                ("http", f"{sanitizedName}.local", f":5807/{request}", "", "", "")
            )
            return url
        except Exception:
            print("bad LL URL")
        return None

    @staticmethod
    def toPose3d(inData: List[float]) -> Optional[geometry.Pose3d]:
        """
        Takes a 6-length array of pose data and converts it to a 3D pose object.
        Array format: [x, y, z, roll, pitch, yaw] where angles are in degrees.

        Args:
            inData (List[float]): Array containing pose data [x, y, z, roll, pitch, yaw].

        Returns:
            Optional[List[float]]: The 3D pose as a list, or None if invalid data.
        """
        if len(inData) < 6:
            # print("Bad LL 3D Pose Data!")
            return None
        return geometry.Pose3d(
            inData[0],
            inData[1],
            inData[2],
            geometry.Rotation3d(
                math.radians(inData[3]),
                math.radians(inData[4]),
                math.radians(inData[5]),
            ),
        )

    @staticmethod
    def toPose2d(inData: List[float]) -> Optional[geometry.Pose2d]:
        """
        Takes a 6-length array of Pose2d data and converts it to a 2D pose object.
        Uses only x, y, and yaw components, ignoring z, roll, and pitch.
        Array format: [x, y, z, roll, pitch, yaw] where angles are in degrees.

        Args:
            inData (List[float]): Array containing pose data [x, y, z, roll, pitch, yaw].

        Returns:
            Optional[List[float]]: The 2D pose as a list, or None if invalid data.
        """
        if len(inData) < 6:
            # print("Bad LL 2D Pose Data!")
            return None
        return geometry.Pose2d(inData[0], inData[1], math.radians(inData[5]))

    @staticmethod
    def toTranslation3d(translation: List[float]) -> geometry.Translation3d:
        """
        Takes a 3-length array of Translation3d data and converts it into a Translation3d.
        Array format: [x, y, z].

        Args:
            translation (List[float]): Array containing translation data [x, y, z] in meters.

        Returns:
            List[float]: The translation as a list of floats.
        """
        return geometry.Translation3d(translation[0], translation[1], translation[2])

    @staticmethod
    def toOrientation(orientation: List[float]) -> Orientation3d:
        """
        Takes a 6-length array of orientation data and converts it to a 3D orientation object.
        Array format: [yaw, yawrate, pitch, pitchrate, roll, rollrate] in degrees.

        Args:
            orientation (List[float]): Orientation data [yaw, yawrate, pitch, pitchrate, roll, rollrate] in degrees.

        Returns:
            List[float]: The orientation as a list.
        """
        obj: Orientation3d = Orientation3d()
        obj.initFromAngularVelocity(
            geometry.Rotation3d(
                math.radians(orientation[4]),
                math.radians(orientation[2]),
                math.radians(orientation[0]),
            ),
            AngularVelocity3d(orientation[1], orientation[3], orientation[5]),
        )

        return obj

    @staticmethod
    def orientation3dToArray(orientation: Orientation3d) -> List[float]:
        """
        Converts an orientation object to an array of [yaw, yawrate, pitch, pitchrate, roll, rollrate] in degrees.

        Args:
            orientation (List[float]): Orientation object to convert.

        Returns:
            List[float]: Array of [yaw, yawrate, pitch, pitchrate, roll, rollrate] in degrees.
        """
        return [
            orientation.orientation.Z(),
            orientation.angularVelocity.yaw,
            orientation.orientation.Y(),
            orientation.angularVelocity.pitch,
            orientation.orientation.X(),
            orientation.angularVelocity.roll,
        ]

    @staticmethod
    def pose3dToArray(pose: geometry.Pose3d) -> List[float]:
        """
        Converts a 3D pose object to an array of [x, y, z, roll, pitch, yaw].

        Args:
            pose (List[float]): Pose object to convert.

        Returns:
            List[float]: Array containing [x, y, z, roll, pitch, yaw].
        """
        return [
            pose.X(),
            pose.Y(),
            pose.Z(),
            pose.rotation().x_degrees,
            pose.rotation().y_degrees,
            pose.rotation().z_degrees,
        ]

    @staticmethod
    def pose2dToArray(pose: geometry.Pose2d) -> List[float]:
        """
        Converts a 2D pose object to an array of [x, y, 0, 0, 0, yaw].

        Args:
            pose (List[float]): Pose object to convert.

        Returns:
            List[float]: Array containing [x, y, 0, 0, 0, yaw].
        """
        return [
            pose.X(),
            pose.Y(),
            units.radiansToDegrees(0),
            units.radiansToDegrees(0),
            units.radiansToDegrees(0),
            pose.rotation().degrees(),
        ]

    @staticmethod
    def translation3dToArray(translation: geometry.Translation3d) -> List[float]:
        """
        Converts a 3D translation object to an array of [x, y, z].

        Args:
            translation (List[float]): Translation object to convert.

        Returns:
            List[float]: Array containing [x, y, z].
        """
        return [translation.X(), translation.Y(), translation.Z()]

    @staticmethod
    def extractArrayEntry(inData: List[float], position: int) -> float:
        """
        Return a double from a double array if it exists, else return 0.

        Args:
            inData (List[float]): Double array to extract from.
            position (int): Position to read.

        Returns:
            float: The value at the position or 0 if data isn't present.
        """
        if len(inData) <= position:
            return 0
        return inData[position]


class PoseEstimate:
    def __init__(self, camera: Limelight, entryName: str, megaTag2: bool):
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
        self.__limelight: Final[Limelight] = camera
        self.__poseEntry: ntcore.DoubleArrayEntry
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
