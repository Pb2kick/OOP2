package ambot.states;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.World;
import ambot.ui.MenuItem;

class MenuFlowTest {
    private final FakeContext ctx = new FakeContext();

    static Controls press(Action a) {
        return Controls.of(Set.of(a), Set.of(a), Set.of());
    }

    private void tick(Controls c, int n) {
        for (int i = 0; i < n; i++) ctx.states().update(c);
    }

    private void tick(Controls c) {
        tick(c, 1);
    }

    private MainMenuState mainMenu() {
        MainMenuState m = new MainMenuState(ctx);
        ctx.states().replaceAll(m);
        tick(Controls.NONE);
        return m;
    }

    private String playingTitle() {
        return assertInstanceOf(PlayingState.class, ctx.states().top()).level().title();
    }

    @Test
    void continueDisabledWithoutProgress() {
        assertEquals(1, mainMenu().selectedIndex(), "New Game is selected; Continue is greyed out");
    }

    @Test
    void continueResumesNextUnfinished() {
        ctx.progress.markCompleted("w1-1");
        MainMenuState m = mainMenu();
        assertEquals(0, m.selectedIndex(), "Continue is offered first");
        tick(press(Action.CONFIRM));
        tick(Controls.NONE);
        assertEquals("Up and Through", playingTitle());
    }

    private void openNewGameConfirmation() {
        ctx.progress.markCompleted("w1-1");
        mainMenu();
        tick(press(Action.DOWN)); // New Game
        tick(press(Action.CONFIRM));
        tick(Controls.NONE);
    }

    @Test
    void newGameWithProgressAsksFirst() {
        openNewGameConfirmation();
        assertInstanceOf(ConfirmState.class, ctx.states().top());
    }

    @Test
    void answeringNoKeepsProgress() {
        openNewGameConfirmation();
        tick(Controls.NONE, InputGrace.GRACE_TICKS);
        tick(press(Action.CONFIRM)); // "No" is the default
        tick(Controls.NONE);
        assertInstanceOf(MainMenuState.class, ctx.states().top());
        assertEquals(Set.of("w1-1"), ctx.progress.data().completed());
    }

    @Test
    void answeringYesResetsAndStarts() {
        openNewGameConfirmation();
        tick(Controls.NONE, InputGrace.GRACE_TICKS);
        tick(press(Action.DOWN));
        tick(press(Action.CONFIRM));
        tick(Controls.NONE);
        assertEquals("First Steps", playingTitle());
        assertEquals(Set.of(), ctx.progress.data().completed());
    }

    @Test
    void worldSelectLocksLaterLevels() {
        List<MenuItem> rows = new WorldSelectState(ctx).rows();
        List<MenuItem> levels = rows.stream().filter(r -> r.label().trim().startsWith("1-")).toList();
        assertEquals(4, levels.size());
        assertTrue(levels.get(0).enabled());
        for (int i = 1; i < 4; i++) assertTrue(!levels.get(i).enabled(), levels.get(i).label());
        assertTrue(rows.stream().anyMatch(r -> r.label().contains("World 2") && r.label().contains("coming soon")));
    }

    @Test
    void quittingMidLevelUnlocksNothing() {
        mainMenu();
        ctx.states().push(new WorldSelectState(ctx));
        tick(Controls.NONE);
        tick(press(Action.CONFIRM)); // 1-1 is the first selectable row
        tick(Controls.NONE);
        assertEquals("First Steps", playingTitle());
        tick(Controls.of(Set.of(Action.RIGHT), Set.of(), Set.of()), 30);
        tick(press(Action.PAUSE));
        tick(Controls.NONE);
        tick(press(Action.DOWN)); // Main Menu
        tick(press(Action.CONFIRM));
        tick(Controls.NONE);
        assertInstanceOf(MainMenuState.class, ctx.states().top());
        assertEquals(Set.of(), ctx.progress.data().completed());
    }

    @Test
    void lastLevelOfWorldOpensWorldClear() {
        mainMenu();
        World w1 = ctx.campaign.world(1);
        ctx.states().push(new LevelCompleteState(ctx, ctx.campaign.catalog(w1), w1.levels().size() - 1, "First Blood"));
        tick(Controls.NONE, InputGrace.GRACE_TICKS);
        tick(press(Action.CONFIRM));
        tick(Controls.NONE);
        assertInstanceOf(WorldClearState.class, ctx.states().top());
        tick(Controls.NONE, InputGrace.GRACE_TICKS);
        tick(press(Action.CONFIRM));
        tick(Controls.NONE);
        assertInstanceOf(WorldSelectState.class, ctx.states().top());
        assertEquals(2, ctx.states().size(), "World Select over the main menu");
    }
}
