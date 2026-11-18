package ambot.save;

/**
 * The player's progress and settings for this run, saved after every change (spec section 5.9).
 * Synchronized: the game loop and the window's F11 handler both change it.
 */
public final class Progress {
    private final SaveStore store;
    private SaveData data;

    public Progress(SaveStore store) {
        this.store = store;
        this.data = store.load();
    }

    public synchronized SaveData data() {
        return data;
    }

    public synchronized boolean hasProgress() {
        return !data.completed().isEmpty();
    }

    public synchronized void markCompleted(String levelId) {
        update(data.withCompleted(levelId));
    }

    public synchronized void setMusicVolume(int volume) {
        update(data.withMusic(volume));
    }

    public synchronized void setSfxVolume(int volume) {
        update(data.withSfx(volume));
    }

    public synchronized void setFullscreen(boolean on) {
        update(data.withFullscreen(on));
    }

    /** New Game: forget completed levels, keep the settings. */
    public synchronized void reset() {
        update(data.withoutProgress());
    }

    private void update(SaveData next) {
        if (next.equals(data)) return;
        data = next;
        store.save(data);
    }
}
