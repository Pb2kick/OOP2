package ambot.save;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * What the game remembers between runs (spec section 5.9): which levels are completed, and the
 * settings. Volumes are 0..10 and are clamped on creation.
 */
public record SaveData(Set<String> completed, int musicVolume, int sfxVolume, boolean fullscreen) {
    public static final int MAX_VOLUME = 10;

    public SaveData {
        completed = Collections.unmodifiableSet(new TreeSet<>(completed));
        musicVolume = clamp(musicVolume);
        sfxVolume = clamp(sfxVolume);
    }

    public static SaveData defaults() {
        return new SaveData(Set.of(), 7, 8, false);
    }

    public SaveData withCompleted(String id) {
        Set<String> more = new TreeSet<>(completed);
        more.add(id);
        return new SaveData(more, musicVolume, sfxVolume, fullscreen);
    }

    public SaveData withoutProgress() {
        return new SaveData(Set.of(), musicVolume, sfxVolume, fullscreen);
    }

    public SaveData withMusic(int volume) {
        return new SaveData(completed, volume, sfxVolume, fullscreen);
    }

    public SaveData withSfx(int volume) {
        return new SaveData(completed, musicVolume, volume, fullscreen);
    }

    public SaveData withFullscreen(boolean on) {
        return new SaveData(completed, musicVolume, sfxVolume, on);
    }

    static int clamp(int volume) {
        return Math.max(0, Math.min(MAX_VOLUME, volume));
    }
}
