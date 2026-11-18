package ambot.entities.enemies;

import java.awt.Color;
import java.awt.Graphics2D;

import ambot.entities.Facing;
import ambot.entities.Projectile;
import ambot.level.Rect;

/**
 * World 1 enemy: a stationary turret that turns to face a nearby player and fires bolts at
 * standing-body height, so crouching or jumping dodges them.
 */
public final class Shooter extends Enemy {
    public static final int W = 14, H = 22, HEALTH = 2;
    public static final int RANGE_X = 160, RANGE_Y = 48;
    public static final int FIRST_SHOT_DELAY = 60, FIRE_INTERVAL = 120;
    /** Height of the bolt's top edge above the shooter's feet. */
    public static final int MUZZLE_HEIGHT = 22;
    public static final double BOLT_SPEED = 2.5;
    private static final Color BODY = new Color(0x83769C);
    private static final Color BARREL = new Color(0x5F574F);

    /** Ticks until the next shot, or -1 while the player is out of range. */
    private int timer = -1;

    /** (x, y) is the top-left of the hitbox. */
    public Shooter(double x, double y, String drop) {
        super(x, y, W, H, HEALTH, drop);
    }

    @Override
    protected void think(EnemyWorld w) {
        vx = 0;
        Rect p = w.player();
        double dx = (p.x() + p.w() / 2) - (x + width / 2);
        double dy = (p.y() + p.h() / 2) - (y + height / 2);
        if (Math.abs(dx) > RANGE_X || Math.abs(dy) > RANGE_Y) {
            timer = -1;
            return;
        }
        facing = dx >= 0 ? Facing.RIGHT : Facing.LEFT;
        if (timer < 0) timer = FIRST_SHOT_DELAY;
        if (--timer == 0) {
            double boltX = facing == Facing.RIGHT ? x + width : x - Projectile.W;
            double boltY = y + height - MUZZLE_HEIGHT;
            w.fire().accept(new Projectile(boltX, boltY, facing == Facing.RIGHT ? BOLT_SPEED : -BOLT_SPEED));
            timer = FIRE_INTERVAL;
        }
    }

    @Override
    public void render(Graphics2D g) {
        if (isDead()) return;
        int ix = (int) Math.round(x), iy = (int) Math.round(y);
        g.setColor(flashing() ? FLASH : BODY);
        g.fillRect(ix, iy + 4, W, H - 4);
        g.fillRect(ix + 3, iy, W - 6, 4);
        g.setColor(BARREL);
        int barrelY = iy + H - MUZZLE_HEIGHT;
        if (facing == Facing.RIGHT) g.fillRect(ix + W - 2, barrelY, 6, 3);
        else g.fillRect(ix - 4, barrelY, 6, 3);
    }
}
