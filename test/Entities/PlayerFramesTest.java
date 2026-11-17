package Entities;

import static Utils.Constants.PlayerConstats.GetSpriteAmount;
import static Utils.Constants.PlayerConstats.IDLE;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class PlayerFramesTest {

    static boolean hasPixels(BufferedImage img) {
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                if ((img.getRGB(x, y) >>> 24) > 20) return true;
        return false;
    }

    @Test
    void idleFramesHavePixels() {
        Player p = new Player(0, 0);
        for (int i = 0; i < GetSpriteAmount(IDLE); i++)
            assertTrue(hasPixels(p.frameFor(IDLE, i)), "idle " + i);
    }
}
