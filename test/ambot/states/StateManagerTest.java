package ambot.states;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ambot.input.Controls;

class StateManagerTest {
    private final List<String> log = new ArrayList<>();
    private final StateManager sm = new StateManager();
    private final Graphics2D g = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB).createGraphics();

    class RecordingState implements GameState {
        final String name;
        final boolean overlay;
        Runnable onUpdate = () -> { };

        RecordingState(String name, boolean overlay) {
            this.name = name;
            this.overlay = overlay;
        }

        @Override public void update(Controls c) { log.add(name + ":update"); onUpdate.run(); }
        @Override public void render(Graphics2D g) { log.add(name + ":render"); }
        @Override public void onEnter() { log.add(name + ":enter"); }
        @Override public void onFocusLost() { log.add(name + ":focus"); }
        @Override public boolean isOverlay() { return overlay; }
    }

    private final RecordingState a = new RecordingState("a", false);

    @Test
    void pushIsDeferredUntilNextUpdate() {
        sm.replaceAll(a);
        sm.update(Controls.NONE);
        assertEquals(List.of("a:enter", "a:update"), log);
        RecordingState b = new RecordingState("b", false);
        sm.push(b);
        assertSame(a, sm.top());
        sm.update(Controls.NONE);
        assertEquals(List.of("a:enter", "a:update", "b:enter", "b:update"), log);
    }

    @Test
    void overlayRendersStateBelow() {
        sm.replaceAll(a);
        sm.push(new RecordingState("b", true));
        sm.update(Controls.NONE);
        log.clear();
        sm.render(g);
        assertEquals(List.of("a:render", "b:render"), log);
    }

    @Test
    void nonOverlayHidesStatesBelow() {
        sm.replaceAll(a);
        sm.push(new RecordingState("b", false));
        sm.update(Controls.NONE);
        log.clear();
        sm.render(g);
        assertEquals(List.of("b:render"), log);
    }

    @Test
    void popReturnsToPrevious() {
        sm.replaceAll(a);
        sm.push(new RecordingState("b", false));
        sm.update(Controls.NONE);
        sm.pop();
        sm.update(Controls.NONE);
        assertEquals("a:update", log.get(log.size() - 1));
        assertEquals(1, sm.size());
    }

    @Test
    void poppingLastStateThrows() {
        sm.replaceAll(a);
        sm.update(Controls.NONE);
        sm.pop();
        assertThrows(IllegalStateException.class, () -> sm.update(Controls.NONE));
    }

    @Test
    void opsQueuedDuringUpdateApplyNextTick() {
        RecordingState b = new RecordingState("b", false);
        a.onUpdate = () -> sm.push(b);
        sm.replaceAll(a);
        sm.update(Controls.NONE);
        assertSame(a, sm.top());
        a.onUpdate = () -> { };
        sm.update(Controls.NONE);
        assertSame(b, sm.top());
    }

    @Test
    void focusLostGoesToTop() {
        sm.replaceAll(a);
        sm.push(new RecordingState("b", false));
        sm.update(Controls.NONE);
        log.clear();
        sm.focusLost();
        assertEquals(List.of("b:focus"), log);
    }

    @Test
    void emptyManagerIsNoOp() {
        assertDoesNotThrow(() -> {
            sm.update(Controls.NONE);
            sm.render(g);
            sm.focusLost();
        });
        assertEquals(null, sm.top());
    }
}
