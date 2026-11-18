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
    void hurtAndDeathClipsHaveFrames() {
        for (int col = 4; col <= 9; col++) assertFalse(SHEET.isEmpty(col, 8), "row 8 column " + col);
        assertEquals(8, PlayerAnimations.HURT.row());
        assertEquals(4, PlayerAnimations.HURT.startCol());
        assertEquals(8, PlayerAnimations.DEATH.row());
        assertEquals(7, PlayerAnimations.DEATH.startCol());
    }

    @Test
    void everyStateHasAClip() {
        for (PlayerState s : PlayerState.values())
            assertNotNull(PlayerAnimations.forState(s), s.name());
    }
}
