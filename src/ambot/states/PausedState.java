package ambot.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

import ambot.core.GameConfig;
import ambot.input.Action;
import ambot.input.Controls;
import ambot.ui.Menu;
import ambot.ui.MenuItem;

/** Pause overlay drawn over the frozen game. */
public final class PausedState implements GameState {
    private static final Color DIM = new Color(0, 0, 0, 150);

    private final GameContext ctx;
    private final Menu menu;

    public PausedState(GameContext ctx) {
        this.ctx = ctx;
        menu = new Menu(List.of(
                MenuItem.of("Resume", () -> ctx.states().pop()),
                MenuItem.of("Main Menu", () -> ctx.states().replaceAll(new MainMenuState(ctx)))));
    }

    @Override
    public void update(Controls c) {
        if (c.wasPressed(Action.PAUSE) || c.wasPressed(Action.BACK)) {
            ctx.states().pop();
            return;
        }
        menu.update(c);
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(DIM);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        MainMenuState.drawCentered(g, "PAUSED", new Font(Font.MONOSPACED, Font.BOLD, 32), MainMenuState.TEXT, 140);
        menu.render(g, GameConfig.LOGICAL_WIDTH / 2, 190);
    }

    @Override
    public boolean isOverlay() {
        return true;
    }
}
