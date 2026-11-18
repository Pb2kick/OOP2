package ambot.audio;

import java.util.Locale;

/** Short sound effects. Each lives at {@code /audio/sfx/<name>.wav}. */
public enum Sfx {
    JUMP, SWING, HIT, HURT, PICKUP, SHOOT, DEATH, LEVEL_CLEAR, MENU_MOVE, MENU_SELECT;

    public String path() {
        return "/audio/sfx/" + name().toLowerCase(Locale.ROOT) + ".wav";
    }
}
