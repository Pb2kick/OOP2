package ambot.save;

/** Where {@link SaveData} lives between runs. */
public interface SaveStore {
    /** The saved data, or defaults when there is none or it can't be read. Never throws. */
    SaveData load();

    /** Saves the data. Never throws: a failure is reported on stderr and play goes on. */
    void save(SaveData d);
}
