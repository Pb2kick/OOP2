package ambot.states;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ambot.input.Action;
import ambot.input.Controls;

class CrashStateTest {
    private final FakeContext ctx = new FakeContext();

    static Controls press(Action a) {
        return Controls.of(Set.of(a), Set.of(a), Set.of());
    }

    @Test
    void showsMessageAndReturnsToMenu() {
        CrashState crash = new CrashState(ctx, new IllegalStateException("level p9 is missing"));
        BufferedImage img = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> crash.render(g));
        g.dispose();
        ctx.states().replaceAll(crash);
        ctx.states().update(Controls.NONE);
        ctx.states().update(press(Action.CONFIRM));
        ctx.states().update(Controls.NONE);
        assertInstanceOf(MainMenuState.class, ctx.states().top());
    }

    @Test
    void escapeQuits() {
        ctx.states().replaceAll(new CrashState(ctx, new RuntimeException()));
        ctx.states().update(Controls.NONE);
        ctx.states().update(press(Action.BACK));
        assertTrue(ctx.quitCalled);
    }

    @Test
    void longMessageWrapsToThreeLines() {
        List<String> lines = CrashState.wrap("word ".repeat(80), 70, 3);
        assertEquals(3, lines.size());
        lines.forEach(l -> assertTrue(l.length() <= 70, "'" + l + "'"));
        assertTrue(lines.get(2).endsWith("..."));
        assertEquals(List.of("(no message)"), CrashState.wrap(null, 70, 3));
    }

    @Test
    void resetDropsPendingChanges() {
        StateManager sm = new StateManager();
        GameState a = new CrashState(ctx, new RuntimeException("a"));
        GameState b = new CrashState(ctx, new RuntimeException("b"));
        GameState crash = new CrashState(ctx, new RuntimeException("crash"));
        sm.replaceAll(a);
        sm.update(Controls.NONE);
        sm.push(b); // queued, not applied yet
        sm.reset(crash);
        sm.update(Controls.NONE);
        assertSame(crash, sm.top());
        assertEquals(1, sm.size());
    }
}
