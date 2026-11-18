package ambot.states;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import ambot.audio.RecordingAudio;
import ambot.audio.Sfx;
import ambot.states.SoundCues.Snapshot;

class SoundCuesTest {
    private final RecordingAudio audio = new RecordingAudio();
    /** Standing, full health, swing 0, two enemies with 4 health in total, one pickup, no bolts. */
    static final Snapshot CALM = new Snapshot(3, 0, false, 4, 1, 0, false);

    private List<Sfx> cues(Snapshot before, Snapshot after) {
        SoundCues.observe(before, after, audio);
        return audio.played;
    }

    @Test
    void jumpCue() {
        assertEquals(List.of(Sfx.JUMP), cues(CALM, new Snapshot(3, 0, true, 4, 1, 0, false)));
    }

    @Test
    void swingCue() {
        assertEquals(List.of(Sfx.SWING), cues(CALM, new Snapshot(3, 1, false, 4, 1, 0, false)));
    }

    @Test
    void hurtCue() {
        // a hit knocks the player upward too, which must not count as a jump
        assertEquals(List.of(Sfx.HURT), cues(CALM, new Snapshot(2, 0, true, 4, 1, 0, false)));
    }

    @Test
    void deathCueNotHurt() {
        Snapshot lastHeart = new Snapshot(1, 0, false, 4, 1, 0, false);
        assertEquals(List.of(Sfx.DEATH), cues(lastHeart, new Snapshot(0, 0, false, 4, 1, 0, true)));
    }

    @Test
    void hitCue() {
        assertEquals(List.of(Sfx.HIT), cues(CALM, new Snapshot(3, 0, false, 3, 1, 0, false)));
    }

    @Test
    void pickupCue() {
        Snapshot hurt = new Snapshot(2, 0, false, 4, 1, 0, false);
        assertEquals(List.of(Sfx.PICKUP), cues(hurt, new Snapshot(3, 0, false, 4, 0, 0, false)));
    }

    @Test
    void shootCue() {
        assertEquals(List.of(Sfx.SHOOT), cues(CALM, new Snapshot(3, 0, false, 4, 1, 1, false)));
    }

    @Test
    void quietTickPlaysNothing() {
        assertEquals(List.of(), cues(CALM, CALM));
    }
}
