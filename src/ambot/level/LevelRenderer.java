package ambot.level;

import java.awt.Color;
import java.awt.Graphics2D;

import ambot.core.GameConfig;

/**
 * Draws a level's tile layers and its checkpoint and exit markers, in level pixel
 * coordinates (the caller translates by the camera). Only tiles inside the given view are
 * drawn, so big levels cost no more than one screen.
 */
public final class LevelRenderer {
    private static final int T = GameConfig.TILE;
    private static final Color POLE = new Color(0xC2C3C7);
    private static final Color FLAG = new Color(0x83769C);
    private static final Color FLAG_ACTIVE = new Color(0xFFEC27);
    private static final Color DOOR_FRAME = new Color(0x29ADFF);
    private static final Color DOOR_INSIDE = new Color(0x1D2B53);

    private final Level level;

    public LevelRenderer(Level level) {
        this.level = level;
    }

    /** Background, solid, one-way and hazard layers: everything behind the player. */
    public void drawBehind(Graphics2D g, Rect view) {
        drawLayer(g, Level.BACKGROUND, view);
        drawLayer(g, Level.SOLID, view);
        drawLayer(g, Level.ONEWAY, view);
        drawLayer(g, Level.HAZARD, view);
    }

    /** The foreground layer, drawn over the player. */
    public void drawFront(Graphics2D g, Rect view) {
        drawLayer(g, Level.FOREGROUND, view);
    }

    /** Checkpoints as flags (lit when {@code activeCheckpoint}, which may be null) and the exit as a door. */
    public void drawMarkers(Graphics2D g, Rect activeCheckpoint) {
        for (Rect cp : level.checkpoints()) {
            int x = (int) cp.x(), y = (int) cp.y(), h = (int) cp.h();
            g.setColor(POLE);
            g.fillRect(x + 3, y + 2, 2, h - 2);
            g.setColor(cp.equals(activeCheckpoint) ? FLAG_ACTIVE : FLAG);
            g.fillRect(x + 5, y + 3, 8, 6);
        }
        for (LevelObject exit : level.objects(LevelObject.EXIT)) {
            Rect r = exit.bounds();
            int x = (int) r.x(), y = (int) r.y(), w = (int) r.w(), h = (int) r.h();
            g.setColor(DOOR_FRAME);
            g.fillRect(x, y, w, h);
            g.setColor(DOOR_INSIDE);
            g.fillRect(x + 2, y + 2, w - 4, h - 2);
        }
    }

    private void drawLayer(Graphics2D g, String name, Rect view) {
        TileLayer layer = level.layer(name).orElse(null);
        if (layer == null) return;
        int tx0 = Math.max(0, (int) Math.floor(view.x() / T));
        int ty0 = Math.max(0, (int) Math.floor(view.y() / T));
        int tx1 = Math.min(layer.width() - 1, (int) Math.floor((view.right() - 1) / T));
        int ty1 = Math.min(layer.height() - 1, (int) Math.floor((view.bottom() - 1) / T));
        Tileset tiles = level.tileset();
        for (int ty = ty0; ty <= ty1; ty++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                int gid = layer.gid(tx, ty);
                if (gid != 0 && tiles.contains(gid)) g.drawImage(tiles.tile(gid), tx * T, ty * T, null);
            }
        }
    }
}
