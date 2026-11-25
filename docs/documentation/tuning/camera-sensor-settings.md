---
icon: sun
---

# Camera Sensor Settings

Before anything else, tune the camera's sensor in its own web page. This is the same image feeding every pipeline type, so getting it right first makes AprilTag detection more consistent and retroreflective thresholding dramatically easier.

## Tuning order

{% stepper %}
{% step %}
### Set exposure low and fixed

**Exposure** is how long the camera lets light in for each picture. Low and fixed (not automatic) blacks out most arena lighting, which is what you want: field lighting is bright and changes, and a long or automatic exposure washes out targets and blurs them while the robot moves.

**Check it:** most of the picture is dark, and targets stay sharp while you move the camera.
{% endstep %}

{% step %}
### Raise black level next (Limelight)

Black level darkens the picture at the sensor, without the noise or blur a longer exposure would add. PhotonVision's closest setting is **Brightness**.

**Check it:** arena lights fade, and targets still stand out.
{% endstep %}

{% step %}
### Add gain only if needed

**Gain** brightens the picture but adds speckles (noise). If the picture is too dark after the steps above, add a little gain rather than raising exposure, which keeps motion blur low.

**Check it:** targets are bright enough to detect, without heavy speckles.
{% endstep %}

{% step %}
### Leave color balance alone

Red and blue balance only matter for a color cast problem. Skip them in a normal pass.

**Check it:** nothing to check.
{% endstep %}
{% endstepper %}

The goal is a **mostly black picture** where only the target (an AprilTag, tape or a game piece) stands out.

* Tuning a PhotonVision camera: [Camera Tuning / Input](https://docs.photonvision.org/en/latest/docs/pipelines/input.html).
* Tuning a Limelight: [Pipeline Setup](https://docs.limelightvision.io/docs/docs-limelight/getting-started/pipelines) (the Input tab).

{% hint style="warning" %}
None of these are set through YALL. They're saved on the camera, per pipeline, from its own web page.
{% endhint %}

## Read more

* [Tuning Workflow Overview](tuning-workflow-overview.md)
* [Tuning a Retroreflective Pipeline](retroreflective-pipeline-tuning.md)
