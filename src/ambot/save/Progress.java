package ambot.save;

/** The player's progress and settings for this run, saved after every change (spec section 5.9). */
public final class Progress {
    private final SaveStore store;
    private SaveData data;

    public Progress(SaveStore store) {
        this.store = store;
        this.data = store.load();
    }

    public SaveData data() {
        return data;
    }

    public boolean hasProgress() {
        return !data.completed().isEmpty();
    }

    public void markCompleted(String levelId) {
        update(data.withCompleted(levelId));
    }

    public void setMusicVolume(int volume) {
        update(data.withMusic(volume));
    }

    public void setSfxVolume(int volume) {
        update(data.withSfx(volume));
    }

    public void setFullscreen(boolean on) {
        update(data.withFullscreen(on));
    }

    /** New Game: forget completed levels, keep the settings. */
    public void reset() {
        update(data.withoutProgress());
    }

    private void update(SaveData next) {
        if (next.equals(data)) return;
        data = next;
        store.save(data);
    }
}
