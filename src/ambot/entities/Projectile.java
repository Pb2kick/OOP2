package ambot.entities;

import java.awt.Color;
import java.awt.Graphics2D;

import ambot.level.TileMap;

/** A bolt flying straight sideways. It stops at the first solid tile or after {@link #MAX_DISTANCE}. */
public final class Projectile extends Entity {
    public static final double MAX_DISTANCE = 320;
    public static final int W = 6, H = 3;
    private static final Color BOLT = new Color(0xFFA300);

    private final double vx;
    private double travelled;
    private boolean alive = true;

    public Projectile(double x, double y, double vx) {
        super(x, y, W, H);
        this.vx = vx;
    }

    public void update(TileMap map) {
        if (!alive) return;
        x += vx;
        travelled += Math.abs(vx);
        boolean offMap = x + W < 0 || x > map.widthPx();
        if (travelled >= MAX_DISTANCE || offMap || map.overlaps(hitbox(), TileMap.SOLID)) alive = false;
    }

    public boolean alive() {
        return alive;
    }

    public void kill() {
        alive = false;
    }

    public int damage() {
        return 1;
    }

    public double vx() {
        return vx;
    }

    @Override
    public void render(Graphics2D g) {
        if (!alive) return;
        g.setColor(BOLT);
        g.fillRect((int) Math.round(x), (int) Math.round(y), W, H);
    }
}
