# Camera Sensor Settings

Before touching any pipeline's thresholds or filters, tune the camera sensor itself in the Limelight web UI's **Camera Settings** tab. This is the same image feeding every pipeline type, so getting it right first makes AprilTag detection more consistent and retroreflective thresholding dramatically easier.

## Tuning order

1. **Exposure: set it low and fixed.** Low, fixed exposure "blacks out" most ambient and arena lighting, which is exactly what you want: FRC field lighting is bright and inconsistent, and a long or auto-adjusting exposure lets it wash out your target and introduces motion blur while the robot is moving.
2. **Black level offset: raise it next.** This is a sensor-level darkening control, distinct from digital brightness. It further suppresses arena lighting without reintroducing the noise or blur that a longer exposure would.
3. **Sensor gain: use sparingly.** Gain brightens the image but adds noise. If the image is too dark after steps 1–2, prefer combining more black level offset with a touch of gain over raising exposure back up, since that keeps motion blur low.
4. **Red/blue balance: leave alone.** Only touch these if you have a specific color-cast problem; they aren't part of the normal tuning pass.

The goal is a **mostly-black image** where only the target (retroreflective tape, AprilTag, game piece) stands out clearly. Everything downstream, from HSV thresholds to contour filters to AprilTag detection reliability, gets easier against that kind of image.

{% hint style="warning" %}
None of these are set through YALL. They're stored in the pipeline's JSON on the Limelight itself and tuned once per pipeline in the web UI, not from robot code.
{% endhint %}

## See Also

* [Tuning Workflow Overview](tuning-workflow-overview.md)
* [Tuning a Retroreflective Pipeline](retroreflective-pipeline-tuning.md)
