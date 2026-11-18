package ambot.ui;

import java.awt.Color;
import java.awt.Graphics2D;

/** A 9x8 pixel heart: red when full, dark grey when empty. Used by the HUD and heart pickups. */
public final class HeartIcon {
    public static final int W = 9, H = 8;
    static final Color FULL = new Color(0xFF004D);
    static final Color EMPTY = new Color(0x5F574F);
    /** Filled spans of each row, top to bottom, as pairs of (first, last) columns. */
    private static final int[][] ROWS = {
            { 1, 3, 5, 7 }, { 0, 8 }, { 0, 8 }, { 0, 8 }, { 1, 7 }, { 2, 6 }, { 3, 5 }, { 4, 4 } };

    private HeartIcon() {
    }

    public static void draw(Graphics2D g, int x, int y, boolean full) {
        g.setColor(full ? FULL : EMPTY);
        for (int r = 0; r < ROWS.length; r++) {
            for (int i = 0; i < ROWS[r].length; i += 2) {
                g.fillRect(x + ROWS[r][i], y + r, ROWS[r][i + 1] - ROWS[r][i] + 1, 1);
            }
        }
    }
}
