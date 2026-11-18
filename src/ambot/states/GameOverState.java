package ambot.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

import ambot.core.GameConfig;
import ambot.input.Controls;
import ambot.ui.Menu;
import ambot.ui.MenuItem;

/** Shown over the level when the player has died: Retry from the last checkpoint, or Main Menu. */
public final class GameOverState implements GameState {
    private static final Color DIM = new Color(0, 0, 0, 160);
    private static final Color RED = new Color(0xFF004D);
    private static final Font TITLE = new Font(Font.MONOSPACED, Font.BOLD, 32);

    private final Menu menu;
    private final InputGrace grace = new InputGrace(InputGrace.GRACE_TICKS);

    public GameOverState(GameContext ctx, PlayingState playing) {
        menu = new Menu(List.of(
                MenuItem.of("Retry", () -> {
                    playing.retry();
                    ctx.states().pop();
                }),
                MenuItem.of("Main Menu", () -> ctx.states().replaceAll(new MainMenuState(ctx)))));
    }

    @Override
    public void update(Controls c) {
        if (grace.tick()) menu.update(c);
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(DIM);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        MainMenuState.drawCentered(g, "GAME OVER", TITLE, RED, 140);
        menu.render(g, GameConfig.LOGICAL_WIDTH / 2, 190);
    }

    @Override
    public boolean isOverlay() {
        return true;
    }
}
