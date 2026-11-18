package ambot.save;

/** An in-memory {@link SaveStore} for tests: remembers the last save and counts saves. */
public final class MemorySaveStore implements SaveStore {
    private SaveData data;
    private int saves;

    public MemorySaveStore() {
        this(SaveData.defaults());
    }

    public MemorySaveStore(SaveData start) {
        this.data = start;
    }

    @Override
    public SaveData load() {
        return data;
    }

    @Override
    public void save(SaveData d) {
        data = d;
        saves++;
    }

    public SaveData saved() {
        return data;
    }

    public int saves() {
        return saves;
    }
}
