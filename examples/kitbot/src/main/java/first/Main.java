package first;

import org.wpilib.framework.RobotBase;

/** Starts the robot. Don't add anything here. */
public final class Main {
  private Main() {}

  /**
   * Starts the robot.
   *
   * @param args Unused.
   */
  public static void main(String... args) {
    RobotBase.startRobot(first.robot.Robot::new);
  }
}
