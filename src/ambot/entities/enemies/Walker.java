package ambot.entities.enemies;

import java.awt.Color;
import java.awt.Graphics2D;

import ambot.core.GameConfig;
import ambot.entities.Facing;
import ambot.level.TileMap;

/** World 1 enemy: patrols left and right, turning at walls and at the edge of its ground. */
public final class Walker extends Enemy {
    public static final int W = 16, H = 12, HEALTH = 2;
    public static final double SPEED = 0.6;
    private static final Color BODY = new Color(0x00E436);
    private static final Color EYE = new Color(0x1D2B53);

    /** (x, y) is the top-left of the hitbox. */
    public Walker(double x, double y, String drop) {
        super(x, y, W, H, HEALTH, drop);
        facing = Facing.LEFT;
    }

    @Override
    protected void think(EnemyWorld w) {
        if (blockedX || (onGround && !groundAhead(w.map()))) {
            facing = facing == Facing.RIGHT ? Facing.LEFT : Facing.RIGHT;
        }
        vx = facing == Facing.RIGHT ? SPEED : -SPEED;
    }

    /** Is there solid or one-way ground just below the leading bottom corner? */
    private boolean groundAhead(TileMap map) {
        int t = GameConfig.TILE;
        double probeX = facing == Facing.RIGHT ? x + width + 0.5 : x - 0.5;
        int tx = (int) Math.floor(probeX / t);
        int ty = (int) Math.floor((y + height + 1) / t);
        return map.isSolid(tx, ty) || map.isOneWay(tx, ty);
    }

    @Override
    public void render(Graphics2D g) {
        if (isDead()) return;
        int ix = (int) Math.round(x), iy = (int) Math.round(y);
        g.setColor(flashing() ? FLASH : BODY);
        g.fillRoundRect(ix, iy + 2, W, H - 2, 8, 8);
        g.fillRect(ix + 1, iy + H - 3, W - 2, 3);
        g.setColor(EYE);
        int eyeX = facing == Facing.RIGHT ? ix + 9 : ix + 3;
        g.fillRect(eyeX, iy + 4, 2, 2);
        g.fillRect(eyeX + 3, iy + 4, 2, 2);
    }
}
