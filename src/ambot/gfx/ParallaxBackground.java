package ambot.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import ambot.core.GameConfig;

/** A sky colour plus layers drawn back to front, each scrolling at its own speed (spec section 5.6). */
public final class ParallaxBackground {
    private final Color sky;
    private final List<ParallaxLayer> layers;

    public ParallaxBackground(Color sky, List<ParallaxLayer> layers) {
        this.sky = sky;
        this.layers = List.copyOf(layers);
    }

    public List<ParallaxLayer> layers() {
        return layers;
    }

    /**
     * Fills the whole canvas.
     *
     * @param cameraX           the camera's left edge in level pixels
     * @param cameraAboveBottom how far the camera is above its lowest position; layers sink as it rises
     */
    public void render(Graphics2D g, int cameraX, int cameraAboveBottom) {
        g.setColor(sky);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        for (ParallaxLayer layer : layers) {
            int w = layer.image().getWidth();
            int drawY = layer.y() + (int) Math.round(cameraAboveBottom * layer.factor());
            for (int x = wrapOffset(cameraX, layer.factor(), w); x < GameConfig.LOGICAL_WIDTH; x += w) {
                g.drawImage(layer.image(), x, drawY, null);
            }
        }
    }

    /** Where the first copy of a repeating layer starts: always in {@code (-imageW, 0]}. */
    public static int wrapOffset(int cameraX, double factor, int imageW) {
        int scrolled = (int) Math.floor(cameraX * factor);
        return -Math.floorMod(scrolled, imageW);
    }
}
