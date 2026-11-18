package ambot.entities;

/** Something with health that can be hurt and healed (spec section 5.7). */
public interface Damageable {
    int health();

    int maxHealth();

    boolean isDead();

    /**
     * Takes damage from a source at horizontal position {@code fromX} (used for knockback).
     *
     * @return false, changing nothing, when already dead or currently invincible
     */
    boolean damage(int amount, double fromX);

    /** Restores health, never above {@link #maxHealth()}. */
    void heal(int amount);
}
