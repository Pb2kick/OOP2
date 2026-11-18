package ambot.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class ProgressTest {
    private final MemorySaveStore store = new MemorySaveStore();
    private final Progress progress = new Progress(store);

    @Test
    void markCompletedSavesOnce() {
        progress.markCompleted("w1-1");
        progress.markCompleted("w1-1");
        assertEquals(1, store.saves());
        assertEquals(Set.of("w1-1"), store.saved().completed());
    }

    @Test
    void settingsSaveImmediately() {
        progress.setMusicVolume(2);
        progress.setSfxVolume(4);
        progress.setFullscreen(true);
        assertEquals(3, store.saves());
        assertEquals(new SaveData(Set.of(), 2, 4, true), store.saved());
        progress.setMusicVolume(2); // no change, no save
        assertEquals(3, store.saves());
    }

    @Test
    void resetKeepsSettings() {
        progress.setMusicVolume(1);
        progress.markCompleted("w1-1");
        progress.reset();
        assertEquals(Set.of(), progress.data().completed());
        assertEquals(1, progress.data().musicVolume());
        assertEquals(store.saved(), progress.data());
    }

    @Test
    void hasProgressOnlyAfterACompletion() {
        assertFalse(progress.hasProgress());
        progress.setSfxVolume(3);
        assertFalse(progress.hasProgress());
        progress.markCompleted("w1-1");
        assertTrue(progress.hasProgress());
    }
}
