package ambot.level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** An ordered list of levels played one after another. */
public final class LevelCatalog {
    /** The M2 placeholder levels, in play order. */
    public static final List<String> PLACEHOLDER_PATHS = List.of(
            "/levels/placeholder/p1.tmx",
            "/levels/placeholder/p2.tmx",
            "/levels/placeholder/p3.tmx",
            "/levels/placeholder/p4.tmx");

    private final List<Supplier<Level>> levels;

    private LevelCatalog(List<Supplier<Level>> levels) {
        if (levels.isEmpty()) throw new IllegalArgumentException("a catalog needs at least one level");
        this.levels = List.copyOf(levels);
    }

    public static LevelCatalog placeholder() {
        return files(PLACEHOLDER_PATHS);
    }

    /** Levels loaded from the classpath and validated each time they're loaded. */
    public static LevelCatalog files(List<String> resourcePaths) {
        List<Supplier<Level>> list = new ArrayList<>();
        for (String path : resourcePaths) list.add(() -> loadValidated(path));
        return new LevelCatalog(list);
    }

    /** Levels already in memory (tests), returned as given. */
    public static LevelCatalog of(Level... levels) {
        List<Supplier<Level>> list = new ArrayList<>();
        for (Level l : levels) list.add(() -> l);
        return new LevelCatalog(list);
    }

    public int size() {
        return levels.size();
    }

    public Level load(int index) {
        return levels.get(index).get();
    }

    private static Level loadValidated(String path) {
        Level level = TmxLoader.load(path);
        List<String> problems = LevelValidator.check(level);
        if (!problems.isEmpty()) throw new LevelFormatException(path + ": " + String.join("; ", problems));
        return level;
    }
}
