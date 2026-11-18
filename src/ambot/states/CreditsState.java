package ambot.states;

import java.awt.Font;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import ambot.core.GameConfig;
import ambot.input.Action;
import ambot.input.Controls;

/**
 * Credits, read from {@code /credits.txt} (kept in step with docs/ASSETS.md by a test). Scrolls
 * upward on its own when it doesn't fit; Up/Down scroll by hand.
 */
public final class CreditsState implements GameState {
    private static final Font FONT = new Font(Font.MONOSPACED, Font.BOLD, 13);
    private static final Font HINT = new Font(Font.MONOSPACED, Font.PLAIN, 10);
    private static final int LINE = 20, TOP = 40, VISIBLE = 270;
    private static final double SPEED = 0.5;
    private static List<String> lines;

    private final GameContext ctx;
    private double offset;

    public CreditsState(GameContext ctx) {
        this.ctx = ctx;
    }

    /** The credits text, one entry per line; "credits missing" if the file can't be read. */
    static synchronized List<String> lines() {
        if (lines == null) {
            try (InputStream in = CreditsState.class.getResourceAsStream("/credits.txt")) {
                lines = in == null ? List.of("credits missing")
                        : List.of(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R", -1));
            } catch (IOException e) {
                lines = List.of("credits missing");
            }
        }
        return lines;
    }

    private int height() {
        return lines().size() * LINE;
    }

    @Override
    public void update(Controls c) {
        if (c.wasPressed(Action.BACK) || c.wasPressed(Action.CONFIRM)) {
            ctx.states().pop();
            return;
        }
        if (height() <= VISIBLE) return;
        if (c.isHeld(Action.DOWN)) offset += 3;
        else if (c.isHeld(Action.UP)) offset -= 3;
        else offset += SPEED;
        double loop = height() + VISIBLE;
        offset = ((offset % loop) + loop) % loop;
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(MainMenuState.BACKGROUND);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        boolean scrolls = height() > VISIBLE;
        int start = scrolls ? TOP + VISIBLE - (int) offset : TOP + (VISIBLE - height()) / 2;
        List<String> text = lines();
        for (int i = 0; i < text.size(); i++) {
            int y = start + i * LINE;
            if (y < TOP - LINE || y > TOP + VISIBLE + LINE) continue;
            MainMenuState.drawCentered(g, text.get(i), FONT, MainMenuState.TEXT, y);
        }
        g.setColor(MainMenuState.BACKGROUND);
        g.fillRect(0, GameConfig.LOGICAL_HEIGHT - 30, GameConfig.LOGICAL_WIDTH, 30);
        MainMenuState.drawCentered(g, "Esc / Enter: back", HINT, MainMenuState.FAINT, GameConfig.LOGICAL_HEIGHT - 12);
    }
}
