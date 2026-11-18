package ambot.states;

/**
 * Ignores input for the first few ticks of a screen, so a key pressed just before it opened
 * (Space is both Jump and Confirm) can't pick a menu item by accident.
 */
public final class InputGrace {
    public static final int GRACE_TICKS = 20;

    private final int ticks;
    private int count;

    public InputGrace(int ticks) {
        this.ticks = ticks;
    }

    /** Counts one tick; true from the {@code ticks}-th call on. */
    public boolean tick() {
        if (count < ticks) count++;
        return count >= ticks;
    }
}
