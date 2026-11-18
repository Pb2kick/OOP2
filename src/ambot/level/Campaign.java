package ambot.level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ambot.save.SaveData;

/**
 * The worlds of the game in order (spec section 5.3), and which levels are unlocked: the first
 * level of World 1, the first level of a world once the previous world is finished, and any
 * level whose predecessor in its world is completed.
 */
public final class Campaign {
    private final List<World> worlds;

    public Campaign(List<World> worlds) {
        this.worlds = List.copyOf(worlds);
    }

    /** World 1 is the placeholder levels until M4b; Worlds 2 and 3 are still to come. */
    public static Campaign standard() {
        List<String> titles = List.of("First Steps", "Up and Through", "Mind the Spikes", "First Blood");
        List<LevelRef> w1 = new ArrayList<>();
        for (int i = 0; i < LevelCatalog.PLACEHOLDER_PATHS.size(); i++) {
            w1.add(new LevelRef("w1-" + (i + 1), titles.get(i), LevelCatalog.PLACEHOLDER_PATHS.get(i)));
        }
        return new Campaign(List.of(
                new World(1, "World 1", w1, true),
                new World(2, "World 2", List.of(), false),
                new World(3, "World 3", List.of(), false)));
    }

    public List<World> worlds() {
        return worlds;
    }

    public World world(int number) {
        for (World w : worlds) {
            if (w.number() == number) return w;
        }
        throw new IllegalArgumentException("no world " + number);
    }

    public boolean isUnlocked(World w, int levelIndex, SaveData d) {
        if (!w.available() || levelIndex < 0 || levelIndex >= w.levels().size()) return false;
        if (levelIndex > 0) return d.completed().contains(w.levels().get(levelIndex - 1).id());
        int i = worlds.indexOf(w);
        if (i <= 0) return true;
        World previous = worlds.get(i - 1);
        return !previous.levels().isEmpty()
                && d.completed().contains(previous.levels().get(previous.levels().size() - 1).id());
    }

    /** {worldNumber, levelIndex} of the first unlocked level not yet completed. */
    public Optional<int[]> nextUnfinished(SaveData d) {
        for (World w : worlds) {
            for (int k = 0; k < w.levels().size(); k++) {
                if (isUnlocked(w, k, d) && !d.completed().contains(w.levels().get(k).id())) {
                    return Optional.of(new int[] { w.number(), k });
                }
            }
        }
        return Optional.empty();
    }

    public LevelCatalog catalog(World w) {
        return LevelCatalog.files(w.levels().stream().map(LevelRef::path).toList(),
                w.levels().stream().map(LevelRef::id).toList());
    }

    /** The world a catalog was made from, matched by its level ids. */
    public Optional<World> worldOf(LevelCatalog c) {
        for (World w : worlds) {
            if (w.levels().size() != c.size() || w.levels().isEmpty()) continue;
            boolean same = true;
            for (int k = 0; k < c.size() && same; k++) same = w.levels().get(k).id().equals(c.id(k));
            if (same) return Optional.of(w);
        }
        return Optional.empty();
    }
}
