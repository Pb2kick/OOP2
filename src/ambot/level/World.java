package ambot.level;

import java.util.List;

/** A world of the campaign; an unavailable world is shown as "coming soon". */
public record World(int number, String name, List<LevelRef> levels, boolean available) {
    public World {
        levels = List.copyOf(levels);
    }
}
