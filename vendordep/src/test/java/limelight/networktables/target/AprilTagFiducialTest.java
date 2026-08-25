package limelight.networktables.target;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.util.Units;
import org.junit.jupiter.api.Test;

class AprilTagFiducialTest
{

  @Test
  void allArgsConstructorPopulatesFieldsAndPoseGetters()
  {
    double[] cameraPoseTargetSpace = new double[]{1, 2, 3, 4, 5, 6};
    double[] robotPoseFieldSpace = new double[]{7, 8, 9, 10, 11, 12};
    double[] robotPoseTargetSpace = new double[]{13, 14, 15, 16, 17, 18};
    double[] targetPoseCameraSpace = new double[]{19, 20, 21, 22, 23, 24};
    double[] targetPoseRobotSpace = new double[]{25, 26, 27, 28, 29, 30};

    AprilTagFiducial fiducial = new AprilTagFiducial(4, "36h11", 1.5, 2.5, -3.5, 100, -150, 2.5, -3.5, 12345,
                                                      cameraPoseTargetSpace, robotPoseFieldSpace,
                                                      robotPoseTargetSpace, targetPoseCameraSpace,
                                                      targetPoseRobotSpace);

    assertEquals(4, fiducial.fiducialID);
    assertEquals("36h11", fiducial.fiducialFamily);
    assertEquals(1.5, fiducial.ta);
    assertEquals(2.5, fiducial.tx);
    assertEquals(-3.5, fiducial.ty);
    assertEquals(100, fiducial.tx_pixels);
    assertEquals(-150, fiducial.ty_pixels);
    assertEquals(2.5, fiducial.tx_nocrosshair);
    assertEquals(-3.5, fiducial.ty_nocrosshair);
    assertEquals(12345, fiducial.ts);

    Pose3d expectedCameraPoseTargetSpace = new Pose3d(1, 2, 3, new edu.wpi.first.math.geometry.Rotation3d(
        Units.degreesToRadians(4), Units.degreesToRadians(5), Units.degreesToRadians(6)));
    assertEquals(expectedCameraPoseTargetSpace, fiducial.getCameraPose_TargetSpace());

    Pose3d expectedRobotPoseFieldSpace = new Pose3d(7, 8, 9, new edu.wpi.first.math.geometry.Rotation3d(
        Units.degreesToRadians(10), Units.degreesToRadians(11), Units.degreesToRadians(12)));
    assertEquals(expectedRobotPoseFieldSpace, fiducial.getRobotPose_FieldSpace());
  }

}
