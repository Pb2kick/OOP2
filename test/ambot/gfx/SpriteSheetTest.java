package ambot.gfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class SpriteSheetTest {
    private static final SpriteSheet SHEET = SpriteSheet.load("/Betteradventurer-Sheet.png", 50, 37);

    @Test
    void loadsReworkedSheetAsElevenByEleven() {
        assertEquals(11, SHEET.columns());
        assertEquals(11, SHEET.rows());
    }

    @Test
    void frameIsCellSized() {
        BufferedImage f = SHEET.frame(0, 0);
        assertEquals(50, f.getWidth());
        assertEquals(37, f.getHeight());
    }

    @Test
    void missingResourceNamesPath() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> SpriteSheet.load("/nope.png", 50, 37));
        assertTrue(e.getMessage().contains("/nope.png"), e.getMessage());
    }

    @Test
    void outOfRangeFrameThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> SHEET.frame(11, 0));
    }

    @Test
    void strayPixelRemoved() {
        assertTrue(SHEET.isEmpty(8, 1), "cell (8,1) should be empty after removing the stray pixel");
    }
}
