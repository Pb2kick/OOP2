package ambot.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

import ambot.input.Action;
import ambot.input.Controls;

/** A vertical keyboard menu: UP/DOWN move (wrapping, skipping disabled items), CONFIRM runs. */
public final class Menu {
    private static final Font FONT = new Font(Font.MONOSPACED, Font.BOLD, 14);
    private static final Color NORMAL = new Color(0xFFF1E8);
    private static final Color SELECTED = new Color(0xFFEC27);
    private static final Color DISABLED = new Color(0x83769C);
    private static final int LINE_HEIGHT = 20;

    private final List<MenuItem> items;
    private int selected;

    public Menu(List<MenuItem> items) {
        this.items = List.copyOf(items);
        selected = -1;
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i).enabled()) {
                selected = i;
                break;
            }
        }
        if (selected < 0) {
            throw new IllegalArgumentException("a menu needs at least one enabled item");
        }
    }

    public void update(Controls c) {
        if (c.wasPressed(Action.DOWN)) move(1);
        if (c.wasPressed(Action.UP)) move(-1);
        if (c.wasPressed(Action.CONFIRM)) items.get(selected).action().run();
    }

    private void move(int dir) {
        int i = selected;
        do {
            i = Math.floorMod(i + dir, items.size());
        } while (!items.get(i).enabled());
        selected = i;
    }

    public int selectedIndex() {
        return selected;
    }

    public void render(Graphics2D g, int centerX, int topY) {
        g.setFont(FONT);
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            String text = (i == selected ? "> " : "  ") + item.label();
            g.setColor(!item.enabled() ? DISABLED : i == selected ? SELECTED : NORMAL);
            g.drawString(text, centerX - fm.stringWidth(text) / 2, topY + i * LINE_HEIGHT);
        }
    }
}
