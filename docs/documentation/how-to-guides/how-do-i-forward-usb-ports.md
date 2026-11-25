# How do I forward USB ports?

Limelight 3A/3G units connected over USB (rather than the robot network) need port forwarding through the roboRIO before their web interface or video stream is reachable from a driver station laptop.

## Set up forwarding once, at robot init

```java
@Override
public void robotInit() {
    Limelight.setupPortForwardingUSB(0); // first USB-connected Limelight
}
```

For a second USB-connected Limelight, use index `1`, and so on. Each index maps to its own IP (`172.29.<index>.1`) and port block.

## Accessing the web UI

| USB index | IP forwarded from      | Web UI URL                             |
| --------- | ---------------------- | -------------------------------------- |
| `0`       | `172.29.0.1:5800-5809` | `http://roboRIO-<team>-FRC.local:5801` |
| `1`       | `172.29.1.1:5810-5819` | `http://roboRIO-<team>-FRC.local:5811` |

The port offset within each block (`+1` for the web UI) matches the Limelight's own documented port layout. `setupPortForwardingUSB` just forwards the whole 10-port block per camera.

## See Also

* Limelight#setupPortForwardingUSB
