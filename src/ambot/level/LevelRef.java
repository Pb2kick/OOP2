package ambot.level;

/** One level of a world: its save id (e.g. "w1-2"), its title, and its classpath resource. */
public record LevelRef(String id, String title, String path) {
}
