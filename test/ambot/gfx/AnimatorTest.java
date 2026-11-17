package ambot.gfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnimatorTest {
    private static final AnimationClip LOOP = new AnimationClip("loop", 0, 2, 4, 3, true, 36);
    private static final AnimationClip ONCE = new AnimationClip("once", 1, 0, 3, 2, false, 36);

    private static void tick(Animator a, int n) {
        for (int i = 0; i < n; i++) a.tick();
    }

    @Test
    void loopAdvancesAndWraps() {
        Animator a = new Animator();
        a.play(LOOP);
        tick(a, 3);
        assertEquals(1, a.frameIndex());
        tick(a, 9);
        assertEquals(0, a.frameIndex());
    }

    @Test
    void columnIsStartColPlusFrameIndex() {
        Animator a = new Animator();
        a.play(LOOP);
        tick(a, 6);
        assertEquals(2 + 2, a.column());
    }

    @Test
    void oneShotFinishesAndHoldsLastFrame() {
        Animator a = new Animator();
        a.play(ONCE);
        tick(a, 5);
        assertFalse(a.isFinished());
        assertEquals(2, a.frameIndex());
        tick(a, 1);
        assertTrue(a.isFinished());
        tick(a, 4);
        assertEquals(2, a.frameIndex());
    }

    @Test
    void playingSameClipDoesNotReset() {
        Animator a = new Animator();
        a.play(LOOP);
        tick(a, 4);
        a.play(LOOP);
        assertEquals(1, a.frameIndex());
    }

    @Test
    void playingOtherClipResets() {
        Animator a = new Animator();
        a.play(ONCE);
        tick(a, 6);
        a.play(LOOP);
        assertEquals(0, a.frameIndex());
        assertFalse(a.isFinished());
        assertEquals(LOOP, a.clip());
    }
}
