package ambot.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Optional;

import ambot.audio.Music;
import ambot.core.GameConfig;
import ambot.input.Controls;
import ambot.level.Campaign;
import ambot.level.World;
import ambot.ui.Menu;
import ambot.ui.MenuItem;

/** Title screen with the main menu (spec section 5.3). */
public final class MainMenuState implements GameState {
    static final Color BACKGROUND = new Color(0x1D2B53);
    static final Color TEXT = new Color(0xFFF1E8);
    static final Color FAINT = new Color(0x83769C);
    static final int CONTINUE = 0, NEW_GAME = 1, WORLD_SELECT = 2, OPTIONS = 3, CREDITS = 4, QUIT = 5;
    private static final Font TITLE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 48);
    private static final Font SMALL = new Font(Font.MONOSPACED, Font.PLAIN, 10);

    private final GameContext ctx;
    private final Menu menu;

    public MainMenuState(GameContext ctx) {
        this.ctx = ctx;
        Campaign campaign = ctx.campaign();
        Optional<int[]> next = campaign.nextUnfinished(ctx.progress().data());
        boolean canContinue = ctx.progress().hasProgress() && next.isPresent();
        menu = new Menu(List.of(
                canContinue ? MenuItem.of("Continue", () -> play(next.get()[0], next.get()[1])) : MenuItem.disabled("Continue"),
                MenuItem.of("New Game", this::newGame),
                MenuItem.of("World Select", () -> ctx.states().push(new WorldSelectState(ctx))),
                MenuItem.of("Options", () -> ctx.states().push(new OptionsState(ctx))),
                MenuItem.of("Credits", () -> ctx.states().push(new CreditsState(ctx))),
                MenuItem.of("Quit", ctx::quit)));
    }

    private void newGame() {
        if (!ctx.progress().hasProgress()) {
            play(1, 0);
            return;
        }
        ctx.states().push(new ConfirmState(ctx, "Start a new game? Your progress will be erased.", () -> {
            ctx.progress().reset();
            play(1, 0);
        }));
    }

    private void play(int worldNumber, int levelIndex) {
        World w = ctx.campaign().world(worldNumber);
        ctx.states().replaceAll(new PlayingState(ctx, ctx.campaign().catalog(w), levelIndex));
    }

    int selectedIndex() {
        return menu.selectedIndex();
    }

    @Override
    public void onEnter() {
        ctx.audio().playMusic(Music.MENU);
    }

    @Override
    public void update(Controls c) {
        menu.update(c, ctx.audio());
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        drawCentered(g, GameConfig.TITLE, TITLE_FONT, TEXT, 110);
        menu.render(g, GameConfig.LOGICAL_WIDTH / 2, 175);
        g.setFont(SMALL);
        g.setColor(FAINT);
        String version = "v" + GameConfig.VERSION;
        g.drawString(version, GameConfig.LOGICAL_WIDTH - g.getFontMetrics().stringWidth(version) - 8,
                GameConfig.LOGICAL_HEIGHT - 8);
    }

    static void drawCentered(Graphics2D g, String text, Font font, Color color, int baselineY) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (GameConfig.LOGICAL_WIDTH - fm.stringWidth(text)) / 2, baselineY);
    }
}
