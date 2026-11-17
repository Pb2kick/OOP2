package ambot.gfx;

import java.awt.image.BufferedImage;

/**
 * One background layer that repeats sideways and scrolls slower than the level.
 *
 * @param factor how fast it scrolls compared with the camera (0 = fixed, 1 = with the level)
 * @param y      its top edge on the canvas when the camera is at the bottom of the level
 */
public record ParallaxLayer(BufferedImage image, double factor, int y) {
}
