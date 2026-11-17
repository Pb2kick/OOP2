package ambot.states;

import java.awt.Graphics2D;

import ambot.input.Controls;

/** One screen of the game (menu, playing, paused, ...), managed by {@link StateManager}. */
public interface GameState {
    void update(Controls c);

    void render(Graphics2D g);

    /** Called when the state becomes the top of the stack by push or replaceAll. */
    default void onEnter() {
    }

    /** Called when the game window loses focus while this state is on top. */
    default void onFocusLost() {
    }

    /** Overlay states (e.g. pause) are drawn on top of the state below them. */
    default boolean isOverlay() {
        return false;
    }
}
