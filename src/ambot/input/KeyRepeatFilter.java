package ambot.input;

import java.util.HashSet;
import java.util.Set;

/** Tells a real key press apart from OS auto-repeat, for one-shot keys such as F11 and F3. */
public final class KeyRepeatFilter {
    private final Set<Integer> down = new HashSet<>();

    /** True only for the first press since the key was last released. */
    public boolean firstPress(int keyCode) {
        return down.add(keyCode);
    }

    public void released(int keyCode) {
        down.remove(keyCode);
    }

    /** Forget all keys, e.g. after focus loss when key-up events may have been missed. */
    public void reset() {
        down.clear();
    }
}
