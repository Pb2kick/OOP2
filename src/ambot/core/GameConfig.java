package ambot.core;

/** Project-wide constants from the restoration design (spec §5.1–5.2). */
public final class GameConfig {
    private GameConfig() {
    }

    public static final int LOGICAL_WIDTH = 640;
    public static final int LOGICAL_HEIGHT = 360;
    public static final int TILE = 16;
    public static final int UPS = 60;
    public static final int MAX_CATCH_UP = 5;
    public static final int DEFAULT_SCALE = 2;
    public static final String TITLE = "AMBOT";
}
