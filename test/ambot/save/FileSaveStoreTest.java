package ambot.save;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSaveStoreTest {
    @TempDir
    Path dir;

    @Test
    void roundTripsEveryField() {
        Path file = dir.resolve("AMBOT/save.properties");
        SaveData d = new SaveData(Set.of("w1-1", "w1-2"), 3, 9, true);
        new FileSaveStore(file).save(d);
        assertEquals(d, new FileSaveStore(file).load());
    }

    @Test
    void missingFileGivesDefaults() {
        assertEquals(SaveData.defaults(), new FileSaveStore(dir.resolve("nope.properties")).load());
    }

    @Test
    void brokenFileFallsBackToDefaults() throws IOException {
        Path file = dir.resolve("save.properties");
        Files.write(file, new byte[] { 0, 'g', 'a', 'r', 'b', 'a', 'g', 'e', '=' });
        assertEquals(SaveData.defaults(), assertDoesNotThrow(() -> new FileSaveStore(file).load()));
    }

    @Test
    void outOfRangeValuesAreClamped() throws IOException {
        Path file = dir.resolve("save.properties");
        Files.writeString(file, "version=1\nmusic=-4\nsfx=99\nfullscreen=maybe\n", StandardCharsets.ISO_8859_1);
        SaveData d = new FileSaveStore(file).load();
        assertEquals(0, d.musicVolume());
        assertEquals(10, d.sfxVolume());
        assertFalse(d.fullscreen());
    }

    @Test
    void nonNumbersFallBackPerKey() throws IOException {
        Path file = dir.resolve("save.properties");
        Files.writeString(file, "music=loud\nsfx=2\n", StandardCharsets.ISO_8859_1);
        SaveData d = new FileSaveStore(file).load();
        assertEquals(SaveData.defaults().musicVolume(), d.musicVolume());
        assertEquals(2, d.sfxVolume());
    }

    @Test
    void unknownLevelIdsAreKeptButHarmless() throws IOException {
        Path file = dir.resolve("save.properties");
        Files.writeString(file, "completed=w1-1, zz-9 ,,\n", StandardCharsets.ISO_8859_1);
        assertEquals(Set.of("w1-1", "zz-9"), new FileSaveStore(file).load().completed());
    }

    @Test
    void saveFailureDoesNotThrow() throws IOException {
        Path notAFolder = dir.resolve("blocker");
        Files.writeString(notAFolder, "I am a file");
        FileSaveStore store = new FileSaveStore(notAFolder.resolve("save.properties"));
        assertDoesNotThrow(() -> store.save(SaveData.defaults().withCompleted("w1-1")));
    }

    @Test
    void noTempFileLeftBehind() throws IOException {
        Path file = dir.resolve("save.properties");
        new FileSaveStore(file).save(SaveData.defaults());
        try (var files = Files.list(dir)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void defaultFileHonoursSystemProperty() {
        String old = System.getProperty("ambot.save");
        try {
            System.setProperty("ambot.save", dir.resolve("custom.properties").toString());
            assertEquals(dir.resolve("custom.properties"), FileSaveStore.defaultFile());
        } finally {
            if (old == null) System.clearProperty("ambot.save");
            else System.setProperty("ambot.save", old);
        }
    }
}
