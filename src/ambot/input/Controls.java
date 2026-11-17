package ambot.input;

import java.util.EnumSet;
import java.util.Set;

/** Immutable snapshot of input for one game update. */
public final class Controls {
    public static final Controls NONE = of(Set.of(), Set.of(), Set.of());

    private final Set<Action> held, pressed, released;

    private Controls(Set<Action> held, Set<Action> pressed, Set<Action> released) {
        this.held = held;
        this.pressed = pressed;
        this.released = released;
    }

    public static Controls of(Set<Action> held, Set<Action> pressed, Set<Action> released) {
        return new Controls(copy(held), copy(pressed), copy(released));
    }

    private static Set<Action> copy(Set<Action> s) {
        return s.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(s));
    }

    /** True while the action is held down. */
    public boolean isHeld(Action a) {
        return held.contains(a);
    }

    /** True if the action went down since the previous update (even if already released). */
    public boolean wasPressed(Action a) {
        return pressed.contains(a);
    }

    /** True if the action went up since the previous update. */
    public boolean wasReleased(Action a) {
        return released.contains(a);
    }
}
