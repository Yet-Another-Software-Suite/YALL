# How do I use a USB Limelight?

LimelightLib 2 names the Limelight vision instance on each SystemCore USB port:

| USB port | Name |
| --- | --- |
| 0 | `Limelight.SYSTEMCORE_USB0` (`"limelightsc0"`) |
| 1 | `Limelight.SYSTEMCORE_USB1` (`"limelightsc1"`) |
| 2 | `Limelight.SYSTEMCORE_USB2` (`"limelightsc2"`) |
| 3 | `Limelight.SYSTEMCORE_USB3` (`"limelightsc3"`) |

Pass the name to `Limelight` and wrap it like any other camera:

```java
VisionCamera camera =
    new LimelightWrapper(new Limelight(Limelight.SYSTEMCORE_USB0), config);
```

{% hint style="info" %}
The roboRIO-era scheme, forwarding ports `5800`-`5809` from `172.29.<index>.1` with
`PortForwarder`, came from the old LimelightHelpers. LimelightLib 2 has no port forwarding, and its
SystemCore cameras are reached by the names above instead.
{% endhint %}

## See Also

* [How do I use multiple cameras?](how-do-i-use-multiple-cameras.md)
