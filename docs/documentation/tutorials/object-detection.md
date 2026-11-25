# Object Detection

This tutorial finds game pieces with a camera running an object detection pipeline: a Limelight neural detector or a PhotonVision object detection pipeline. Detected objects come back as `VisionTarget`s with a class ID and confidence, the same for both vendors.

{% hint style="info" %}
Synapse's Java library reports AprilTag and color pipelines, not object detection. A Synapse color pipeline's targets come back with yaw and pitch but no class ID.
{% endhint %}

## Reading detections

```java
camera.update();

List<VisionTarget> objects = camera.getLatestResult()
    .map(VisionResult::getDetectedObjects)
    .orElse(List.of());

for (VisionTarget object : objects) {
  int classId = object.classId();         // index into your model's labels
  double confidence = object.confidence(); // 0 to 1
  double yaw = object.yawDegrees();       // positive right
  double pitch = object.pitchDegrees();   // positive up
}
```

`isDetectedObject()` is true for targets with a class ID. Class IDs are the order of labels in your model; keep a list of names next to it in your code.

## Driving to the closest game piece

On a downward-tilted camera, the closest game piece is the one lowest in the image:

```java
Optional<VisionTarget> closest = objects.stream()
    .filter(o -> o.classId() == GAME_PIECE_CLASS && o.confidence() > 0.6)
    .min(Comparator.comparingDouble(VisionTarget::pitchDegrees));

closest.ifPresent(piece -> {
  double turn = -kP * piece.yawDegrees();
  drive.arcadeDrive(0.5, turn);
});
```

Apply a confidence threshold in code when you want it stricter than the pipeline's, for example only in autonomous.

## Simulating game pieces

Add game pieces to the simulated world with a class ID. Every simulated camera, whatever its vendor, then reports them as detected objects:

```java
VisionTargetSim piece = new VisionTargetSim(
    new Pose3d(5, 4, 0.1, Rotation3d.ZERO),
    new TargetModel(0.15), // a 15 cm sphere
    GAME_PIECE_CLASS,
    0.9f);
VisionSim.getDefault().addVisionTargets("gamePieces", piece);
```

`VisionTargetSim` and `TargetModel` are PhotonVision's (`org.photonvision.simulation`, `org.photonvision.estimation`). Remove the group with `removeVisionTargets("gamePieces")`.

## See Also

* [Tuning a Neural Network Pipeline](../tuning/neural-network-pipeline-tuning.md)
* [Simulating Cameras](simulating-cameras.md)
* [Results Model](../understanding/results-model.md)
