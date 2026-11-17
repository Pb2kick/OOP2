package ambot.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import ambot.core.GameConfig;
import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.LevelCatalog;

/** Shown over a finished level. Confirm goes on to the next level, or back to the main menu after the last one. */
public final class LevelCompleteState implements GameState {
    private static final Color DIM = new Color(0, 0, 0, 0x80);
    private static final Color TEXT = new Color(0xFFF1E8);
    private static final Color GOLD = new Color(0xFFEC27);
    private static final Font BIG = new Font(Font.MONOSPACED, Font.BOLD, 32);
    private static final Font MEDIUM = new Font(Font.MONOSPACED, Font.PLAIN, 16);
    private static final Font SMALL = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private final GameContext ctx;
    private final LevelCatalog catalog;
    private final int index;
    private final String title;

    public LevelCompleteState(GameContext ctx, LevelCatalog catalog, int index, String title) {
        this.ctx = ctx;
        this.catalog = catalog;
        this.index = index;
        this.title = title;
    }

    @Override
    public void update(Controls c) {
        if (!c.wasPressed(Action.CONFIRM)) return;
        if (index + 1 < catalog.size()) {
            ctx.states().replaceAll(new PlayingState(ctx, catalog, index + 1));
        } else {
            ctx.states().replaceAll(new MainMenuState(ctx));
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(DIM);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        MainMenuState.drawCentered(g, "LEVEL COMPLETE", BIG, GOLD, 150);
        MainMenuState.drawCentered(g, title, MEDIUM, TEXT, 182);
        MainMenuState.drawCentered(g, "Enter: continue", SMALL, TEXT, 230);
    }

    @Override
    public boolean isOverlay() {
        return true;
    }
}
