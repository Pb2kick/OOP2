package ambot.input;

import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Maps key codes to the actions they trigger. One key may trigger several actions. */
public final class KeyBindings {
    private final Map<Integer, Set<Action>> byKey = new HashMap<>();

    private KeyBindings() {
    }

    /** Default controls from the design (spec §5.5). */
    public static KeyBindings defaults() {
        KeyBindings b = new KeyBindings();
        b.bind(KeyEvent.VK_A, Action.LEFT);
        b.bind(KeyEvent.VK_LEFT, Action.LEFT);
        b.bind(KeyEvent.VK_D, Action.RIGHT);
        b.bind(KeyEvent.VK_RIGHT, Action.RIGHT);
        b.bind(KeyEvent.VK_W, Action.UP, Action.JUMP);
        b.bind(KeyEvent.VK_UP, Action.UP, Action.JUMP);
        b.bind(KeyEvent.VK_S, Action.DOWN);
        b.bind(KeyEvent.VK_DOWN, Action.DOWN);
        b.bind(KeyEvent.VK_SPACE, Action.JUMP, Action.CONFIRM);
        b.bind(KeyEvent.VK_ENTER, Action.CONFIRM);
        b.bind(KeyEvent.VK_J, Action.ATTACK);
        b.bind(KeyEvent.VK_ESCAPE, Action.PAUSE, Action.BACK);
        return b;
    }

    private void bind(int keyCode, Action first, Action... rest) {
        byKey.put(keyCode, Set.copyOf(EnumSet.of(first, rest)));
    }

    public Set<Action> actionsFor(int keyCode) {
        return byKey.getOrDefault(keyCode, Set.of());
    }
}
