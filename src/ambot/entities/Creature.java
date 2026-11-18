package ambot.entities;

import ambot.gfx.Animator;
import ambot.level.TileCollider;
import ambot.level.TileMap;

/** An entity that moves under gravity, collides with tiles, is animated and has health. */
public abstract class Creature extends Entity implements Damageable {
    protected double vx, vy;
    protected Facing facing = Facing.RIGHT;
    protected boolean onGround;
    protected final Animator animator = new Animator();
    protected final int maxHealth;
    protected int health;

    protected Creature(double x, double y, double width, double height) {
        this(x, y, width, height, 1);
    }

    protected Creature(double x, double y, double width, double height, int maxHealth) {
        super(x, y, width, height);
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    @Override
    public int health() {
        return health;
    }

    @Override
    public int maxHealth() {
        return maxHealth;
    }

    @Override
    public boolean isDead() {
        return health <= 0;
    }

    @Override
    public boolean damage(int amount, double fromX) {
        if (amount <= 0 || isDead() || isInvincible()) return false;
        health = Math.max(0, health - amount);
        onDamaged(fromX);
        return true;
    }

    @Override
    public void heal(int amount) {
        if (amount <= 0 || isDead()) return;
        health = Math.min(maxHealth, health + amount);
    }

    /** While true, {@link #damage} does nothing. */
    protected boolean isInvincible() {
        return false;
    }

    /** Called after health has dropped; {@code fromX} is where the hit came from. */
    protected abstract void onDamaged(double fromX);

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
