package ambot.level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ambot.core.GameConfig;

/**
 * A loaded level (spec section 5.6): its tile layers, tileset, objects and the collision
 * grid built from the {@code solid}, {@code oneway} and {@code hazard} layers.
 */
public final class Level {
    public static final String BACKGROUND = "background";
    public static final String SOLID = "solid";
    public static final String ONEWAY = "oneway";
    public static final String HAZARD = "hazard";
    public static final String FOREGROUND = "foreground";
    public static final String ENTITIES = "entities";
    /** Every allowed tile layer, in drawing order. */
    public static final List<String> TILE_LAYERS = List.of(BACKGROUND, SOLID, ONEWAY, HAZARD, FOREGROUND);

    private final String name;
    private final Map<String, String> properties;
    private final Tileset tileset;
    private final int widthTiles, heightTiles;
    private final Map<String, TileLayer> layers = new LinkedHashMap<>();
    private final List<LevelObject> objects;
    private final TileMap collision;

    public Level(String name, Map<String, String> properties, Tileset tileset, int widthTiles, int heightTiles,
            List<TileLayer> layers, List<LevelObject> objects) {
        if (widthTiles <= 0 || heightTiles <= 0) {
            throw new IllegalArgumentException("level size must be positive, got " + widthTiles + "x" + heightTiles);
        }
        for (TileLayer l : layers) {
            if (l.width() != widthTiles || l.height() != heightTiles) {
                throw new IllegalArgumentException("layer '" + l.name() + "' is " + l.width() + "x" + l.height()
                        + ", but the map is " + widthTiles + "x" + heightTiles);
            }
            if (this.layers.put(l.name(), l) != null) {
                throw new IllegalArgumentException("duplicate layer '" + l.name() + "'");
            }
        }
        this.name = name;
        this.properties = Map.copyOf(properties);
        this.tileset = tileset;
        this.widthTiles = widthTiles;
        this.heightTiles = heightTiles;
        this.objects = List.copyOf(objects);
        this.collision = buildCollision();
    }

    private TileMap buildCollision() {
        int[] kinds = new int[widthTiles * heightTiles];
        for (int ty = 0; ty < heightTiles; ty++) {
            for (int tx = 0; tx < widthTiles; tx++) {
                int kind = TileMap.EMPTY;
                if (has(SOLID, tx, ty)) kind = TileMap.SOLID;
                else if (has(ONEWAY, tx, ty)) kind = TileMap.ONEWAY;
                else if (has(HAZARD, tx, ty)) kind = TileMap.HAZARD;
                kinds[ty * widthTiles + tx] = kind;
            }
        }
        return new TileMap(widthTiles, heightTiles, kinds);
    }

    private boolean has(String layer, int tx, int ty) {
        TileLayer l = layers.get(layer);
        return l != null && l.gid(tx, ty) != 0;
    }

    public String name() {
        return name;
    }

    public Map<String, String> properties() {
        return properties;
    }

    /** The {@code title} property, or the file name. */
    public String title() {
        return properties.getOrDefault("title", name);
    }

    /** The {@code world} property, or {@code "placeholder"}. */
    public String world() {
        return properties.getOrDefault("world", "placeholder");
    }

    public boolean isBossLevel() {
        return "true".equalsIgnoreCase(properties.get("boss"));
    }

    public Tileset tileset() {
        return tileset;
    }

    public TileMap collision() {
        return collision;
    }

    public Optional<TileLayer> layer(String layerName) {
        return Optional.ofNullable(layers.get(layerName));
    }

    public List<TileLayer> layers() {
        return List.copyOf(layers.values());
    }

    public int widthTiles() {
        return widthTiles;
    }

    public int heightTiles() {
        return heightTiles;
    }

    public int widthPx() {
        return widthTiles * GameConfig.TILE;
    }

    public int heightPx() {
        return heightTiles * GameConfig.TILE;
    }

    public List<LevelObject> objects() {
        return objects;
    }

    public List<LevelObject> objects(String type) {
        List<LevelObject> out = new ArrayList<>();
        for (LevelObject o : objects) {
            if (o.type().equals(type)) out.add(o);
        }
        return out;
    }

    /** Where the player stands at the start: the bottom-centre of the first {@code player_spawn}. */
    public Spot spawn() {
        Rect b = first(LevelObject.PLAYER_SPAWN).bounds();
        return new Spot(b.x() + b.w() / 2, b.bottom());
    }

    public Rect exit() {
        return first(LevelObject.EXIT).bounds();
    }

    public List<Rect> checkpoints() {
        return objects(LevelObject.CHECKPOINT).stream().map(LevelObject::bounds).toList();
    }

    public Optional<Rect> bossArena() {
        return objects(LevelObject.BOSS_ARENA).stream().map(LevelObject::bounds).findFirst();
    }

    private LevelObject first(String type) {
        for (LevelObject o : objects) {
            if (o.type().equals(type)) return o;
        }
        throw new IllegalStateException("level '" + name + "' has no " + type);
    }
}
