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

/** A yes/no question over the current screen. "No" is the default, and Esc means no. */
public final class ConfirmState implements GameState {
    private static final Color DIM = new Color(0, 0, 0, 170);
    private static final Font QUESTION = new Font(Font.MONOSPACED, Font.BOLD, 13);

    private final GameContext ctx;
    private final String question;
    private final Menu menu;
    private final InputGrace grace = new InputGrace(InputGrace.GRACE_TICKS);

    public ConfirmState(GameContext ctx, String question, Runnable yes) {
        this.ctx = ctx;
        this.question = question;
        this.menu = new Menu(List.of(
                MenuItem.of("No", () -> ctx.states().pop()),
                MenuItem.of("Yes", yes)));
    }

    @Override
    public void update(Controls c) {
        if (!grace.tick()) return;
        if (c.wasPressed(Action.BACK)) {
            ctx.states().pop();
            return;
        }
        menu.update(c, ctx.audio());
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(DIM);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        MainMenuState.drawCentered(g, question, QUESTION, MainMenuState.TEXT, 150);
        menu.render(g, GameConfig.LOGICAL_WIDTH / 2, 190);
    }

    @Override
    public boolean isOverlay() {
        return true;
    }
}
