package ambot.audio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Audio through {@code javax.sound.sampled}: each sound effect is preloaded into a few clips
 * used in turn so quick repeats overlap; music is loaded when first played and loops. If
 * anything goes wrong while setting up, {@link #createOrSilent()} returns {@link SilentAudio}.
 */
public final class SampledAudio implements Audio {
    private static final int CLIPS_PER_SFX = 3;

    private final Function<String, InputStream> resources;
    private final Map<Sfx, Clip[]> sfx = new EnumMap<>(Sfx.class);
    private final Map<Sfx, Integer> next = new EnumMap<>(Sfx.class);
    private Clip music;
    private Music musicNow;
    private int musicVolume = 7, sfxVolume = 8;
    private boolean musicWarned;

    private SampledAudio(Function<String, InputStream> resources)
            throws IOException, LineUnavailableException, UnsupportedAudioFileException {
        this.resources = resources;
        for (Sfx s : Sfx.values()) {
            byte[] data = read(s.path());
            Clip[] clips = new Clip[CLIPS_PER_SFX];
            for (int i = 0; i < clips.length; i++) clips[i] = open(data);
            sfx.put(s, clips);
            next.put(s, 0);
        }
    }

    public static Audio createOrSilent() {
        return createOrSilentFrom(SampledAudio.class::getResourceAsStream);
    }

    static Audio createOrSilentFrom(Function<String, InputStream> resources) {
        try {
            return new SampledAudio(resources);
        } catch (Exception | LinkageError e) {
            System.err.println("AMBOT: sound is off (" + e + ")");
            return new SilentAudio();
        }
    }

    /** Volume level 0..10 as a gain in decibels: 0 is silence (-80 dB), 10 is full (0 dB). */
    static float gainDb(int level) {
        int l = Math.max(0, Math.min(10, level));
        if (l == 0) return -80f;
        return (float) (20 * Math.log10(l / 10.0));
    }

    @Override
    public void play(Sfx s) {
        if (sfxVolume == 0) return;
        Clip[] clips = sfx.get(s);
        int i = next.get(s);
        next.put(s, (i + 1) % clips.length);
        Clip c = clips[i];
        c.stop();
        c.setFramePosition(0);
        setGain(c, sfxVolume);
        c.start();
    }

    @Override
    public void playMusic(Music m) {
        if (m == musicNow) return;
        stopMusic();
        musicNow = m;
        try {
            music = open(read(m.path()));
            setGain(music, musicVolume);
            music.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException | RuntimeException e) {
            if (!musicWarned) System.err.println("AMBOT: music is off (" + e + ")");
            musicWarned = true;
            music = null;
        }
    }

    @Override
    public void stopMusic() {
        if (music != null) {
            music.stop();
            music.close();
            music = null;
        }
        musicNow = null;
    }

    @Override
    public void setMusicVolume(int level) {
        musicVolume = Math.max(0, Math.min(10, level));
        if (music != null) setGain(music, musicVolume);
    }

    @Override
    public void setSfxVolume(int level) {
        sfxVolume = Math.max(0, Math.min(10, level));
    }

    private byte[] read(String path) throws IOException {
        try (InputStream in = resources.apply(path)) {
            if (in == null) throw new IOException("missing sound " + path);
            return in.readAllBytes();
        }
    }

    private static Clip open(byte[] wav) throws IOException, LineUnavailableException, UnsupportedAudioFileException {
        Clip clip = AudioSystem.getClip();
        try (AudioInputStream in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav))) {
            clip.open(in);
        }
        return clip;
    }

    private static void setGain(Clip c, int level) {
        if (!c.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), gainDb(level))));
    }
}
