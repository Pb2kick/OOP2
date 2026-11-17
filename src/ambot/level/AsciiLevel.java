package ambot.level;

import ambot.core.GameConfig;

/** A level written as text rows; exactly one 'P' marks the player spawn tile. Used until M2 adds Tiled maps. */
public record AsciiLevel(TileMap map, double spawnX, double spawnY) {

    public static AsciiLevel parse(String... rows) {
        TileMap map = TileMap.fromAscii(rows);
        int found = 0, sx = 0, sy = 0;
        for (int ty = 0; ty < rows.length; ty++) {
            for (int tx = 0; tx < rows[ty].length(); tx++) {
                if (rows[ty].charAt(tx) == 'P') {
                    found++;
                    sx = tx;
                    sy = ty;
                }
            }
        }
        if (found != 1) {
            throw new IllegalArgumentException("a level needs exactly one 'P' spawn, found " + found);
        }
        return new AsciiLevel(map, sx * GameConfig.TILE, sy * GameConfig.TILE);
    }
}
