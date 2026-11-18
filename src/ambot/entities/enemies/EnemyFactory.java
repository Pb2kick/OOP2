package ambot.entities.enemies;

import java.util.Set;

import ambot.level.LevelObject;
import ambot.level.Rect;
import ambot.level.Spot;

/** Builds enemies from a level's {@code enemy} objects (property {@code type}, optional {@code drop}). */
public final class EnemyFactory {
    public static final Set<String> KNOWN_TYPES = Set.of("walker", "shooter");

    private EnemyFactory() {
    }

    /** The enemy stands with its bottom-centre on the object's bottom-centre. */
    public static Enemy create(LevelObject o) {
        String type = o.property("type");
        Rect body = bodyAt(type, feet(o.bounds()));
        String drop = o.property("drop");
        return switch (type) {
            case "walker" -> new Walker(body.x(), body.y(), drop);
            case "shooter" -> throw new UnsupportedOperationException("shooter is not built yet");
            default -> throw new IllegalArgumentException("unknown enemy type '" + type + "'");
        };
    }

    /** The hitbox an enemy of this type would have standing on {@code feet}. */
    public static Rect bodyAt(String type, Spot feet) {
        double w, h;
        switch (type) {
            case "walker" -> {
                w = Walker.W;
                h = Walker.H;
            }
            case "shooter" -> {
                w = 14;
                h = 22;
            }
            default -> throw new IllegalArgumentException("unknown enemy type '" + type + "'");
        }
        return new Rect(feet.x() - w / 2, feet.y() - h, w, h);
    }

    private static Spot feet(Rect b) {
        return new Spot(b.x() + b.w() / 2, b.bottom());
    }
}
