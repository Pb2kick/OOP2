package ambot.level;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.util.Map;

import ambot.tools.AsciiTmx;

/** Builds small levels for tests from ASCII rows, through the real TMX writer and loader. */
public final class TestLevels {
    public static final String PATH = "/levels/test/test.tmx";

    private TestLevels() {
    }

    public static Level ascii(String... rows) {
        return ascii(Map.of("title", "Test", "world", "placeholder"), rows);
    }

    public static Level ascii(Map<String, String> props, String... rows) {
        byte[] tmx = AsciiTmx.toTmx(props, rows).getBytes(UTF_8);
        return TmxLoader.parse(new ByteArrayInputStream(tmx), PATH);
    }
}
