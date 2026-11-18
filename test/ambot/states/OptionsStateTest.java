package ambot.states;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ambot.input.Action;
import ambot.input.Controls;

class OptionsStateTest {
    private final FakeContext ctx = new FakeContext();

    static Controls press(Action a) {
        return Controls.of(Set.of(a), Set.of(a), Set.of());
    }

    private void tick(Controls c, int n) {
        for (int i = 0; i < n; i++) ctx.states().update(c);
    }

    private void openOptionsOverMenu() {
        ctx.states().replaceAll(new MainMenuState(ctx));
        ctx.states().update(Controls.NONE);
        ctx.states().push(new OptionsState(ctx));
        ctx.states().update(Controls.NONE);
    }

    @Test
    void rightRaisesMusicAndSaves() {
        openOptionsOverMenu();
        tick(press(Action.RIGHT), 1);
        assertEquals(8, ctx.progress.data().musicVolume());
        assertEquals(8, ctx.store.saved().musicVolume());
        assertEquals(8, ctx.audio.musicVolume);
    }

    @Test
    void volumeClampsAtBounds() {
        openOptionsOverMenu();
        tick(press(Action.RIGHT), 15);
        assertEquals(10, ctx.progress.data().musicVolume());
        tick(press(Action.DOWN), 1); // Sound
        tick(press(Action.LEFT), 15);
        assertEquals(0, ctx.progress.data().sfxVolume());
        assertEquals(0, ctx.audio.sfxVolume);
    }

    @Test
    void fullscreenToggles() {
        openOptionsOverMenu();
        tick(press(Action.DOWN), 2);
        tick(press(Action.CONFIRM), 1);
        assertTrue(ctx.fullscreen);
        assertTrue(ctx.progress.data().fullscreen());
        tick(press(Action.LEFT), 1);
        assertTrue(!ctx.fullscreen, "LEFT/RIGHT toggle it too");
    }

    @Test
    void controlsOpensAndCloses() {
        openOptionsOverMenu();
        tick(press(Action.DOWN), 3);
        tick(press(Action.CONFIRM), 1);
        tick(Controls.NONE, 1);
        assertInstanceOf(ControlsState.class, ctx.states().top());
        tick(press(Action.BACK), 1);
        tick(Controls.NONE, 1);
        assertInstanceOf(OptionsState.class, ctx.states().top());
    }

    @Test
    void backReturnsToMenu() {
        openOptionsOverMenu();
        tick(press(Action.UP), 1); // wraps to Back
        tick(press(Action.CONFIRM), 1);
        tick(Controls.NONE, 1);
        assertInstanceOf(MainMenuState.class, ctx.states().top());
    }

    @Test
    void mainMenuOpensOptions() {
        MainMenuState menu = new MainMenuState(ctx);
        ctx.states().replaceAll(menu);
        ctx.states().update(Controls.NONE);
        for (int i = 0; i < 5 && menu.selectedIndex() != MainMenuState.OPTIONS; i++) tick(press(Action.DOWN), 1);
        assertEquals(MainMenuState.OPTIONS, menu.selectedIndex());
        tick(press(Action.CONFIRM), 1);
        tick(Controls.NONE, 1);
        assertInstanceOf(OptionsState.class, ctx.states().top());
    }
}
