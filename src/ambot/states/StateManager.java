package ambot.states;

import java.awt.Graphics2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import ambot.input.Controls;

/**
 * A stack of game states. Changes (push, pop, replaceAll) are queued and applied at
 * the start of the next update, so a state can safely change the stack from inside
 * its own update.
 */
public final class StateManager {
    private final Deque<GameState> stack = new ArrayDeque<>();
    private final Deque<Runnable> pending = new ArrayDeque<>();

    public void push(GameState s) {
        pending.add(() -> enter(s));
    }

    public void pop() {
        pending.add(() -> {
            if (stack.size() <= 1) {
                throw new IllegalStateException("cannot pop the last game state");
            }
            stack.removeLast();
        });
    }

    public void replaceAll(GameState s) {
        pending.add(() -> {
            stack.clear();
            enter(s);
        });
    }

    /** Drops every state and every queued change at once, then enters {@code s} (used after a crash). */
    public void reset(GameState s) {
        pending.clear();
        stack.clear();
        enter(s);
    }

    private void enter(GameState s) {
        stack.addLast(s);
        s.onEnter();
    }

    public void update(Controls c) {
        while (!pending.isEmpty()) {
            pending.removeFirst().run();
        }
        GameState top = stack.peekLast();
        if (top != null) {
            top.update(c);
        }
    }

    /** Renders from the topmost non-overlay state upward. */
    public void render(Graphics2D g) {
        List<GameState> states = new ArrayList<>(stack);
        int from = states.size() - 1;
        while (from > 0 && states.get(from).isOverlay()) {
            from--;
        }
        for (int i = Math.max(0, from); i < states.size(); i++) {
            states.get(i).render(g);
        }
    }

    public void focusLost() {
        GameState top = stack.peekLast();
        if (top != null) {
            top.onFocusLost();
        }
    }

    /** The current state, or null when the stack is empty. */
    public GameState top() {
        return stack.peekLast();
    }

    public int size() {
        return stack.size();
    }
}
