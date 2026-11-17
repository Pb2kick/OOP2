package ambot.level;

import java.awt.image.BufferedImage;

import ambot.core.GameConfig;

/** A tileset image cut into 16x16 tiles, numbered from {@code firstGid} as in Tiled. */
public final class Tileset {
    private static final int T = GameConfig.TILE;

    private final int firstGid, tileCount, columns;
    private final BufferedImage[] tiles;

    public Tileset(int firstGid, int tileCount, int columns, BufferedImage image) {
        if (firstGid < 1 || tileCount < 1 || columns < 1) {
            throw new IllegalArgumentException("bad tileset numbers: firstgid " + firstGid + ", tilecount " + tileCount
                    + ", columns " + columns);
        }
        int rows = (tileCount + columns - 1) / columns;
        if (image.getWidth() < columns * T || image.getHeight() < rows * T) {
            throw new IllegalArgumentException("tileset image " + image.getWidth() + "x" + image.getHeight()
                    + " is too small for " + tileCount + " tiles in " + columns + " columns");
        }
        this.firstGid = firstGid;
        this.tileCount = tileCount;
        this.columns = columns;
        this.tiles = new BufferedImage[tileCount];
        for (int i = 0; i < tileCount; i++) {
            tiles[i] = image.getSubimage((i % columns) * T, (i / columns) * T, T, T);
        }
    }

    public boolean contains(int gid) {
        return gid >= firstGid && gid < firstGid + tileCount;
    }

    public BufferedImage tile(int gid) {
        if (!contains(gid)) throw new IllegalArgumentException("tile id " + gid + " is not in this tileset");
        return tiles[gid - firstGid];
    }

    public int firstGid() {
        return firstGid;
    }

    public int tileCount() {
        return tileCount;
    }

    public int columns() {
        return columns;
    }
}
