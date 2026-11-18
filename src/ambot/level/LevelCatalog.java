package ambot.level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** An ordered list of levels played one after another, each with an id for saving progress. */
public final class LevelCatalog {
    /** The M2/M3 placeholder levels, in play order. */
    public static final List<String> PLACEHOLDER_PATHS = List.of(
            "/levels/placeholder/p1.tmx",
            "/levels/placeholder/p2.tmx",
            "/levels/placeholder/p3.tmx",
            "/levels/placeholder/p4.tmx");

    private final List<Supplier<Level>> levels;
    private final List<String> ids;

    private LevelCatalog(List<Supplier<Level>> levels, List<String> ids) {
        if (levels.isEmpty()) throw new IllegalArgumentException("a catalog needs at least one level");
        if (ids.size() != levels.size()) throw new IllegalArgumentException("one id per level");
        this.levels = List.copyOf(levels);
        this.ids = List.copyOf(ids);
    }

    public static LevelCatalog placeholder() {
        return files(PLACEHOLDER_PATHS);
    }

    /** Levels loaded from the classpath and validated each time; ids are the file names without ".tmx". */
    public static LevelCatalog files(List<String> resourcePaths) {
        List<String> ids = new ArrayList<>();
        for (String p : resourcePaths) {
            String name = p.substring(p.lastIndexOf('/') + 1);
            ids.add(name.endsWith(".tmx") ? name.substring(0, name.length() - 4) : name);
        }
        return files(resourcePaths, ids);
    }

    public static LevelCatalog files(List<String> resourcePaths, List<String> ids) {
        List<Supplier<Level>> list = new ArrayList<>();
        for (String path : resourcePaths) list.add(() -> loadValidated(path));
        return new LevelCatalog(list, ids);
    }

    /** Levels already in memory (tests), returned as given, with ids "level-1", "level-2", ... */
    public static LevelCatalog of(Level... levels) {
        List<Supplier<Level>> list = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (Level l : levels) {
            list.add(() -> l);
            ids.add("level-" + (ids.size() + 1));
        }
        return new LevelCatalog(list, ids);
    }

    public int size() {
        return levels.size();
    }

    public Level load(int index) {
        return levels.get(index).get();
    }

    public String id(int index) {
        return ids.get(index);
    }

    private static Level loadValidated(String path) {
        Level level = TmxLoader.load(path);
        List<String> problems = LevelValidator.check(level);
        if (!problems.isEmpty()) throw new LevelFormatException(path + ": " + String.join("; ", problems));
        return level;
    }
}
