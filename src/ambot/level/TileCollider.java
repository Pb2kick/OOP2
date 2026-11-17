package ambot.level;

import ambot.core.GameConfig;

/**
 * Moves a box through a tile map one axis at a time. A blocked move snaps the box
 * flush against the tile it hit. Moves are limited to one tile per call so nothing
 * can tunnel through a wall.
 */
public final class TileCollider {
    private static final int T = GameConfig.TILE;
    private static final double EPS = 1e-9;

    private TileCollider() {
    }

    /** The resulting position on the moved axis, and whether a tile stopped the move. */
    public record Move(double position, boolean blocked) {
    }

    public static Move moveX(Rect box, double dx, TileMap map) {
        checkStep(dx);
        if (dx == 0) return new Move(box.x(), false);
        int ty0 = floor(box.y() / T);
        int ty1 = floor((box.bottom() - EPS) / T);
        double newX = box.x() + dx;
        if (dx > 0) {
            int colOld = floor((box.right() - EPS) / T);
            int colNew = floor((newX + box.w() - EPS) / T);
            for (int col = colOld + 1; col <= colNew; col++) {
                if (anySolidInColumn(map, col, ty0, ty1)) return new Move(col * T - box.w(), true);
            }
        } else {
            int colOld = floor(box.x() / T);
            int colNew = floor(newX / T);
            for (int col = colOld - 1; col >= colNew; col--) {
                if (anySolidInColumn(map, col, ty0, ty1)) return new Move((col + 1) * T, true);
            }
        }
        return new Move(newX, false);
    }

    public static Move moveY(Rect box, double dy, TileMap map) {
        checkStep(dy);
        if (dy == 0) return new Move(box.y(), false);
        int tx0 = floor(box.x() / T);
        int tx1 = floor((box.right() - EPS) / T);
        double newY = box.y() + dy;
        if (dy > 0) {
            int rowOld = floor((box.bottom() - EPS) / T);
            int rowNew = floor((newY + box.h() - EPS) / T);
            for (int row = rowOld + 1; row <= rowNew; row++) {
                if (anySolidInRow(map, row, tx0, tx1)) return new Move(row * T - box.h(), true);
            }
        } else {
            int rowOld = floor(box.y() / T);
            int rowNew = floor(newY / T);
            for (int row = rowOld - 1; row >= rowNew; row--) {
                if (anySolidInRow(map, row, tx0, tx1)) return new Move((row + 1) * T, true);
            }
        }
        return new Move(newY, false);
    }

    /** True if there is a solid tile just below the box. */
    public static boolean onGround(Rect box, TileMap map) {
        int row = floor((box.bottom() + 0.01) / T);
        return anySolidInRow(map, row, floor(box.x() / T), floor((box.right() - EPS) / T));
    }

    private static boolean anySolidInRow(TileMap map, int row, int tx0, int tx1) {
        for (int tx = tx0; tx <= tx1; tx++) {
            if (map.isSolid(tx, row)) return true;
        }
        return false;
    }

    private static boolean anySolidInColumn(TileMap map, int col, int ty0, int ty1) {
        for (int ty = ty0; ty <= ty1; ty++) {
            if (map.isSolid(col, ty)) return true;
        }
        return false;
    }

    private static void checkStep(double d) {
        if (Math.abs(d) > T) {
            throw new IllegalArgumentException("move of " + d + "px exceeds one tile (" + T + "px)");
        }
    }

    private static int floor(double v) {
        return (int) Math.floor(v);
    }
}
