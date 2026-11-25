# NeuralClassifier

**Package:** `limelight.networktables.target.pipeline`

A single result from the JSON `Classifier` array in [`LimelightResults`](../networktables/limelight-results.md). The classifier pipeline labels the whole image/zone rather than drawing a bounding box.

***

## Fields

| Field                    | Type     | Description                                   |
| ------------------------ | -------- | --------------------------------------------- |
| `className`              | `String` | Predicted class name (`class`).               |
| `classID`                | `double` | Predicted class ID (`classID`).               |
| `confidence`             | `double` | Prediction confidence (`conf`).               |
| `zone`                   | `double` | Zone index the classification applies to.     |
| `tx`, `ty`               | `double` | Object position in the image as a percentage. |
| `tx_pixels`, `ty_pixels` | `double` | Object position in the image as pixels.       |

## Usage

```java
limelight.getLatestResults().ifPresent(result -> {
    for (NeuralClassifier object : result.targets_Classifier) {
        if (object.className.equals("algae") && object.confidence > 0.7) {
            // valid detection
        }
    }
});
```

## See Also

* [NeuralDetector](neural-detector.md)
* [LimelightResults](../networktables/limelight-results.md)
