---
icon: circle-half-stroke
---

# Tuning a Retroreflective Pipeline

{% hint style="info" %}
This page follows Limelight's retroreflective pipeline. See Limelight's [Color Thresholding](https://docs.limelightvision.io/docs/docs-limelight/pipeline-retro/color-filtering) and [Contour Filtering and Sorting](https://docs.limelightvision.io/docs/docs-limelight/pipeline-retro/contour-filtering-and-sorting). Retroreflective tape bounces light straight back at its source, so it glows in the camera's picture when the camera has lights around it.
{% endhint %}

A retroreflective pipeline finds targets in two sequential stages: an HSV color threshold turns the image into a binary mask, then contour filtering/sorting picks which blob(s) in that mask are the real target. Tune them in that order, since filtering a noisy mask is much harder than filtering a clean one.

{% hint style="info" %}
Do the [camera sensor settings](camera-sensor-settings.md) pass first. A properly dark, low-exposure image makes the threshold step below almost trivial.
{% endhint %}

## Stage 1: HSV color threshold

Switch the web UI's video feed to **threshold** view so you can see the binary mask update live while you tune.

| Slider | What it does |
| --- | --- |
| **Hue** | The "pure" color, independent of brightness (0 = red, about 59 = green). Tune this first, since it's the most discriminating channel. |
| **Saturation** | Color purity: how washed-out (low) vs. pure (high) the color is. |
| **Value** | Brightness: how much black is mixed in. |

Workflow:

1. Use the **eyedropper** tool on the target in the raw camera feed to center the HSV range on it automatically.
2. Refine with the **add**/**subtract** wands rather than dragging sliders blindly.
3. **Raise the minimum Value off of zero.** Leaving it at `0` passes black pixels through the mask, which is almost never what you want.
4. Keep every range **as small as possible**: the tighter the range, the more portable the pipeline is to other fields/lighting rigs.
5. If tracking a red target, enable hue inversion (red wraps around both ends of the hue range).

## Stage 2: Contour filtering and sorting

Once the mask is clean, filter which contours in it count as "the target," in this order:

1. **Target Area**: eliminates obviously-wrong-size blobs first (stadium lights, scoreboard displays). The area slider is quartically scaled, giving finer control at the low end where real FRC targets usually sit.
2. **Target Aspect Ratio**: width ÷ height of the bounding rectangle; discriminates target shape (tall vs. wide). Also nonlinearly scaled.
3. **Target Fullness**: percentage of active pixels inside the bounding rectangle; solid targets approach `1.0`, hollow/U-shaped targets read lower. Use this to reject partial detections that passed area/aspect-ratio by coincidence.
4. **Sort Mode**: how to pick among multiple contours that still pass every filter above. `closest` picks the one nearest the (calibrated) crosshair, usually what you want for aiming.
5. **Target Grouping**: enable dual/tri mode if your real target is made of multiple retroreflective shapes that should be tracked as one.
6. **Smart Speckle Rejection**: apply last, to reject small, close-range artifacts (e.g. LEDs). This is an outlier filter, not a primary one.

{% hint style="warning" %}
Lean on area/aspect-ratio/fullness to do the real work. Outlier rejection (speckle rejection, smart grouping) works *against* you if the individual filters are loose: it starts throwing away legitimate detections instead of noise.
{% endhint %}

## Read more

* [Tuning Workflow Overview](tuning-workflow-overview.md)
* [Results Model](../understanding/results-model.md): retroreflective targets come back as [`VisionTarget`](https://yall.yassrobotics.com/api-reference/java-reference/results/vision-target)s with no tag or class ID.
* [Crosshair Calibration](crosshair-calibration.md): needed before "closest" sort mode means what you expect.
