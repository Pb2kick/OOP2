package ambot.states;

import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

import ambot.core.GameConfig;
import ambot.input.Action;
import ambot.input.Controls;

/** Credits screen. Lines follow docs/ASSETS.md; team names are added before v0.1. */
public final class CreditsState implements GameState {
    static final List<String> LINES = List.of(
            "AMBOT",
            "",
            "Original game: Original OOP2 team (2025)",
            "Player sprite: Adventurer by rvros (itch.io)",
            "Restoration: 2026",
            "",
            "Esc / Enter: back");

    private final GameContext ctx;

    public CreditsState(GameContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void update(Controls c) {
        if (c.wasPressed(Action.BACK) || c.wasPressed(Action.CONFIRM)) {
            ctx.states().pop();
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(MainMenuState.BACKGROUND);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        Font font = new Font(Font.MONOSPACED, Font.BOLD, 14);
        for (int i = 0; i < LINES.size(); i++) {
            MainMenuState.drawCentered(g, LINES.get(i), font, MainMenuState.TEXT, 110 + i * 22);
        }
    }
}
