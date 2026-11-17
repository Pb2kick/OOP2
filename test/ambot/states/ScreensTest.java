package ambot.states;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ambot.input.Action;
import ambot.input.Controls;

class ScreensTest {
    private final FakeContext ctx = new FakeContext();

    private static Controls press(Action a) {
        return Controls.of(Set.of(a), Set.of(a), Set.of());
    }

    private void tick(Controls c) {
        ctx.states().update(c);
    }

    private void startOn(GameState s) {
        ctx.states().replaceAll(s);
        tick(Controls.NONE);
    }

    @Test
    void newGameStartsPlaying() {
        startOn(new MainMenuState(ctx));
        tick(press(Action.CONFIRM));
        tick(Controls.NONE);
        assertInstanceOf(PlayingState.class, ctx.states().top());
    }

    @Test
    void quitSkipsDisabledOptions() {
        startOn(new MainMenuState(ctx));
        tick(press(Action.DOWN));
        tick(press(Action.DOWN));
        tick(press(Action.CONFIRM));
        assertTrue(ctx.quitCalled);
    }

    @Test
    void creditsBackReturnsToMenu() {
        startOn(new MainMenuState(ctx));
        tick(press(Action.DOWN));
        tick(press(Action.CONFIRM));
        tick(Controls.NONE);
        assertInstanceOf(CreditsState.class, ctx.states().top());
        tick(press(Action.BACK));
        tick(Controls.NONE);
        assertInstanceOf(MainMenuState.class, ctx.states().top());
    }

    @Test
    void escapePushesPause() {
        startOn(new PlayingState(ctx));
        tick(press(Action.PAUSE));
        tick(Controls.NONE);
        assertInstanceOf(PausedState.class, ctx.states().top());
        assertEquals(2, ctx.states().size());
    }

    @Test
    void pauseEscapeResumes() {
        startOn(new PlayingState(ctx));
        tick(press(Action.PAUSE));
        tick(Controls.NONE);
        tick(press(Action.PAUSE));
        tick(Controls.NONE);
        assertInstanceOf(PlayingState.class, ctx.states().top());
    }

    @Test
    void focusLostPushesPause() {
        startOn(new PlayingState(ctx));
        ctx.states().focusLost();
        tick(Controls.NONE);
        assertInstanceOf(PausedState.class, ctx.states().top());
    }

    @Test
    void spawnsStandingOnFloor() {
        PlayingState playing = new PlayingState(ctx);
        startOn(playing);
        for (int i = 0; i < 5; i++) tick(Controls.NONE);
        assertTrue(playing.player().onGround());
        assertEquals(playing.level().spawn().y(), playing.player().y() + 28);
    }

    @Test
    void pausedFreezesPlayer() {
        PlayingState playing = new PlayingState(ctx);
        startOn(playing);
        for (int i = 0; i < 3; i++) tick(Controls.NONE);
        tick(press(Action.PAUSE));
        tick(Controls.NONE);
        double x = playing.player().x(), y = playing.player().y();
        for (int i = 0; i < 10; i++) tick(Controls.of(Set.of(Action.RIGHT), Set.of(), Set.of()));
        assertEquals(x, playing.player().x());
        assertEquals(y, playing.player().y());
    }
}
