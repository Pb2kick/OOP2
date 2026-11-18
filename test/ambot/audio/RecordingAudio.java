package ambot.audio;

import java.util.ArrayList;
import java.util.List;

/** An {@link Audio} for tests: records what was played and the volumes, plays nothing. */
public final class RecordingAudio implements Audio {
    public final List<Sfx> played = new ArrayList<>();
    public Music musicNow;
    public int musicVolume = -1, sfxVolume = -1;

    @Override
    public void play(Sfx s) {
        played.add(s);
    }

    @Override
    public void playMusic(Music m) {
        musicNow = m;
    }

    @Override
    public void stopMusic() {
        musicNow = null;
    }

    @Override
    public void setMusicVolume(int level) {
        musicVolume = level;
    }

    @Override
    public void setSfxVolume(int level) {
        sfxVolume = level;
    }
}
