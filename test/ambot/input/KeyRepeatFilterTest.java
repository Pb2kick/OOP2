package ambot.input;

import static java.awt.event.KeyEvent.VK_F11;
import static java.awt.event.KeyEvent.VK_F3;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeyRepeatFilterTest {

    @Test
    void onlyFirstPressCountsWhileHeld() {
        KeyRepeatFilter f = new KeyRepeatFilter();
        assertTrue(f.firstPress(VK_F11));
        assertFalse(f.firstPress(VK_F11), "OS auto-repeat must not toggle fullscreen again");
        f.released(VK_F11);
        assertTrue(f.firstPress(VK_F11));
    }

    @Test
    void keysAreIndependent() {
        KeyRepeatFilter f = new KeyRepeatFilter();
        assertTrue(f.firstPress(VK_F11));
        assertTrue(f.firstPress(VK_F3));
    }

    @Test
    void resetForgetsHeldKeys() {
        KeyRepeatFilter f = new KeyRepeatFilter();
        f.firstPress(VK_F11);
        f.reset(); // e.g. focus lost, release never delivered
        assertTrue(f.firstPress(VK_F11));
    }
}
