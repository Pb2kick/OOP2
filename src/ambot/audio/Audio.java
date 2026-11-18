package ambot.audio;

/** Sound effects and music (spec section 5.8). Volumes are 0..10; out-of-range values are clamped. */
public interface Audio {
    void play(Sfx s);

    /** Starts a looping track; does nothing if that track is already playing. */
    void playMusic(Music m);

    void stopMusic();

    void setMusicVolume(int level);

    void setSfxVolume(int level);
}
