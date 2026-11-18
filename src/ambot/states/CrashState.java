package ambot.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import ambot.core.GameConfig;
import ambot.input.Action;
import ambot.input.Controls;

/** Shown when the game hits an unexpected error, instead of freezing: the error, then menu or quit. */
public final class CrashState implements GameState {
    static final int WRAP = 70, MAX_LINES = 3;
    private static final Color BACKGROUND = new Color(0x1D2B53);
    private static final Color TEXT = new Color(0xFFF1E8);
    private static final Color DETAIL = new Color(0xC2C3C7);
    private static final Font TITLE = new Font(Font.MONOSPACED, Font.BOLD, 20);
    private static final Font BODY = new Font(Font.MONOSPACED, Font.PLAIN, 10);

    private final GameContext ctx;
    private final String kind;
    private final List<String> message;

    public CrashState(GameContext ctx, RuntimeException e) {
        this.ctx = ctx;
        this.kind = e.getClass().getSimpleName();
        this.message = wrap(e.getMessage(), WRAP, MAX_LINES);
    }

    @Override
    public void update(Controls c) {
        if (c.wasPressed(Action.CONFIRM)) ctx.states().replaceAll(new MainMenuState(ctx));
        else if (c.wasPressed(Action.BACK)) ctx.quit();
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        MainMenuState.drawCentered(g, "Something went wrong.", TITLE, TEXT, 120);
        MainMenuState.drawCentered(g, kind, BODY, DETAIL, 150);
        int y = 168;
        for (String line : message) {
            MainMenuState.drawCentered(g, line, BODY, DETAIL, y);
            y += 13;
        }
        MainMenuState.drawCentered(g, "Enter: main menu   Esc: quit", BODY, TEXT, 240);
    }

    /** Splits text into at most {@code maxLines} lines of at most {@code width} characters, ending in "..." if cut. */
    static List<String> wrap(String text, int width, int maxLines) {
        if (text == null || text.isBlank()) return List.of("(no message)");
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            while (word.length() > width) { // a single very long word
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) lines.add(line.toString());
        if (lines.size() > maxLines) {
            lines = new ArrayList<>(lines.subList(0, maxLines));
            String last = lines.get(maxLines - 1);
            lines.set(maxLines - 1, (last.length() > width - 3 ? last.substring(0, width - 3) : last) + "...");
        }
        return lines;
    }
}
