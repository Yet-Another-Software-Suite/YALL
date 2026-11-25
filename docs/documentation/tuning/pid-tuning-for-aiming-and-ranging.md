# PID Tuning for Aiming & Ranging

Once tracking itself is stable (pipeline tuned, [crosshair calibrated](crosshair-calibration.md)), this is the last step: closing the loop between `tx`/`ty` and drivetrain output. This only applies to direct visual servoing off `tx`/`ty`/`ta` (`limelight.getData().targetData.getHorizontalOffset()` / `getVerticalOffset()` / `getTargetArea()`, as in [Basic Setup](../tutorials/basic-setup.md)); if you're driving off a fused `PoseEstimate` instead, this doesn't apply, so tune your pose estimator's trust filters ([Pose Estimation & Ambiguity](../understanding/pose-estimation-and-ambiguity.md)) instead.

## Starting gains

Limelight's own aiming/ranging example uses simple proportional-only control as its starting point, a reasonable baseline before you add integral/derivative terms:

| Term            | Starting value | Drives                                                                                                                                                             |
| --------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `kP_aim`        | `-0.1`         | Turn rate, from `tx` (heading error)                                                                                                                               |
| `kP_distance`   | `-0.1`         | Forward/back speed, from `ty` (range error)                                                                                                                        |
| `minAimCommand` | `0.05`         | Deadband floor: the minimum command applied once outside the deadband, so small errors don't get commanded a near-zero output that never overcomes static friction |

The negative sign matters: a positive `tx` (target to the right) should produce a _negative_ turn command if your convention is "positive turn = counterclockwise." Get the sign backwards and the loop actively drives away from the target instead of toward it.

## Deadband

Only apply the aim correction once `|tx| > 1.0` degrees (this exact threshold is a starting point, not a hard rule). Without a deadband, the loop chases sensor noise once it's essentially on-target, and oscillates instead of settling.

```java
double tx = limelight.getData().targetData.getHorizontalOffset();
double steer = 0.0;
if (Math.abs(tx) > 1.0) {
    steer = kP_aim * tx;
}
```

## Tuning order

1. **Aim (`tx`) alone first**, with ranging disabled. Confirm the robot settles on heading without oscillating before adding range control.
2. **Range (`ty`/`ta`) alone next**, with aim disabled, same idea: isolate one loop before combining them.
3. **Combine both** once each is independently stable. Combined aim+range is more sensitive to gain that's slightly too high, since heading and distance errors can compound.
4. **Increase `kP` gradually** from the starting values above until the robot responds promptly without overshoot/oscillation at your target distance; back off \~20% from the point oscillation starts.

## See Also

* [Tuning Workflow Overview](tuning-workflow-overview.md)
* [Crosshair Calibration](crosshair-calibration.md)
* [Pose Estimation & Ambiguity](../understanding/pose-estimation-and-ambiguity.md): the equivalent tuning problem when driving off a fused pose instead of raw `tx`/`ty`.
