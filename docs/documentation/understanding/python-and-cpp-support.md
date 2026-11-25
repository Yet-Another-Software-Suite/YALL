---
icon: code
---

# Python & C++ Support

## Java only

The YALL camera API (`VisionCamera`, the vendor wrappers, `VisionPoseFilter`, `VisionSystem` and `VisionSim`) is Java-only. The vendordep ships no C++ or Python artifact.

## The old Limelight library

The repository's `yall/python` and `yall/cpp` folders hold the earlier, Limelight-only YALL library, not the camera API on this site.

{% hint style="info" %}
The Python port of the earlier library was written by FRC Team 9738, who translated the Java API and contributed it back to YALL.
{% endhint %}

* **Python**: a `yall` package mirroring the old Java `Limelight` API (`from yall.limelight import Limelight`). It reads Limelight NetworkTables directly and has no PhotonVision or Synapse support.
* **C++**: `yall/cpp/Limelight.cpp` is a placeholder; no C++ library is built.

For Limelight in C++, use LimelightLib 2's own C++ library. For PhotonVision, use PhotonLib's C++ library.

## Read more

* [Why YALL?](../why-yall.md)
