class RawDetection:
    """
    A class that represents raw detection data, including class ID, coordinates, and other parameters.

    Attributes:
        class_id (int): The class identifier.
        txnc (float): The x-coordinate of the detection center.
        tync (float): The y-coordinate of the detection center.
        ta (float): A parameter related to the detection area (possibly area).
        corner0_X (float): The x-coordinate of the first corner of the detected object.
        corner0_Y (float): The y-coordinate of the first corner of the detected object.
        corner1_X (float): The x-coordinate of the second corner of the detected object.
        corner1_Y (float): The y-coordinate of the second corner of the detected object.
        corner2_X (float): The x-coordinate of the third corner of the detected object.
        corner2_Y (float): The y-coordinate of the third corner of the detected object.
        corner3_X (float): The x-coordinate of the fourth corner of the detected object.
        corner3_Y (float): The y-coordinate of the fourth corner of the detected object.
    """

    def __init__(
        self,
        class_id: int = 0,
        txnc: float = 0.0,
        tync: float = 0.0,
        ta: float = 0.0,
        corner0_X: float = 0.0,
        corner0_Y: float = 0.0,
        corner1_X: float = 0.0,
        corner1_Y: float = 0.0,
        corner2_X: float = 0.0,
        corner2_Y: float = 0.0,
        corner3_X: float = 0.0,
        corner3_Y: float = 0.0,
    ):
        """
        Initializes a RawDetection instance with the provided values.

        Args:
            class_id (int): The class identifier.
            txnc (float): The x-coordinate of the detection center.
            tync (float): The y-coordinate of the detection center.
            ta (float): A parameter related to the detection area.
            corner0_X (float): The x-coordinate of the first corner.
            corner0_Y (float): The y-coordinate of the first corner.
            corner1_X (float): The x-coordinate of the second corner.
            corner1_Y (float): The y-coordinate of the second corner.
            corner2_X (float): The x-coordinate of the third corner.
            corner2_Y (float): The y-coordinate of the third corner.
            corner3_X (float): The x-coordinate of the fourth corner.
            corner3_Y (float): The y-coordinate of the fourth corner.
        """
        self.class_id: int = class_id
        self.txnc: float = txnc
        self.tync: float = tync
        self.ta: float = ta
        self.corner0_X: float = corner0_X
        self.corner0_Y: float = corner0_Y
        self.corner1_X: float = corner1_X
        self.corner1_Y: float = corner1_Y
        self.corner2_X: float = corner2_X
        self.corner2_Y: float = corner2_Y
        self.corner3_X: float = corner3_X
        self.corner3_Y: float = corner3_Y


class RawFiducial:
    """
    Represents a Raw Fiducial result from Limelight's NetworkTables output,
    specifically for the AprilTag results.

    Attributes:
        id (int): The AprilTag ID.
        txnc (float): The X coordinate of the tag in the image.
        tync (float): The Y coordinate of the tag in the image.
        ta (float): The tag's ambiguity as a percentage of the image.
        distToCamera (float): The distance to the camera in meters.
        distToRobot (float): The distance to the robot in meters.
        ambiguity (float): The tag's ambiguity as a percentage [0, 1].
    """

    def __init__(
        self,
        id: int = 0,
        txnc: float = 0.0,
        tync: float = 0.0,
        ta: float = 0.0,
        distToCamera: float = 0.0,
        distToRobot: float = 0.0,
        ambiguity: float = 0.0,
    ):
        """
        Initializes a RawFiducial instance with the provided values.

        Args:
            id (int): The AprilTag ID.
            txnc (float): The X coordinate of the tag in the image.
            tync (float): The Y coordinate of the tag in the image.
            ta (float): The tag's ambiguity as a percentage of the image.
            distToCamera (float): The distance to the camera in meters.
            distToRobot (float): The distance to the robot in meters.
            ambiguity (float): The tag's ambiguity as a percentage [0, 1].
        """
        self.id: int = id
        self.txnc: float = txnc
        self.tync: float = tync
        self.ta: float = ta
        self.distToCamera: float = distToCamera
        self.distToRobot: float = distToRobot
        self.ambiguity: float = ambiguity

    def __str__(self) -> str:
        """
        Converts the RawFiducial instance into a formatted string representation.

        Returns:
            str: The string representation of the RawFiducial instance, including
                 the ID, coordinates, area, distances, and ambiguity.
        """
        return (
            f"Tag ID {self.id}\n"
            f" Coordinate in image ({self.txnc:.2f}, {self.tync:.2f})\n"
            f" Tag Area {self.ta:.2f}\n"
            f" Distance to Camera {self.distToCamera:.2f}\n"
            f" Distance to Robot {self.distToRobot:.2f}\n"
            f" Ambiguity {self.ambiguity:.2f}"
        )
