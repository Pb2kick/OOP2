package Entities;

import static Utils.Constants.PlayerConstats.ATTACK;
import static Utils.Constants.PlayerConstats.CROUCH;
import static Utils.Constants.PlayerConstats.GetSpriteAmount;
import static Utils.Constants.PlayerConstats.IDLE;
import static Utils.Constants.PlayerConstats.RUNNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void everyFrameOfEveryUsedAnimationHasPixels() {
        Player p = new Player(0, 0);
        for (int action : new int[] { IDLE, RUNNING, ATTACK, CROUCH })
            for (int i = 0; i < GetSpriteAmount(action); i++)
                assertTrue(hasPixels(p.frameFor(action, i)), action + ":" + i);
    }

    @Test
    void allElevenColumnsAreLoaded() {
        assertTrue(hasPixels(new Player(0, 0).frameFor(2, 9)));
    }

    @Test
    void drawSizeKeepsSpriteAspect() {
        assertEquals(Player.DRAW_WIDTH * 37, Player.DRAW_HEIGHT * 50);
    }
}
