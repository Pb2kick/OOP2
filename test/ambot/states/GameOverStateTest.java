package ambot.states;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.Level;
import ambot.level.LevelCatalog;
import ambot.level.TestLevels;

class GameOverStateTest {
    private final FakeContext ctx = new FakeContext();

    static final Level LEVEL = TestLevels.ascii(
            "....................",
            "....................",
            ".P................E.",
            "####################");

    static Controls press(Action a) {
        return Controls.of(Set.of(a), Set.of(a), Set.of());
    }

    private void tick(Controls c, int n) {
        for (int i = 0; i < n; i++) ctx.states().update(c);
    }

    /** A dead player with Game Over pushed; the next tick enters it and gives it its first update. */
    private PlayingState deadWithGameOver() {
        PlayingState playing = new PlayingState(ctx, LevelCatalog.of(LEVEL), 0);
        ctx.states().replaceAll(playing);
        tick(Controls.NONE, 1);
        playing.player().kill();
        ctx.states().push(new GameOverState(ctx, playing));
        return playing;
    }

    @Test
    void confirmIgnoredDuringGrace() {
        PlayingState playing = deadWithGameOver();
        tick(press(Action.CONFIRM), 1); // first Game Over tick: a stray Space/Enter
        tick(Controls.NONE, 1);
        assertInstanceOf(GameOverState.class, ctx.states().top());
        tick(Controls.NONE, InputGrace.GRACE_TICKS - 3);
        tick(press(Action.CONFIRM), 1); // tick 20: Retry
        tick(Controls.NONE, 1);
        assertInstanceOf(PlayingState.class, ctx.states().top());
        assertFalse(playing.player().isDead());
        assertEquals(3, playing.player().health());
    }

    @Test
    void mainMenuOption() {
        deadWithGameOver();
        tick(Controls.NONE, InputGrace.GRACE_TICKS);
        tick(press(Action.DOWN), 1);
        tick(press(Action.CONFIRM), 1);
        tick(Controls.NONE, 1);
        assertInstanceOf(MainMenuState.class, ctx.states().top());
    }

    @Test
    void levelCompleteHasGraceToo() {
        Level b = TestLevels.ascii(Map.of("title", "B"),
                "....................",
                "....................",
                ".P................E.",
                "####################");
        PlayingState playing = new PlayingState(ctx, LevelCatalog.of(LEVEL, b), 0);
        ctx.states().replaceAll(playing);
        tick(Controls.NONE, 1);
        ctx.states().push(new LevelCompleteState(ctx, LevelCatalog.of(LEVEL, b), 0, "Test"));
        tick(press(Action.CONFIRM), 1);
        tick(Controls.NONE, 1);
        assertInstanceOf(LevelCompleteState.class, ctx.states().top(), "a stray Space doesn't skip it");
        tick(Controls.NONE, InputGrace.GRACE_TICKS);
        tick(press(Action.CONFIRM), 1);
        tick(Controls.NONE, 1);
        PlayingState next = assertInstanceOf(PlayingState.class, ctx.states().top());
        assertEquals("B", next.level().title());
    }
}
