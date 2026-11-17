package ambot.states;

import ambot.gfx.SpriteSheet;

/** What screens need from the running game. Tests supply a fake. */
public interface GameContext {
    StateManager states();

    SpriteSheet playerSheet();

    void quit();
}
