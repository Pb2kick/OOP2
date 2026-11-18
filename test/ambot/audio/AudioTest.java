package ambot.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.jupiter.api.Test;

import ambot.tools.PlaceholderAudio;

class AudioTest {

    @Test
    void gainCurve() {
        assertEquals(-80f, SampledAudio.gainDb(0));
        assertEquals(0f, SampledAudio.gainDb(10), 1e-6);
        assertEquals(-6.02, SampledAudio.gainDb(5), 0.01);
    }

    @Test
    void missingResourceFallsBackToSilent() {
        assertInstanceOf(SilentAudio.class, SampledAudio.createOrSilentFrom(name -> null));
    }

    @Test
    void silentAudioAcceptsEverything() {
        Audio a = new SilentAudio();
        assertDoesNotThrow(() -> {
            for (Sfx s : Sfx.values()) a.play(s);
            for (Music m : Music.values()) a.playMusic(m);
            a.stopMusic();
            a.setMusicVolume(-3);
            a.setSfxVolume(42);
        });
    }

    @Test
    void placeholderFilesDecode() throws Exception {
        for (Sfx s : Sfx.values()) assertDecodes(s.path());
        for (Music m : Music.values()) assertDecodes(m.path());
    }

    private static void assertDecodes(String path) throws Exception {
        try (InputStream raw = AudioTest.class.getResourceAsStream(path)) {
            assertNotNull(raw, "missing " + path);
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
                assertEquals(22050f, in.getFormat().getSampleRate(), path);
                assertEquals(16, in.getFormat().getSampleSizeInBits(), path);
            }
        }
    }

    @Test
    void placeholderAudioIsCurrent() throws Exception {
        for (Sfx s : Sfx.values()) assertArrayEquals(PlaceholderAudio.render(s), bytes(s.path()), s.path() + ": run .\\build.cmd levels");
        for (Music m : Music.values()) assertArrayEquals(PlaceholderAudio.render(m), bytes(m.path()), m.path() + ": run .\\build.cmd levels");
    }

    private static byte[] bytes(String path) throws Exception {
        try (InputStream in = AudioTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            return in.readAllBytes();
        }
    }
}
