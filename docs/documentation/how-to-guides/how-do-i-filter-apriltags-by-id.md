# How do I filter AprilTags by ID?

## Restrict tracking to specific tags

If your field has tags you never want to localize off of (opposing alliance station tags, tags outside your robot's operating area), restrict the fiducial pipeline to a known-good set:

```java
limelight.getSettings()
         .withAprilTagIdFilter(List.of(17, 18, 19, 20, 21, 22, 6, 7, 8, 9, 10, 11))
         .save();
```

This affects every fiducial-based reading from this camera: `LimelightResults.targets_Fiducials`, MegaTag pose estimates, and raw fiducials.

## Prioritize one tag when several are visible

If you need MegaTag to favor a specific tag when multiple are in view (e.g. targeting a specific scoring location), set a priority ID instead of / in addition to a filter:

```java
limelight.getSettings().withPriorityTagId(7).save();
```

## Reading which tag is currently targeted

```java
double currentTagId = limelight.getData().targetData.getAprilTagID();
```

## See Also

* LimelightSettings reference
* [Target & Pipeline Types](../understanding/target-and-pipeline-types.md)
