package ambot.level;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import ambot.tools.AsciiTmx;
import ambot.tools.PlaceholderTiles;

/** Checks the level files on disk. Runs from the project root, where build.ps1 runs. */
class LevelFilesTest {
    static final Path RESOURCES = Path.of("resources");
    static final Path SOURCES = Path.of("levels-src");

    static List<Path> files(Path root, String suffix) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> p.toString().endsWith(suffix)).sorted().toList();
        }
    }

    static String resourcePath(Path file) {
        return "/" + RESOURCES.relativize(file).toString().replace('\\', '/');
    }

    static String lf(String s) {
        return s.replace("\r\n", "\n");
    }

    @Test
    void everyLevelFileIsValid() throws IOException {
        List<Path> levels = files(RESOURCES.resolve("levels"), ".tmx");
        assertFalse(levels.isEmpty(), "no level files found");
        for (Path p : levels) {
            String res = resourcePath(p);
            assertEquals(List.of(), LevelValidator.check(TmxLoader.load(res)), res);
        }
    }

    @Test
    void catalogListsEveryPlaceholderFile() throws IOException {
        Set<String> onDisk = files(RESOURCES.resolve("levels/placeholder"), ".tmx").stream()
                .map(LevelFilesTest::resourcePath).collect(Collectors.toSet());
        assertEquals(onDisk, Set.copyOf(LevelCatalog.PLACEHOLDER_PATHS));
    }

    @Test
    void generatedFilesAreUpToDate() throws IOException {
        List<Path> sources = files(SOURCES, ".txt");
        assertFalse(sources.isEmpty(), "no level sources found");
        for (Path src : sources) {
            String rel = SOURCES.relativize(src).toString().replace('\\', '/');
            Path tmx = RESOURCES.resolve("levels").resolve(rel.substring(0, rel.length() - 4) + ".tmx");
            assertTrue(Files.exists(tmx), tmx + " is missing: run .\\build.cmd levels");
            assertEquals(lf(AsciiTmx.convert(Files.readString(src, UTF_8))), lf(Files.readString(tmx, UTF_8)),
                    tmx + " doesn't match " + src + ". If you changed the .txt, run .\\build.cmd levels."
                            + " If you edited the .tmx in Tiled, running levels would throw those edits away:"
                            + " delete " + src + " instead, so the .tmx becomes the source.");
        }
    }

    @Test
    void placeholderTilesImageIsCurrent() throws IOException {
        BufferedImage onDisk = ImageIO.read(RESOURCES.resolve("tiles/placeholder.png").toFile());
        BufferedImage drawn = PlaceholderTiles.draw();
        assertEquals(drawn.getWidth(), onDisk.getWidth());
        assertEquals(drawn.getHeight(), onDisk.getHeight());
        for (int y = 0; y < drawn.getHeight(); y++) {
            for (int x = 0; x < drawn.getWidth(); x++) {
                assertEquals(drawn.getRGB(x, y), onDisk.getRGB(x, y), "pixel " + x + "," + y + ": run .\\build.cmd levels");
            }
        }
    }

    @Test
    void catalogLoadsEveryLevel() {
        LevelCatalog catalog = LevelCatalog.placeholder();
        assertEquals(LevelCatalog.PLACEHOLDER_PATHS.size(), catalog.size());
        for (int i = 0; i < catalog.size(); i++) assertNotNull(catalog.load(i));
    }
}
