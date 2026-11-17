package ambot.gfx;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

/** A sprite sheet cut into equal cells, addressed by (column, row). */
public final class SpriteSheet {
    private final BufferedImage image;
    private final int cellW, cellH, columns, rows;
    private final BufferedImage[][] cache;

    public SpriteSheet(BufferedImage image, int cellW, int cellH) {
        this.image = image;
        this.cellW = cellW;
        this.cellH = cellH;
        this.columns = image.getWidth() / cellW;
        this.rows = image.getHeight() / cellH;
        this.cache = new BufferedImage[rows][columns];
    }

    /** Loads a sheet from the classpath, failing with the path in the message if it is missing. */
    public static SpriteSheet load(String resourcePath, int cellW, int cellH) {
        try (InputStream in = SpriteSheet.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + resourcePath);
            }
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                throw new IllegalStateException("Not a readable image: " + resourcePath);
            }
            return new SpriteSheet(img, cellW, cellH);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + resourcePath, e);
        }
    }

    public BufferedImage frame(int col, int row) {
        if (col < 0 || col >= columns || row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException("cell (" + col + "," + row + ") outside " + columns + "x" + rows);
        }
        BufferedImage f = cache[row][col];
        if (f == null) {
            f = image.getSubimage(col * cellW, row * cellH, cellW, cellH);
            cache[row][col] = f;
        }
        return f;
    }

    /** True if the cell has no visible pixel (alpha above 20). */
    public boolean isEmpty(int col, int row) {
        BufferedImage f = frame(col, row);
        for (int y = 0; y < f.getHeight(); y++)
            for (int x = 0; x < f.getWidth(); x++)
                if ((f.getRGB(x, y) >>> 24) > 20) return false;
        return true;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }
}
