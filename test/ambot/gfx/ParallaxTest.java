package ambot.gfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

class ParallaxTest {

    @Test
    void wrapOffsetStaysInRange() {
        assertEquals(0, ParallaxBackground.wrapOffset(0, 0.5, 320));
        assertEquals(-50, ParallaxBackground.wrapOffset(100, 0.5, 320));
        assertEquals(-30, ParallaxBackground.wrapOffset(700, 0.5, 320));
        assertEquals(-240, ParallaxBackground.wrapOffset(-160, 0.5, 320));
    }

    @Test
    void placeholderHasTwoSlowerLayers() {
        List<ParallaxLayer> layers = PlaceholderBackgrounds.forWorld("placeholder").layers();
        assertEquals(2, layers.size());
        double previous = 0;
        for (ParallaxLayer l : layers) {
            assertTrue(l.factor() > previous && l.factor() < 1, "factor " + l.factor());
            previous = l.factor();
        }
    }

    @Test
    void unknownWorldFallsBack() {
        assertEquals(2, PlaceholderBackgrounds.forWorld("no-such-world").layers().size());
    }

    @Test
    void renderCoversWholeCanvas() {
        BufferedImage canvas = new BufferedImage(640, 360, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        PlaceholderBackgrounds.forWorld("placeholder").render(g, 1234, 0);
        g.dispose();
        for (int y = 0; y < 360; y++) {
            for (int x = 0; x < 640; x++) {
                assertEquals(255, canvas.getRGB(x, y) >>> 24, "pixel " + x + "," + y + " not painted");
            }
        }
    }

    @Test
    void layersSinkWhenCameraRises() {
        ParallaxLayer near = PlaceholderBackgrounds.forWorld("placeholder").layers().get(1);
        ParallaxBackground only = new ParallaxBackground(new Color(0, 0, 0, 0), List.of(near));
        assertTrue(topOpaqueRow(only, 200) > topOpaqueRow(only, 0));
    }

    private static int topOpaqueRow(ParallaxBackground bg, int cameraAboveBottom) {
        BufferedImage canvas = new BufferedImage(640, 360, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        bg.render(g, 0, cameraAboveBottom);
        g.dispose();
        for (int y = 0; y < 360; y++) {
            if ((canvas.getRGB(320, y) >>> 24) != 0) return y;
        }
        return 360;
    }
}
