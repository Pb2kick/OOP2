package ambot.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CrashLogTest {
    @TempDir
    Path dir;

    @Test
    void writesStackTrace() throws IOException {
        Path log = CrashLog.write(dir.resolve("AMBOT"), new IllegalStateException("boom in the boss fight"));
        assertNotNull(log);
        String text = Files.readString(log, StandardCharsets.UTF_8);
        assertTrue(text.contains("boom in the boss fight"), text);
        assertTrue(text.contains("at "), "has a stack trace");
        assertTrue(text.contains(GameConfig.VERSION), "names the version");
    }

    @Test
    void unwritableDirReturnsNull() throws IOException {
        Path notAFolder = dir.resolve("blocker");
        Files.writeString(notAFolder, "a file");
        assertNull(CrashLog.write(notAFolder, new RuntimeException("x")));
    }
}
