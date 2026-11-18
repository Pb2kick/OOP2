package ambot.states;

import ambot.audio.RecordingAudio;
import ambot.entities.player.PlayerAnimations;
import ambot.gfx.SpriteSheet;
import ambot.level.Campaign;
import ambot.save.MemorySaveStore;
import ambot.save.Progress;

/** A stand-in for the running game, for screen tests. Loads the player sheet once, on first use. */
final class FakeContext implements GameContext {
    private static SpriteSheet sheet;

    final StateManager sm = new StateManager();
    final MemorySaveStore store = new MemorySaveStore();
    final Progress progress = new Progress(store);
    final RecordingAudio audio = new RecordingAudio();
    final Campaign campaign = Campaign.standard();
    boolean quitCalled, fullscreen;

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

    @Override
    public Progress progress() {
        return progress;
    }

    @Override
    public RecordingAudio audio() {
        return audio;
    }

    @Override
    public Campaign campaign() {
        return campaign;
    }

    @Override
    public void setFullscreen(boolean on) {
        fullscreen = on;
    }
}
