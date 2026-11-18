package ambot.entities.enemies;

import java.awt.Color;

import ambot.entities.Creature;
import ambot.entities.Facing;

/**
 * Base of every enemy (spec section 5.7): an AI hook, contact damage and an optional drop.
 * A hit stuns it briefly, pushes it away and makes it flash; a dead enemy lingers for
 * {@link #REMOVE_TICKS} before it is removed.
 */
public abstract class Enemy extends Creature {
    public static final int STUN_TICKS = 12;
    public static final int REMOVE_TICKS = 20;
    static final double STUN_PUSH = 1.5;
    protected static final Color FLASH = Color.WHITE;

    private final String drop;
    private int stunTicks, deadTicks;
    private int lastSwing = Integer.MIN_VALUE;

    protected Enemy(double x, double y, double w, double h, int maxHealth, String drop) {
        super(x, y, w, h, maxHealth);
        this.drop = drop;
    }

    public final void update(EnemyWorld w) {
        if (isDead()) {
            deadTicks++;
            vx = 0;
        } else if (stunTicks > 0) {
            stunTicks--; // keep the push from onDamaged
        } else {
            think(w);
        }
        applyGravityAndMove(w.map());
    }

    /** Decides movement (sets {@code vx}, {@code facing}) and may fire, once per tick when not stunned. */
    protected abstract void think(EnemyWorld w);

    public int contactDamage() {
        return 1;
    }

    /** {@code "heart"} or null. */
    public String drop() {
        return drop;
    }

    /** True the first time a given sword swing reaches this enemy, false after that. */
    public boolean hitBySwing(int swingId) {
        if (swingId == lastSwing) return false;
        lastSwing = swingId;
        return true;
    }

    public boolean isRemovable() {
        return isDead() && deadTicks >= REMOVE_TICKS;
    }

    @Override
    protected boolean isInvincible() {
        return stunTicks > 0;
    }

    @Override
    protected void onDamaged(double fromX) {
        stunTicks = STUN_TICKS;
        double centre = x + width / 2;
        double away = centre == fromX ? (facing == Facing.RIGHT ? -1 : 1) : Math.signum(centre - fromX);
        vx = STUN_PUSH * away;
        if (isDead()) deadTicks = 0;
    }

    /** True on alternate frames while stunned, for a white hit flash. */
    protected boolean flashing() {
        return stunTicks > 0 && (stunTicks / 2) % 2 == 0;
    }
}
