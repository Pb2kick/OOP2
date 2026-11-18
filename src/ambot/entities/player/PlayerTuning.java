package ambot.entities.player;

/** How the player feels to control. Speeds are logical pixels per tick at 60 UPS. */
public final class PlayerTuning {
    private PlayerTuning() {
    }

    public static final double RUN_SPEED = 2.0;
    public static final double JUMP_VELOCITY = -6.0;
    /** Releasing jump early caps the upward speed here, giving a short hop. */
    public static final double JUMP_CUT_VELOCITY = -2.0;
    /** A jump always rises at full speed for this many ticks, so even a quick tap gives a visible hop. */
    public static final int MIN_JUMP_TICKS = 4;
    /** Ticks after running off a ledge during which a jump still works. */
    public static final int COYOTE_TICKS = 6;
    /** Ticks a jump press is remembered before landing. */
    public static final int JUMP_BUFFER_TICKS = 6;
    /** Ticks during which one-way platforms are ignored after pressing down on one. */
    public static final int DROP_THROUGH_TICKS = 6;
    /** Health in hearts (spec section 5.4). */
    public static final int MAX_HEARTS = 3;
    /** Ticks the player is knocked back and ignores input after a hit. */
    public static final int HURT_TICKS = 18;
    /** Ticks of invincibility after a hit, "about 1 second" (spec section 5.4). */
    public static final int INVINCIBLE_TICKS = 60;
    public static final double KNOCKBACK_X = 2.5;
    public static final double KNOCKBACK_Y = -4.0;
    /** Ticks from death until Game Over may open. */
    public static final int DEATH_TICKS = 60;
    /** While invincible the sprite is hidden for this many ticks, then shown for as many. */
    public static final int BLINK_TICKS = 4;
    public static final int HITBOX_W = 14;
    public static final int HITBOX_H = 28;
    /** The body's centre column inside the 50-px sheet cell. */
    public static final int SPRITE_CENTER_X = 21;
}
