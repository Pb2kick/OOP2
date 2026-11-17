package ambot.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

import ambot.core.GameConfig;
import ambot.input.Controls;
import ambot.ui.Menu;
import ambot.ui.MenuItem;

/** Title screen with the main menu. */
public final class MainMenuState implements GameState {
    static final Color BACKGROUND = new Color(0x1D2B53);
    static final Color TEXT = new Color(0xFFF1E8);

    private final Menu menu;

    public MainMenuState(GameContext ctx) {
        menu = new Menu(List.of(
                MenuItem.of("New Game", () -> ctx.states().replaceAll(new PlayingState(ctx))),
                MenuItem.disabled("Options (coming soon)"),
                MenuItem.of("Credits", () -> ctx.states().push(new CreditsState(ctx))),
                MenuItem.of("Quit", ctx::quit)));
    }

    @Override
    public void update(Controls c) {
        menu.update(c);
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        drawCentered(g, GameConfig.TITLE, new Font(Font.MONOSPACED, Font.BOLD, 48), TEXT, 120);
        menu.render(g, GameConfig.LOGICAL_WIDTH / 2, 200);
    }

    static void drawCentered(Graphics2D g, String text, Font font, Color color, int baselineY) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (GameConfig.LOGICAL_WIDTH - fm.stringWidth(text)) / 2, baselineY);
    }
}
