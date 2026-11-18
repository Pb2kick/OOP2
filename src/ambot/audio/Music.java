package ambot.audio;

import java.util.Locale;

/** Looping music tracks. Each lives at {@code /audio/music/<name>.wav}. */
public enum Music {
    MENU, WORLD1;

    public String path() {
        return "/audio/music/" + name().toLowerCase(Locale.ROOT) + ".wav";
    }
}
