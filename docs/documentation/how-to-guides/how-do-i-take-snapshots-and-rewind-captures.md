# How do I take snapshots and rewind captures?

## Snapshots (all hardware)

Asynchronously requests the Limelight to save a still image, without blocking the robot loop:

```java
limelight.snapshot("auto-align-attempt");
```

Pass `null` or `""` to let the Limelight auto-name the snapshot. This runs on a background thread via `CompletableFuture`, so it's safe to call from a periodic loop, though it's typically triggered from a button or a one-shot event rather than every cycle.

## Rewind capture (supported hardware only)

Rewind saves a rolling video buffer you can flush to disk on demand, useful for post-match diagnosis of a missed vision read. Enable recording, then trigger a capture:

```java
limelight.getSettings().withRewindEnable(RewindState.ENABLED).save();

// Later, e.g. bound to a driver button after a suspicious auto-align:
limelight.rewindCapture(10); // capture the last 10 seconds, max 165
```

Capture requests are rate-limited to once every 2 seconds on the Limelight and are dropped if a flush is already in progress.

## Checking rewind buffer status

```java
limelight.getLatestResults().ifPresent(result -> {
    if (result.rewindStats != null) {
        boolean isFull = result.rewindStats.bufferUsage >= 1.0;
        double secondsStored = result.rewindStats.storedSeconds;
    }
});
```

## See Also

* Limelight#rewindCapture
* RewindStats reference
