# PID Tuning for Aiming & Ranging

Once tracking itself is stable (pipeline tuned, [crosshair calibrated](crosshair-calibration.md)), this is the last step: closing the loop between `tx`/`ty` and drivetrain output. This only applies to direct visual servoing off a target's angles and area (`VisionTarget.yawDegrees()` / `pitchDegrees()` / `areaPercent()`, as in [Basic Setup](../tutorials/basic-setup.md)); if you're driving off a pose estimate instead, this doesn't apply, so tune the [pose filter](../how-to-guides/how-do-i-tune-the-pose-filter.md) instead.

The gains below come from Limelight's documentation, but they work for any camera: YALL's yaw is positive right for every vendor (like Limelight's `tx`), and its pitch is positive up (the negative of Limelight's per-target `ty`).

## Starting gains

Limelight's own aiming/ranging example uses simple proportional-only control as its starting point, a reasonable baseline before you add integral/derivative terms:

| Term | Starting value | Drives |
| --- | --- | --- |
| `kP_aim` | `-0.1` | Turn rate, from yaw (heading error) |
| `kP_distance` | `-0.1` | Forward/back speed, from pitch (range error) |
| `minAimCommand` | `0.05` | Deadband floor: the minimum command applied once outside the deadband, so small errors don't get commanded a near-zero output that never overcomes static friction |

The negative sign matters: a positive yaw (target to the right) should produce a *negative* turn command if your convention is "positive turn = counterclockwise." Get the sign backwards and the loop actively drives away from the target instead of toward it. Check the range sign the same way against your camera's tilt and target height.

## Deadband

Only apply the aim correction once `|yaw| > 1.0` degrees (this exact threshold is a starting point, not a hard rule). Without a deadband, the loop chases sensor noise once it's essentially on-target, and oscillates instead of settling.

```java
double steer = 0.0;
Optional<VisionTarget> target = camera.getBestTarget();
if (target.isPresent() && Math.abs(target.get().yawDegrees()) > 1.0) {
    steer = kP_aim * target.get().yawDegrees();
}
```

## Tuning order

1. **Aim (yaw) alone first**, with ranging disabled. Confirm the robot settles on heading without oscillating before adding range control.
2. **Range (pitch/area) alone next**, with aim disabled, same idea: isolate one loop before combining them.
3. **Combine both** once each is independently stable. Combined aim+range is more sensitive to gain that's slightly too high, since heading and distance errors can compound.
4. **Increase `kP` gradually** from the starting values above until the robot responds promptly without overshoot/oscillation at your target distance; back off ~20% from the point oscillation starts.

## See Also

* [Tuning Workflow Overview](tuning-workflow-overview.md)
* [Crosshair Calibration](crosshair-calibration.md)
* [How do I tune the pose filter?](../how-to-guides/how-do-i-tune-the-pose-filter.md): the equivalent tuning problem when driving off a pose estimate instead of a target's angles.
