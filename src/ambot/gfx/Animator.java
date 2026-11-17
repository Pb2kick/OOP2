package ambot.gfx;

/** Plays one {@link AnimationClip} at a time, advanced by game ticks. */
public final class Animator {
    private AnimationClip clip;
    private int elapsed;

    /** Starts a clip; playing the clip that is already playing changes nothing. */
    public void play(AnimationClip next) {
        if (next != clip) {
            clip = next;
            elapsed = 0;
        }
    }

    public void restart() {
        elapsed = 0;
    }

    public void tick() {
        if (clip != null && elapsed < Integer.MAX_VALUE) {
            elapsed++;
        }
    }

    public AnimationClip clip() {
        return clip;
    }

    public int frameIndex() {
        if (clip == null) return 0;
        int step = elapsed / clip.ticksPerFrame();
        return clip.loop() ? step % clip.frameCount() : Math.min(step, clip.frameCount() - 1);
    }

    public int column() {
        return clip == null ? 0 : clip.startCol() + frameIndex();
    }

    /** True once a non-looping clip has shown its last frame for its full duration. */
    public boolean isFinished() {
        return clip != null && !clip.loop() && elapsed >= clip.frameCount() * clip.ticksPerFrame();
    }
}
