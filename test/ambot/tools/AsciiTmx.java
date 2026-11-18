package ambot.tools;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

/**
 * Turns levels drawn as ASCII art into Tiled-compatible .tmx files (see docs/LEVELS.md).
 * Not shipped. {@code main(levels-src, resources)} converts every levels-src/**&#47;*.txt into
 * resources/levels/**&#47;*.tmx and rewrites the placeholder tileset image.
 *
 * <pre>
 * #  solid      -  one-way platform   ^  hazard (spikes)
 * b  background decoration            f  foreground decoration
 * P  player spawn (feet on the cell bottom)
 * C  checkpoint, E  exit (16x32, bottom on the cell bottom)
 * .  empty
 * </pre>
 */
public final class AsciiTmx {
    private static final int T = 16;
    private static final String[] LAYERS = { "background", "solid", "oneway", "hazard", "foreground" };

    private AsciiTmx() {
    }

    /** A source file: optional {@code key: value} header lines, a {@code ---} line, then the rows. */
    public static String convert(String sourceText) {
        List<String> lines = new ArrayList<>(List.of(sourceText.replace("\r\n", "\n").replace('\r', '\n').split("\n")));
        Map<String, String> props = new LinkedHashMap<>();
        int sep = lines.indexOf("---");
        if (sep >= 0) {
            for (String h : lines.subList(0, sep)) {
                if (h.isBlank()) continue;
                int colon = h.indexOf(':');
                if (colon <= 0) throw new IllegalArgumentException("header line '" + h + "' should be 'key: value'");
                props.put(h.substring(0, colon).trim(), h.substring(colon + 1).trim());
            }
            lines = lines.subList(sep + 1, lines.size());
        }
        List<String> rows = lines.stream().filter(l -> !l.isBlank()).toList();
        return toTmx(props, rows.toArray(String[]::new));
    }

    public static String toTmx(Map<String, String> properties, String... rows) {
        if (rows.length == 0 || rows[0].isEmpty()) throw new IllegalArgumentException("a level needs at least one row");
        int w = rows[0].length(), h = rows.length;
        int[][] layers = new int[LAYERS.length][w * h];
        List<String> objects = new ArrayList<>();
        int nextId = 1;
        for (int ty = 0; ty < h; ty++) {
            if (rows[ty].length() != w) {
                throw new IllegalArgumentException("row " + ty + " has length " + rows[ty].length() + ", expected " + w);
            }
            for (int tx = 0; tx < w; tx++) {
                int i = ty * w + tx;
                char c = rows[ty].charAt(tx);
                switch (c) {
                    case '.' -> { }
                    case '#' -> layers[1][i] = ty > 0 && rows[ty - 1].charAt(tx) == '#'
                            ? PlaceholderTiles.GROUND : PlaceholderTiles.GROUND_TOP;
                    case '-' -> layers[2][i] = PlaceholderTiles.ONEWAY;
                    case '^' -> layers[3][i] = PlaceholderTiles.SPIKES;
                    case 'b' -> layers[0][i] = PlaceholderTiles.BACK_WALL;
                    case 'f' -> layers[4][i] = PlaceholderTiles.GRASS;
                    case 'P' -> objects.add("  <object id=\"" + nextId++ + "\" type=\"player_spawn\" x=\"" + (tx * T + T / 2)
                            + "\" y=\"" + (ty + 1) * T + "\">\n   <point/>\n  </object>\n");
                    case 'C', 'E' -> objects.add("  <object id=\"" + nextId++ + "\" type=\""
                            + (c == 'C' ? "checkpoint" : "exit") + "\" x=\"" + tx * T + "\" y=\"" + (ty - 1) * T
                            + "\" width=\"" + T + "\" height=\"" + 2 * T + "\"/>\n");
                    case 'w', 'W', 's' -> objects.add(pointObject(nextId++, "enemy", tx, ty,
                            c == 's' ? "shooter" : "walker", c == 'W' ? "heart" : null));
                    case 'h' -> objects.add(pointObject(nextId++, "pickup", tx, ty, "heart", null));
                    default -> throw new IllegalArgumentException("unknown level char '" + c + "' at " + tx + "," + ty);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<map version=\"1.10\" tiledversion=\"1.10.2\" orientation=\"orthogonal\" renderorder=\"right-down\"")
                .append(" width=\"").append(w).append("\" height=\"").append(h)
                .append("\" tilewidth=\"16\" tileheight=\"16\" infinite=\"0\" nextlayerid=\"").append(LAYERS.length + 2)
                .append("\" nextobjectid=\"").append(nextId).append("\">\n");
        if (!properties.isEmpty()) {
            sb.append(" <properties>\n");
            for (Map.Entry<String, String> e : new TreeMap<>(properties).entrySet()) {
                sb.append("  <property name=\"").append(xml(e.getKey())).append("\" value=\"").append(xml(e.getValue()))
                        .append("\"/>\n");
            }
            sb.append(" </properties>\n");
        }
        sb.append(" <tileset firstgid=\"1\" name=\"placeholder\" tilewidth=\"16\" tileheight=\"16\" tilecount=\"")
                .append(PlaceholderTiles.TILE_COUNT).append("\" columns=\"").append(PlaceholderTiles.TILE_COUNT)
                .append("\">\n  <image source=\"../../tiles/placeholder.png\" width=\"").append(PlaceholderTiles.TILE_COUNT * T)
                .append("\" height=\"").append(T).append("\"/>\n </tileset>\n");
        for (int l = 0; l < LAYERS.length; l++) {
            sb.append(" <layer id=\"").append(l + 1).append("\" name=\"").append(LAYERS[l]).append("\" width=\"")
                    .append(w).append("\" height=\"").append(h).append("\">\n  <data encoding=\"csv\">\n");
            for (int ty = 0; ty < h; ty++) {
                for (int tx = 0; tx < w; tx++) {
                    sb.append(layers[l][ty * w + tx]);
                    if (tx < w - 1 || ty < h - 1) sb.append(',');
                }
                sb.append('\n');
            }
            sb.append("</data>\n </layer>\n");
        }
        sb.append(" <objectgroup id=\"").append(LAYERS.length + 1).append("\" name=\"entities\">\n");
        objects.forEach(sb::append);
        sb.append(" </objectgroup>\n</map>\n");
        return sb.toString();
    }

    /** A point object standing on the bottom-centre of tile (tx, ty), with a {@code type} and optional {@code drop}. */
    private static String pointObject(int id, String objectType, int tx, int ty, String kind, String drop) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <object id=\"").append(id).append("\" type=\"").append(objectType).append("\" x=\"")
                .append(tx * T + T / 2).append("\" y=\"").append((ty + 1) * T).append("\">\n   <properties>\n");
        if (drop != null) sb.append("    <property name=\"drop\" value=\"").append(drop).append("\"/>\n");
        sb.append("    <property name=\"type\" value=\"").append(kind).append("\"/>\n   </properties>\n   <point/>\n  </object>\n");
        return sb.toString();
    }

    private static String xml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static void main(String[] args) throws IOException {
        Path src = Path.of(args.length > 0 ? args[0] : "levels-src");
        Path res = Path.of(args.length > 1 ? args[1] : "resources");
        int count = 0;
        if (Files.isDirectory(src)) {
            List<Path> sources;
            try (Stream<Path> s = Files.walk(src)) {
                sources = s.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
            }
            for (Path p : sources) {
                String rel = src.relativize(p).toString().replace('\\', '/');
                Path out = res.resolve("levels").resolve(rel.substring(0, rel.length() - 4) + ".tmx");
                Files.createDirectories(out.getParent());
                Files.writeString(out, convert(Files.readString(p, UTF_8)), UTF_8);
                System.out.println("wrote " + out);
                count++;
            }
        }
        Path tiles = res.resolve(PlaceholderTiles.OUTPUT.substring("resources/".length()));
        Files.createDirectories(tiles.getParent());
        ImageIO.write(PlaceholderTiles.draw(), "png", tiles.toFile());
        System.out.println(count + " level(s) converted; tileset written to " + tiles);
    }
}
