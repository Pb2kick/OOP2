package ambot.level;

/** One tile layer of a level: a grid of tile ids (gids), 0 meaning no tile. */
public final class TileLayer {
    private final String name;
    private final int width, height;
    private final int[] gids;

    public TileLayer(String name, int width, int height, int[] gids) {
        if (width <= 0 || height <= 0 || gids.length != width * height) {
            throw new IllegalArgumentException("layer '" + name + "' has " + gids.length + " tiles, expected "
                    + width + "x" + height);
        }
        this.name = name;
        this.width = width;
        this.height = height;
        this.gids = gids.clone();
    }

    public String name() {
        return name;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** The tile id at a tile position, or 0 outside the layer. */
    public int gid(int tx, int ty) {
        if (tx < 0 || tx >= width || ty < 0 || ty >= height) return 0;
        return gids[ty * width + tx];
    }
}
