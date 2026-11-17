package ambot.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Draws the flat-coloured placeholder tileset used until real art arrives in M4
 * (spec section 5.10). Not shipped: run it to (re)write resources/tiles/placeholder.png.
 */
public final class PlaceholderTiles {
    public static final int GROUND = 1, GROUND_TOP = 2, ONEWAY = 3, SPIKES = 4, BACK_WALL = 5, GRASS = 6;
    public static final int TILE_COUNT = 6;
    public static final String OUTPUT = "resources/tiles/placeholder.png";
    private static final int T = 16;

    private PlaceholderTiles() {
    }

    /** The whole tileset, one row of 16x16 tiles in gid order. */
    public static BufferedImage draw() {
        BufferedImage img = new BufferedImage(TILE_COUNT * T, T, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        ground(g, x(GROUND));

        ground(g, x(GROUND_TOP));
        g.setColor(new Color(0x00E436));
        g.fillRect(x(GROUND_TOP), 0, T, 3);
        g.setColor(new Color(0x008751));
        g.fillRect(x(GROUND_TOP), 3, T, 1);

        g.setColor(new Color(0xAB5236));
        g.fillRect(x(ONEWAY), 0, T, 4);
        g.setColor(new Color(0x7E2553));
        g.fillRect(x(ONEWAY), 3, T, 1);

        g.setColor(new Color(0xC2C3C7));
        for (int i = 0; i < 3; i++) {
            int bx = x(SPIKES) + i * 5;
            g.fillPolygon(new int[] { bx, bx + 3, bx + 6 }, new int[] { T, 7, T }, 3);
        }

        int wx = x(BACK_WALL);
        g.setColor(new Color(0x1D2B53));
        g.fillRect(wx, 0, T, T);
        g.setColor(new Color(0x29366F));
        g.fillRect(wx, 7, T, 1);
        g.fillRect(wx, 15, T, 1);
        g.fillRect(wx + 5, 0, 1, 7);
        g.fillRect(wx + 12, 8, 1, 7);

        int gx = x(GRASS);
        g.setColor(new Color(0x008751));
        g.fillPolygon(new int[] { gx + 2, gx + 4, gx + 5 }, new int[] { T, 9, T }, 3);
        g.fillPolygon(new int[] { gx + 6, gx + 8, gx + 10 }, new int[] { T, 6, T }, 3);
        g.fillPolygon(new int[] { gx + 10, gx + 13, gx + 14 }, new int[] { T, 10, T }, 3);

        g.dispose();
        return img;
    }

    private static int x(int gid) {
        return (gid - 1) * T;
    }

    private static void ground(Graphics2D g, int x) {
        g.setColor(new Color(0x5F574F));
        g.fillRect(x, 0, T, T);
        g.setColor(new Color(0x4A443E));
        int[][] specks = { { 3, 5 }, { 10, 2 }, { 6, 11 }, { 13, 9 }, { 1, 14 }, { 9, 13 } };
        for (int[] s : specks) g.fillRect(x + s[0], s[1], 2, 1);
    }

    public static void main(String[] args) throws IOException {
        File out = new File(args.length > 0 ? args[0] : OUTPUT);
        out.getAbsoluteFile().getParentFile().mkdirs();
        ImageIO.write(draw(), "png", out);
        System.out.println("wrote " + out);
    }
}
