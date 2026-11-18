package ambot.level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ambot.entities.enemies.EnemyFactory;
import ambot.entities.player.PlayerTuning;

/**
 * Checks a loaded level for mistakes (spec section 5.6): spawn and exit counts, unknown
 * object, enemy, pickup and tile types, spawns or checkpoints inside walls or on hazards,
 * objects outside the map, and boss arenas.
 */
public final class LevelValidator {
    /** Enemy types the game can spawn. M3 adds the World 1 enemies. */
    public static final Set<String> KNOWN_ENEMY_TYPES = EnemyFactory.KNOWN_TYPES;
    public static final Set<String> KNOWN_PICKUP_TYPES = Set.of("heart");
    private static final Set<String> KNOWN_OBJECT_TYPES = Set.of(LevelObject.PLAYER_SPAWN, LevelObject.CHECKPOINT,
            LevelObject.EXIT, LevelObject.ENEMY, LevelObject.PICKUP, LevelObject.BOSS_ARENA);

    private LevelValidator() {
    }

    /** Every problem found, each naming a position; an empty list means the level is valid. */
    public static List<String> check(Level level) {
        List<String> problems = new ArrayList<>();
        TileMap map = level.collision();

        count(level, LevelObject.PLAYER_SPAWN, problems);
        count(level, LevelObject.EXIT, problems);

        for (LevelObject o : level.objects()) {
            String at = at(o.bounds());
            if (!KNOWN_OBJECT_TYPES.contains(o.type())) {
                problems.add("unknown object type '" + o.type() + "' at " + at);
                continue;
            }
            if (outside(o.bounds(), level)) problems.add(o.type() + " at " + at + " is outside the map");
            switch (o.type()) {
                case LevelObject.ENEMY -> {
                    knownType(o, KNOWN_ENEMY_TYPES, problems);
                    String type = o.property("type");
                    if (type != null && KNOWN_ENEMY_TYPES.contains(type)
                            && map.overlaps(EnemyFactory.bodyAt(type, feet(o.bounds())), TileMap.SOLID)) {
                        problems.add("enemy at " + at + " overlaps solid tiles");
                    }
                    String drop = o.property("drop");
                    if (drop != null && !drop.equals("heart")) problems.add("unknown drop '" + drop + "' at " + at);
                }
                case LevelObject.PICKUP -> knownType(o, KNOWN_PICKUP_TYPES, problems);
                case LevelObject.CHECKPOINT -> {
                    // the player touches the flag's box, then respawns standing on its bottom-centre
                    needsSize(o, at, problems);
                    notDangerous(o.type(), at, respawnBody(o.bounds()), map, problems);
                }
                case LevelObject.PLAYER_SPAWN -> notDangerous(o.type(), at, respawnBody(o.bounds()), map, problems);
                case LevelObject.EXIT -> needsSize(o, at, problems);
                default -> {
                }
            }
        }

        boolean hasArena = !level.objects(LevelObject.BOSS_ARENA).isEmpty();
        if (level.isBossLevel() && !hasArena) problems.add("boss level has no boss_arena");
        if (!level.isBossLevel()) {
            for (LevelObject a : level.objects(LevelObject.BOSS_ARENA)) {
                problems.add("boss_arena at " + at(a.bounds())
                        + " is in a level that is not a boss level (set map property boss=true)");
            }
        }

        for (TileLayer layer : level.layers()) {
            for (int ty = 0; ty < layer.height(); ty++) {
                for (int tx = 0; tx < layer.width(); tx++) {
                    int gid = layer.gid(tx, ty);
                    if (gid != 0 && !level.tileset().contains(gid)) {
                        problems.add("unknown tile id " + gid + " in layer '" + layer.name() + "' at tile (" + tx
                                + "," + ty + ")");
                    }
                }
            }
        }
        return problems;
    }

    private static void count(Level level, String type, List<String> problems) {
        int n = level.objects(type).size();
        if (n != 1) problems.add("expected exactly 1 " + type + ", found " + n);
    }

    private static void knownType(LevelObject o, Set<String> known, List<String> problems) {
        String type = o.property("type");
        if (type == null || type.isBlank()) {
            problems.add(o.type() + " at " + at(o.bounds()) + " has no 'type' property");
        } else if (!known.contains(type)) {
            problems.add("unknown " + o.type() + " type '" + type + "' at " + at(o.bounds()));
        }
    }

    private static Spot feet(Rect b) {
        return new Spot(b.x() + b.w() / 2, b.bottom());
    }

    /** The player's 14x28 body standing on the bottom-centre of a box (a point for spawns). */
    private static Rect respawnBody(Rect b) {
        double feetX = b.x() + b.w() / 2, feetY = b.bottom();
        return new Rect(feetX - PlayerTuning.HITBOX_W / 2.0, feetY - PlayerTuning.HITBOX_H,
                PlayerTuning.HITBOX_W, PlayerTuning.HITBOX_H);
    }

    /** Checkpoints and exits are touched by overlap, so a point (0x0) one could never be reached. */
    private static void needsSize(LevelObject o, String at, List<String> problems) {
        if (o.bounds().w() <= 0 || o.bounds().h() <= 0) {
            problems.add(o.type() + " at " + at + " has no size; draw it as a rectangle, not a point");
        }
    }

    private static void notDangerous(String what, String at, Rect box, TileMap map, List<String> problems) {
        if (map.overlaps(box, TileMap.SOLID)) problems.add(what + " at " + at + " overlaps solid tiles");
        if (map.touchesHazard(box)) problems.add(what + " at " + at + " overlaps hazard tiles");
    }

    private static boolean outside(Rect b, Level level) {
        return b.x() < 0 || b.y() < 0 || b.right() > level.widthPx() || b.bottom() > level.heightPx();
    }

    private static String at(Rect b) {
        return "(" + (int) b.x() + "," + (int) b.y() + ")";
    }
}
