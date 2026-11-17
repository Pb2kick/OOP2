package ambot.level;

import ambot.core.GameConfig;

/** A grid of 16x16 tiles. Tile 0 is empty, 1 is solid. */
public final class TileMap {
    public static final int EMPTY = 0, SOLID = 1;

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

    /** '#' is solid; '.' and 'P' (a spawn marker) are empty. */
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
