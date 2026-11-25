# How do I install YALL?

## Vendordep (recommended)

In WPILib VS Code: **WPILib: Manage Vendor Libraries → Install new library (online)**, then paste:

```
https://Yet-Another-Software-Suite.github.io/YALL/yall.json
```

## Manual installation

Download the latest release from the [releases page](https://github.com/BroncBotz3481/YALL/releases) and drop it into `src/main` of your FRC project.

## Verify it worked

```java
import limelight.Limelight;
```

should resolve without errors after a Gradle sync. Javadocs for every class are at [yet-another-software-suite.github.io/YALL/javadocs](https://yet-another-software-suite.github.io/YALL/javadocs/).

## See Also

* [Basic Setup](../tutorials/basic-setup.md)
