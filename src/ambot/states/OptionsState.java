package ambot.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import ambot.audio.Sfx;
import ambot.core.GameConfig;
import ambot.input.Action;
import ambot.input.Controls;
import ambot.save.SaveData;

/**
 * Options (spec section 5.3): music and sound volume, fullscreen, and the controls list.
 * Every change is applied at once and saved.
 */
public final class OptionsState implements GameState {
    static final int MUSIC = 0, SOUND = 1, FULLSCREEN = 2, CONTROLS = 3, BACK = 4, ROWS = 5;
    private static final Font TITLE = new Font(Font.MONOSPACED, Font.BOLD, 24);
    private static final Font ROW = new Font(Font.MONOSPACED, Font.BOLD, 14);
    private static final Color SELECTED = new Color(0xFFEC27);

    private final GameContext ctx;
    private int selected = MUSIC;

    public OptionsState(GameContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void update(Controls c) {
        if (c.wasPressed(Action.BACK)) {
            ctx.states().pop();
            return;
        }
        if (c.wasPressed(Action.DOWN)) move(1);
        if (c.wasPressed(Action.UP)) move(-1);
        int dir = (c.wasPressed(Action.RIGHT) ? 1 : 0) - (c.wasPressed(Action.LEFT) ? 1 : 0);
        SaveData d = ctx.progress().data();
        if (dir != 0) {
            switch (selected) {
                case MUSIC -> {
                    ctx.progress().setMusicVolume(d.musicVolume() + dir);
                    ctx.audio().setMusicVolume(ctx.progress().data().musicVolume());
                }
                case SOUND -> {
                    ctx.progress().setSfxVolume(d.sfxVolume() + dir);
                    ctx.audio().setSfxVolume(ctx.progress().data().sfxVolume());
                    ctx.audio().play(Sfx.MENU_MOVE); // a sample at the new volume
                }
                case FULLSCREEN -> toggleFullscreen();
                default -> {
                }
            }
        }
        if (c.wasPressed(Action.CONFIRM)) {
            switch (selected) {
                case FULLSCREEN -> toggleFullscreen();
                case CONTROLS -> ctx.states().push(new ControlsState(ctx));
                case BACK -> ctx.states().pop();
                default -> {
                }
            }
            ctx.audio().play(Sfx.MENU_SELECT);
        }
    }

    private void move(int dir) {
        selected = Math.floorMod(selected + dir, ROWS);
        ctx.audio().play(Sfx.MENU_MOVE);
    }

    private void toggleFullscreen() {
        boolean on = !ctx.progress().data().fullscreen();
        ctx.progress().setFullscreen(on);
        ctx.setFullscreen(on);
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(MainMenuState.BACKGROUND);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        MainMenuState.drawCentered(g, "OPTIONS", TITLE, MainMenuState.TEXT, 80);
        SaveData d = ctx.progress().data();
        String[] labels = {
                "Music  " + bar(d.musicVolume()),
                "Sound  " + bar(d.sfxVolume()),
                "Fullscreen: " + (d.fullscreen() ? "On" : "Off"),
                "Controls",
                "Back" };
        g.setFont(ROW);
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < labels.length; i++) {
            String text = (i == selected ? "> " : "  ") + labels[i];
            g.setColor(i == selected ? SELECTED : MainMenuState.TEXT);
            g.drawString(text, (GameConfig.LOGICAL_WIDTH - fm.stringWidth(text)) / 2, 140 + i * 24);
        }
        MainMenuState.drawCentered(g, "Left / Right: change   Esc: back", new Font(Font.MONOSPACED, Font.PLAIN, 10),
                MainMenuState.FAINT, 300);
    }

    private static String bar(int volume) {
        return "[" + "#".repeat(volume) + "-".repeat(SaveData.MAX_VOLUME - volume) + "]";
    }
}
