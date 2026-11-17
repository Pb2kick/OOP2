package ambot.level;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import ambot.tools.LevelPreview;

class LevelRendererTest {
    static final int SKY = 0x29366F;

    static final Level LEVEL = TestLevels.ascii(
            "..........",
            "..........",
            ".P.C...E..",
            "##########");

    @Test
    void previewIsLevelSized() {
        BufferedImage img = LevelPreview.render(LEVEL);
        assertEquals(160, img.getWidth());
        assertEquals(64, img.getHeight());
    }

    @Test
    void solidTileIsDrawn() {
        BufferedImage img = LevelPreview.render(LEVEL);
        assertNotEquals(SKY, img.getRGB(2 * 16 + 8, 3 * 16 + 8) & 0xFFFFFF);
        assertEquals(SKY, img.getRGB(5 * 16 + 8, 0 * 16 + 8) & 0xFFFFFF, "empty tiles show the sky");
    }

    @Test
    void drawsViewLargerThanMapWithoutError() {
        BufferedImage img = new BufferedImage(160, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        LevelRenderer r = new LevelRenderer(LEVEL);
        Rect huge = new Rect(-200, -200, 1000, 1000);
        assertDoesNotThrow(() -> {
            r.drawBehind(g, huge);
            r.drawFront(g, huge);
            r.drawBehind(g, new Rect(5000, 5000, 640, 360));
        });
        g.dispose();
    }

    @Test
    void activeCheckpointLooksDifferent() {
        Rect cp = LEVEL.checkpoints().get(0);
        int fx = (int) cp.x() + 8, fy = (int) cp.y() + 5;
        assertNotEquals(markerPixel(null, fx, fy), markerPixel(cp, fx, fy));
    }

    private static int markerPixel(Rect active, int x, int y) {
        BufferedImage img = new BufferedImage(160, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        new LevelRenderer(LEVEL).drawMarkers(g, active);
        g.dispose();
        return img.getRGB(x, y);
    }
}
