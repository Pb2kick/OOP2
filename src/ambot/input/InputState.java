package ambot.input;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Collects input events from the Swing event thread and hands the game loop a
 * consistent {@link Controls} snapshot once per update. All methods are synchronized.
 */
public final class InputState {
    private final KeyBindings bindings;
    private final Set<Integer> heldKeys = new HashSet<>();
    private final Set<Action> heldDirect = EnumSet.noneOf(Action.class);
    private final Set<Action> pressed = EnumSet.noneOf(Action.class);
    private final Set<Action> released = EnumSet.noneOf(Action.class);

    public InputState(KeyBindings bindings) {
        this.bindings = bindings;
    }

    public synchronized void keyPressed(int keyCode) {
        Set<Action> before = held();
        if (heldKeys.add(keyCode)) {
            recordTransitions(before);
        }
    }

    public synchronized void keyReleased(int keyCode) {
        Set<Action> before = held();
        if (heldKeys.remove(keyCode)) {
            recordTransitions(before);
        }
    }

    /** For inputs that are not keys, such as the left mouse button. */
    public synchronized void actionPressed(Action a) {
        Set<Action> before = held();
        if (heldDirect.add(a)) {
            recordTransitions(before);
        }
    }

    public synchronized void actionReleased(Action a) {
        Set<Action> before = held();
        if (heldDirect.remove(a)) {
            recordTransitions(before);
        }
    }

    /** Forget everything held, e.g. when the window loses focus and key-ups would be missed. */
    public synchronized void releaseAll() {
        heldKeys.clear();
        heldDirect.clear();
    }

    public synchronized Controls poll() {
        Controls c = Controls.of(held(), pressed, released);
        pressed.clear();
        released.clear();
        return c;
    }

    private Set<Action> held() {
        Set<Action> h = EnumSet.noneOf(Action.class);
        h.addAll(heldDirect);
        for (int key : heldKeys) {
            h.addAll(bindings.actionsFor(key));
        }
        return h;
    }

    private void recordTransitions(Set<Action> before) {
        Set<Action> after = held();
        for (Action a : after) {
            if (!before.contains(a)) pressed.add(a);
        }
        for (Action a : before) {
            if (!after.contains(a)) released.add(a);
        }
    }
}
