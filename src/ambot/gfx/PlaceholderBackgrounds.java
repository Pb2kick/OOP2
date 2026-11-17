package ambot.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import ambot.core.GameConfig;

/**
 * Procedurally drawn hills used until each world's background art arrives in M4
 * (spec section 5.10). Every world currently gets the same two layers.
 */
public final class PlaceholderBackgrounds {
    static final Color SKY = new Color(0x29366F);
    static final Color FAR = new Color(0x5F574F);
    static final Color NEAR = new Color(0x008751);
    private static final int WIDTH = 320;

    private PlaceholderBackgrounds() {
    }

    public static ParallaxBackground forWorld(String world) {
        int h = GameConfig.LOGICAL_HEIGHT;
        BufferedImage far = hills(160, FAR, 60, 22, 2, 14, 5, 0.7);
        BufferedImage near = hills(110, NEAR, 40, 16, 3, 9, 7, 2.1);
        return new ParallaxBackground(SKY, List.of(
                new ParallaxLayer(far, 0.2, h - far.getHeight()),
                new ParallaxLayer(near, 0.5, h - near.getHeight())));
    }

    /**
     * A strip of hills whose top edge is a sum of two sine waves. The frequencies are whole
     * numbers of waves per image width, so copies placed side by side join seamlessly.
     */
    private static BufferedImage hills(int height, Color color, int base, int amp1, int waves1, int amp2, int waves2,
            double phase) {
        BufferedImage img = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        for (int x = 0; x < WIDTH; x++) {
            double t = 2 * Math.PI * x / WIDTH;
            double crest = base + amp1 * Math.sin(waves1 * t) + amp2 * Math.sin(waves2 * t + phase);
            int top = Math.max(0, (int) Math.round(height - crest - amp1 - amp2));
            g.fillRect(x, top, 1, height - top);
        }
        g.dispose();
        return img;
    }
}
