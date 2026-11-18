package ambot.states;

import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

import ambot.core.GameConfig;
import ambot.input.Action;
import ambot.input.Controls;

/** The controls from spec section 5.5, for reading. */
public final class ControlsState implements GameState {
    static final List<String> LINES = List.of(
            "Move          A / D  or  Left / Right",
            "Jump          Space, W  or  Up",
            "Crouch        S  or  Down",
            "Drop through  S / Down on a plank",
            "Attack        J",
            "Pause         Esc",
            "Fullscreen    F11");
    private static final Font TITLE = new Font(Font.MONOSPACED, Font.BOLD, 24);
    private static final Font ROW = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    private final GameContext ctx;

    public ControlsState(GameContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void update(Controls c) {
        if (c.wasPressed(Action.BACK) || c.wasPressed(Action.CONFIRM)) ctx.states().pop();
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(MainMenuState.BACKGROUND);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        MainMenuState.drawCentered(g, "CONTROLS", TITLE, MainMenuState.TEXT, 70);
        g.setFont(ROW);
        g.setColor(MainMenuState.TEXT);
        int x = (GameConfig.LOGICAL_WIDTH - g.getFontMetrics().stringWidth(LINES.get(0))) / 2;
        for (int i = 0; i < LINES.size(); i++) g.drawString(LINES.get(i), x, 120 + i * 22);
        MainMenuState.drawCentered(g, "Esc / Enter: back", new Font(Font.MONOSPACED, Font.PLAIN, 10),
                MainMenuState.FAINT, 300);
    }
}
