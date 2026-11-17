package ambot.level;

import java.util.Map;

/** One object from a level's {@code entities} layer. Point objects have a 0x0 box. */
public record LevelObject(String type, Rect bounds, Map<String, String> properties) {
    public static final String PLAYER_SPAWN = "player_spawn";
    public static final String CHECKPOINT = "checkpoint";
    public static final String EXIT = "exit";
    public static final String ENEMY = "enemy";
    public static final String PICKUP = "pickup";
    public static final String BOSS_ARENA = "boss_arena";

    public LevelObject {
        properties = Map.copyOf(properties);
    }

    /** The property's value, or null when it isn't set. */
    public String property(String key) {
        return properties.get(key);
    }
}
