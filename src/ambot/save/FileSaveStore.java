package ambot.save;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Saves to a properties file, by default {@code %APPDATA%\AMBOT\save.properties} (spec section 5.9).
 * Anything missing or unreadable falls back to the default value for that key.
 */
public final class FileSaveStore implements SaveStore {
    /** Overrides the save file location (used by the smoke run and for testing a build). */
    public static final String PATH_PROPERTY = "ambot.save";

    private final Path file;

    public FileSaveStore(Path file) {
        this.file = file;
    }

    public static Path defaultFile() {
        String override = System.getProperty(PATH_PROPERTY);
        if (override != null && !override.isBlank()) return Path.of(override);
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) return Path.of(appData, "AMBOT", "save.properties");
        return Path.of(System.getProperty("user.home"), ".ambot", "save.properties");
    }

    public Path file() {
        return file;
    }

    @Override
    public SaveData load() {
        SaveData d = SaveData.defaults();
        if (!Files.isRegularFile(file)) return d;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("AMBOT: could not read " + file + " (" + e.getMessage() + "); starting fresh");
            return d;
        }
        Set<String> completed = Arrays.stream(p.getProperty("completed", "").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        return new SaveData(completed,
                intOr(p, "music", d.musicVolume()),
                intOr(p, "sfx", d.sfxVolume()),
                Boolean.parseBoolean(p.getProperty("fullscreen", "false").trim()));
    }

    @Override
    public void save(SaveData d) {
        Properties p = new Properties();
        p.setProperty("version", "1");
        p.setProperty("completed", String.join(",", d.completed()));
        p.setProperty("music", Integer.toString(d.musicVolume()));
        p.setProperty("sfx", Integer.toString(d.sfxVolume()));
        p.setProperty("fullscreen", Boolean.toString(d.fullscreen()));
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            try (OutputStream out = Files.newOutputStream(tmp)) {
                p.store(out, "AMBOT save");
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException e) {
            System.err.println("AMBOT: could not save to " + file + " (" + e + "); progress is kept for this session");
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException | RuntimeException ignored) {
                // nothing more to do
            }
        }
    }

    private static int intOr(Properties p, String key, int fallback) {
        String v = p.getProperty(key);
        if (v == null) return fallback;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
