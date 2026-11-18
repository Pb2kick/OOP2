package ambot.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import ambot.entities.Damageable;

class HudTest {
    static final int RED = 0xFF004D;

    /** A stand-in with fixed health. */
    static Damageable fixed(int health, int max) {
        return new Damageable() {
            @Override public int health() { return health; }
            @Override public int maxHealth() { return max; }
            @Override public boolean isDead() { return health <= 0; }
            @Override public boolean damage(int amount, double fromX) { return false; }
            @Override public void heal(int amount) { }
        };
    }

    @Test
    void drawsFullThenEmptyHearts() {
        BufferedImage img = new BufferedImage(640, 360, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Hud.render(g, fixed(2, 3));
        g.dispose();
        int cy = 8 + 3;
        assertEquals(RED, img.getRGB(8 + 4, cy) & 0xFFFFFF, "heart 1 full");
        assertEquals(RED, img.getRGB(8 + 12 + 4, cy) & 0xFFFFFF, "heart 2 full");
        assertNotEquals(RED, img.getRGB(8 + 24 + 4, cy) & 0xFFFFFF, "heart 3 empty");
        assertNotEquals(0, img.getRGB(8 + 24 + 4, cy) >>> 24, "an empty heart is still drawn");
    }
}
