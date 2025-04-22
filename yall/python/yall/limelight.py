from typing import Final, Optional

import ntcore
from wpimath import geometry

from yall.networktables.util import LimelightUtils

from .deprecated import deprecated


class Limelight: ...  # TODO


class LimelightPoseEstimator:
    def __init__(self, camera: Limelight, megatag2: bool) -> None:
        self.__limelight: Final[Limelight] = camera
        self.__megatag2: Final[bool] = megatag2
        self.__botpose: ntcore.DoubleArrayEntry  # TODO get value from limelight

    @deprecated
    def getBotPose(self) -> Optional[geometry.Pose3d]:
        return LimelightUtils.toPose3d(self.__botpose.get())
