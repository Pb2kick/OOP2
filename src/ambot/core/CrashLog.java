package ambot.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import ambot.save.FileSaveStore;

/**
 * Appends crash details to {@code crash.log} next to the save file, so a player running the
 * packaged game (which has no console) can send them in.
 */
public final class CrashLog {
    private CrashLog() {
    }

    /** The folder the save file lives in, e.g. {@code %APPDATA%\AMBOT}. */
    public static Path defaultDir() {
        Path parent = FileSaveStore.defaultFile().toAbsolutePath().getParent();
        return parent != null ? parent : Path.of(".");
    }

    /** Appends a timestamped stack trace; returns the log file, or null if it couldn't be written. */
    public static Path write(Path dir, Throwable e) {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve("crash.log");
            StringWriter trace = new StringWriter();
            e.printStackTrace(new PrintWriter(trace));
            String entry = "=== " + LocalDateTime.now() + "  AMBOT v" + GameConfig.VERSION
                    + "  Java " + System.getProperty("java.version") + System.lineSeparator()
                    + trace + System.lineSeparator();
            Files.writeString(file, entry, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return file;
        } catch (IOException | RuntimeException failure) {
            System.err.println("AMBOT: could not write the crash log (" + failure + ")");
            return null;
        }
    }
}
