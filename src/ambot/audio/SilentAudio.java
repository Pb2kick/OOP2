package ambot.audio;

/** Plays nothing: used when there is no sound device or the sound files can't be read. */
public final class SilentAudio implements Audio {
    @Override
    public void play(Sfx s) {
    }

    @Override
    public void playMusic(Music m) {
    }

    @Override
    public void stopMusic() {
    }

    @Override
    public void setMusicVolume(int level) {
    }

    @Override
    public void setSfxVolume(int level) {
    }
}
