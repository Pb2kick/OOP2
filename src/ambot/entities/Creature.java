package ambot.entities;

import ambot.gfx.Animator;
import ambot.level.TileCollider;
import ambot.level.TileMap;

/** An entity that moves under gravity, collides with tiles and is animated. */
public abstract class Creature extends Entity {
    protected double vx, vy;
    protected Facing facing = Facing.RIGHT;
    protected boolean onGround;
    protected final Animator animator = new Animator();

    protected Creature(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    /** Applies gravity, then moves horizontally and vertically against the tile map. */
    protected void applyGravityAndMove(TileMap map) {
        applyGravityAndMove(map, false);
    }

    /** As {@link #applyGravityAndMove(TileMap)}; {@code ignoreOneWay} lets the creature drop through one-way platforms. */
    protected void applyGravityAndMove(TileMap map, boolean ignoreOneWay) {
        vy = Math.min(vy + Physics.GRAVITY, Physics.MAX_FALL);

        TileCollider.Move mx = TileCollider.moveX(hitbox(), vx, map);
        x = mx.position();
        if (mx.blocked()) vx = 0;

        TileCollider.Move my = TileCollider.moveY(hitbox(), vy, map, ignoreOneWay);
        y = my.position();
        if (my.blocked()) vy = 0;

        onGround = TileCollider.onGround(hitbox(), map, ignoreOneWay);
    }

    public double vx() {
        return vx;
    }

    public double vy() {
        return vy;
    }

    public Facing facing() {
        return facing;
    }

    public boolean onGround() {
        return onGround;
    }

    public Animator animator() {
        return animator;
    }
}
