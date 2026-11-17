package ambot.input;

import static ambot.input.Action.ATTACK;
import static ambot.input.Action.BACK;
import static ambot.input.Action.CONFIRM;
import static ambot.input.Action.JUMP;
import static ambot.input.Action.LEFT;
import static ambot.input.Action.PAUSE;
import static ambot.input.Action.RIGHT;
import static ambot.input.Action.UP;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_F11;
import static java.awt.event.KeyEvent.VK_J;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.awt.event.KeyEvent.VK_W;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class InputStateTest {
    private final InputState in = new InputState(KeyBindings.defaults());

    @Test
    void tapBetweenPollsIsNotLost() {
        in.keyPressed(VK_SPACE);
        in.keyReleased(VK_SPACE);
        Controls c = in.poll();
        assertTrue(c.wasPressed(JUMP));
        assertFalse(c.isHeld(JUMP));
        assertTrue(c.wasReleased(JUMP));
    }

    @Test
    void pressedReportedOnlyOnce() {
        in.keyPressed(VK_D);
        assertTrue(in.poll().wasPressed(RIGHT));
        Controls c = in.poll();
        assertFalse(c.wasPressed(RIGHT));
        assertTrue(c.isHeld(RIGHT));
    }

    @Test
    void autoRepeatDoesNotRetrigger() {
        in.keyPressed(VK_J);
        in.poll();
        in.keyPressed(VK_J); // OS key auto-repeat sends another press without a release
        Controls c = in.poll();
        assertFalse(c.wasPressed(ATTACK));
        assertTrue(c.isHeld(ATTACK));
    }

    @Test
    void actionHeldWhileAnyBoundKeyHeld() {
        in.keyPressed(VK_A);
        in.keyPressed(VK_LEFT);
        in.keyReleased(VK_A);
        assertTrue(in.poll().isHeld(LEFT));
    }

    @Test
    void releaseAllClearsHeldActions() {
        in.keyPressed(VK_D);
        in.actionPressed(ATTACK);
        in.poll();
        in.releaseAll();
        Controls c = in.poll();
        assertFalse(c.isHeld(RIGHT));
        assertFalse(c.isHeld(ATTACK));
    }

    @Test
    void defaultBindings() {
        KeyBindings b = KeyBindings.defaults();
        assertEquals(Set.of(UP, JUMP), b.actionsFor(VK_W));
        assertEquals(Set.of(JUMP, CONFIRM), b.actionsFor(VK_SPACE));
        assertEquals(Set.of(PAUSE, BACK), b.actionsFor(VK_ESCAPE));
        assertEquals(Set.of(), b.actionsFor(VK_F11));
    }
}
