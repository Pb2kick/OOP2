package ambot.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import ambot.core.GameConfig;
import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.World;

/** Shown after the last level of a world; Enter goes to World Select (over the main menu). */
public final class WorldClearState implements GameState {
    private static final Color GOLD = new Color(0xFFEC27);
    private static final Font BIG = new Font(Font.MONOSPACED, Font.BOLD, 36);
    private static final Font SMALL = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private final GameContext ctx;
    private final World world;
    private final InputGrace grace = new InputGrace(InputGrace.GRACE_TICKS);

    public WorldClearState(GameContext ctx, World world) {
        this.ctx = ctx;
        this.world = world;
    }

    @Override
    public void update(Controls c) {
        if (!grace.tick() || !c.wasPressed(Action.CONFIRM)) return;
        ctx.states().replaceAll(new MainMenuState(ctx));
        ctx.states().push(new WorldSelectState(ctx));
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(MainMenuState.BACKGROUND);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        MainMenuState.drawCentered(g, "WORLD " + world.number() + " CLEAR", BIG, GOLD, 160);
        MainMenuState.drawCentered(g, "Enter: continue", SMALL, MainMenuState.TEXT, 220);
    }
}
