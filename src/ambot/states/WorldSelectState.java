package ambot.states;

import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import ambot.core.GameConfig;
import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.Campaign;
import ambot.level.LevelRef;
import ambot.level.World;
import ambot.save.SaveData;
import ambot.ui.Menu;
import ambot.ui.MenuItem;

/** The worlds and their levels as a list; locked levels are greyed out, finished ones marked "*". */
public final class WorldSelectState implements GameState {
    private static final Font TITLE = new Font(Font.MONOSPACED, Font.BOLD, 24);

    private final GameContext ctx;
    private final List<MenuItem> rows = new ArrayList<>();
    private final Menu menu;

    public WorldSelectState(GameContext ctx) {
        this.ctx = ctx;
        Campaign campaign = ctx.campaign();
        SaveData d = ctx.progress().data();
        for (World w : campaign.worlds()) {
            rows.add(MenuItem.disabled("World " + w.number() + (w.available() ? "" : " - coming soon")));
            if (!w.available()) continue;
            for (int k = 0; k < w.levels().size(); k++) {
                LevelRef ref = w.levels().get(k);
                String label = "  " + w.number() + "-" + (k + 1) + "  " + ref.title()
                        + (d.completed().contains(ref.id()) ? " *" : "");
                int index = k;
                rows.add(campaign.isUnlocked(w, k, d)
                        ? MenuItem.of(label, () -> ctx.states().replaceAll(new PlayingState(ctx, campaign.catalog(w), index)))
                        : MenuItem.disabled(label));
            }
        }
        rows.add(MenuItem.of("Back", () -> ctx.states().pop()));
        menu = new Menu(rows);
    }

    List<MenuItem> rows() {
        return List.copyOf(rows);
    }

    @Override
    public void update(Controls c) {
        if (c.wasPressed(Action.BACK)) {
            ctx.states().pop();
            return;
        }
        menu.update(c, ctx.audio());
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(MainMenuState.BACKGROUND);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        MainMenuState.drawCentered(g, "WORLD SELECT", TITLE, MainMenuState.TEXT, 60);
        menu.render(g, GameConfig.LOGICAL_WIDTH / 2, 100);
    }
}
