package ambot.entities.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import ambot.gfx.AnimationClip;
import ambot.gfx.SpriteSheet;

class PlayerAnimationsTest {
    private static final SpriteSheet SHEET =
            SpriteSheet.load(PlayerAnimations.SHEET_PATH, PlayerAnimations.CELL_W, PlayerAnimations.CELL_H);

    @Test
    void everyClipFrameHasPixels() {
        for (AnimationClip clip : PlayerAnimations.ALL)
            for (int i = 0; i < clip.frameCount(); i++)
                assertFalse(SHEET.isEmpty(clip.startCol() + i, clip.row()), clip.name() + " frame " + i);
    }

    @Test
    void footYMatchesArt() {
        for (AnimationClip clip : PlayerAnimations.ALL) {
            int lowest = -1;
            for (int i = 0; i < clip.frameCount(); i++) {
                BufferedImage f = SHEET.frame(clip.startCol() + i, clip.row());
                for (int y = 0; y < f.getHeight(); y++)
                    for (int x = 0; x < f.getWidth(); x++)
                        if ((f.getRGB(x, y) >>> 24) > 20) lowest = Math.max(lowest, y);
            }
            assertEquals(lowest + 1, clip.footY(), clip.name());
        }
    }

    @Test
    void everyStateHasAClip() {
        for (PlayerState s : PlayerState.values())
            assertNotNull(PlayerAnimations.forState(s), s.name());
    }
}
