package ambot.states;

import ambot.audio.Audio;
import ambot.gfx.SpriteSheet;
import ambot.level.Campaign;
import ambot.save.Progress;

/** What screens need from the running game. Tests supply a fake. */
public interface GameContext {
    StateManager states();

    SpriteSheet playerSheet();

    void quit();

    /** Saved progress and settings; every change is written to disk at once. */
    Progress progress();

    Audio audio();

    Campaign campaign();

    /** Switches the window to borderless fullscreen or back. */
    void setFullscreen(boolean on);
}
