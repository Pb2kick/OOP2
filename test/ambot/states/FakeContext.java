package ambot.states;

import ambot.entities.player.PlayerAnimations;
import ambot.gfx.SpriteSheet;

/** A stand-in for the running game, for screen tests. Loads the player sheet once, on first use. */
final class FakeContext implements GameContext {
    private static SpriteSheet sheet;

    final StateManager sm = new StateManager();
    boolean quitCalled;

    static synchronized SpriteSheet sheet() {
        if (sheet == null) {
            sheet = SpriteSheet.load(PlayerAnimations.SHEET_PATH, PlayerAnimations.CELL_W, PlayerAnimations.CELL_H);
        }
        return sheet;
    }

    @Override
    public StateManager states() {
        return sm;
    }

    @Override
    public SpriteSheet playerSheet() {
        return sheet();
    }

    @Override
    public void quit() {
        quitCalled = true;
    }
}
