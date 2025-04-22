from wpimath import geometry
from wpimath.units import radians_per_second


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
