package ambot.level;

import ambot.core.GameConfig;

/** A grid of 16x16 collision tiles: empty, solid, one-way platform or hazard. */
public final class TileMap {
    public static final int EMPTY = 0, SOLID = 1, ONEWAY = 2, HAZARD = 3;
    /** Hazards forgive this many pixels of overlap on every side of a box. */
    public static final int HAZARD_INSET = 2;
    private static final double EPS = 1e-9;

    private final int widthTiles, heightTiles;
    private final int[] tiles;

    public TileMap(int widthTiles, int heightTiles, int[] tiles) {
        if (widthTiles <= 0 || heightTiles <= 0 || tiles.length != widthTiles * heightTiles) {
            throw new IllegalArgumentException("tile array does not match " + widthTiles + "x" + heightTiles);
        }
        this.widthTiles = widthTiles;
        this.heightTiles = heightTiles;
        this.tiles = tiles.clone();
    }

    /** '#' is solid, '-' one-way, '^' hazard; '.' and 'P' (a spawn marker) are empty. */
    public static TileMap fromAscii(String... rows) {
        if (rows.length == 0 || rows[0].isEmpty()) {
            throw new IllegalArgumentException("empty map");
        }
        int w = rows[0].length();
        int[] t = new int[w * rows.length];
        for (int ty = 0; ty < rows.length; ty++) {
            if (rows[ty].length() != w) {
                throw new IllegalArgumentException("row " + ty + " has length " + rows[ty].length() + ", expected " + w);
            }
            for (int tx = 0; tx < w; tx++) {
                char c = rows[ty].charAt(tx);
                t[ty * w + tx] = switch (c) {
                    case '#' -> SOLID;
                    case '-' -> ONEWAY;
                    case '^' -> HAZARD;
                    case '.', 'P' -> EMPTY;
                    default -> throw new IllegalArgumentException("unknown map char '" + c + "' at " + tx + "," + ty);
                };
            }
        }
        return new TileMap(w, rows.length, t);
    }

    /** Off the left or right edge counts as wall; above or below the map is open. */
    public boolean isSolid(int tx, int ty) {
        if (tx < 0 || tx >= widthTiles) return true;
        if (ty < 0 || ty >= heightTiles) return false;
        return tiles[ty * widthTiles + tx] == SOLID;
    }

    public boolean isOneWay(int tx, int ty) {
        return tileAt(tx, ty) == ONEWAY;
    }

    public boolean isHazard(int tx, int ty) {
        return tileAt(tx, ty) == HAZARD;
    }

    /** True if any tile of the given kind lies under the box. */
    public boolean overlaps(Rect box, int kind) {
        if (box.w() <= 0 || box.h() <= 0) return false;
        int T = GameConfig.TILE;
        int tx0 = (int) Math.floor(box.x() / T), tx1 = (int) Math.floor((box.right() - EPS) / T);
        int ty0 = (int) Math.floor(box.y() / T), ty1 = (int) Math.floor((box.bottom() - EPS) / T);
        for (int ty = ty0; ty <= ty1; ty++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                if (tileAt(tx, ty) == kind) return true;
            }
        }
        return false;
    }

    /** True if the box, shrunk by {@link #HAZARD_INSET} on every side, overlaps a hazard tile. */
    public boolean touchesHazard(Rect box) {
        int i = HAZARD_INSET;
        return overlaps(new Rect(box.x() + i, box.y() + i, box.w() - 2 * i, box.h() - 2 * i), HAZARD);
    }

    public int tileAt(int tx, int ty) {
        if (tx < 0 || tx >= widthTiles || ty < 0 || ty >= heightTiles) return EMPTY;
        return tiles[ty * widthTiles + tx];
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
}
