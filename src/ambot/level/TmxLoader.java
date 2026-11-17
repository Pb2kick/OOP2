package ambot.level;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import ambot.core.GameConfig;

/**
 * Reads Tiled {@code .tmx} maps (spec section 5.6) with the JDK's XML parser: orthogonal,
 * finite, 16x16 tiles, CSV layers and one embedded tileset. Every failure is a
 * {@link LevelFormatException} whose message starts with the file's resource path.
 */
public final class TmxLoader {
    private static final long GID_MASK = 0x0FFFFFFFL; // Tiled keeps flip flags in the top 4 bits

    private TmxLoader() {
    }

    /** Loads a map from the classpath, e.g. {@code "/levels/placeholder/p1.tmx"}. */
    public static Level load(String resourcePath) {
        try (InputStream in = TmxLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new LevelFormatException(resourcePath + ": level file not found");
            return parse(in, resourcePath);
        } catch (IOException e) {
            throw new LevelFormatException(resourcePath + ": " + e.getMessage(), e);
        }
    }

    /** Parses a map; {@code resourcePath} names it in errors and anchors the tileset image path. */
    public static Level parse(InputStream in, String resourcePath) {
        return new Reader(resourcePath).read(in);
    }

    private static final class Reader {
        private final String path;

        Reader(String path) {
            this.path = path;
        }

        private LevelFormatException fail(String message) {
            return new LevelFormatException(path + ": " + message);
        }

        Level read(InputStream in) {
            Element map = parseXml(in).getDocumentElement();
            if (!map.getTagName().equals("map")) throw fail("root element is <" + map.getTagName() + ">, expected <map>");
            String orientation = map.getAttribute("orientation");
            if (!orientation.isEmpty() && !orientation.equals("orthogonal")) {
                throw fail("only orthogonal maps are supported, got '" + orientation + "'");
            }
            if (map.getAttribute("infinite").equals("1")) throw fail("infinite maps are not supported; untick Infinite in Tiled");
            requireTileSize(map, "map");
            int w = intAttr(map, "width"), h = intAttr(map, "height");

            Tileset tileset = null;
            List<TileLayer> layers = new ArrayList<>();
            List<LevelObject> objects = new ArrayList<>();
            for (Element child : children(map)) {
                switch (child.getTagName()) {
                    case "tileset" -> {
                        if (tileset != null) throw fail("expected exactly one tileset, found more");
                        tileset = readTileset(child);
                    }
                    case "layer" -> layers.add(readLayer(child, w, h));
                    case "objectgroup" -> objects.addAll(readObjects(child));
                    default -> {
                        // properties, editorsettings, ... are read elsewhere or ignored
                    }
                }
            }
            if (tileset == null) throw fail("no tileset; embed one tileset in the map");
            if (layers.stream().noneMatch(l -> l.name().equals(Level.SOLID))) {
                throw fail("missing required layer '" + Level.SOLID + "'");
            }
            try {
                return new Level(baseName(), readProperties(map), tileset, w, h, layers, objects);
            } catch (IllegalArgumentException e) {
                throw fail(e.getMessage());
            }
        }

        private Document parseXml(InputStream in) {
            try {
                DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
                f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                f.setExpandEntityReferences(false);
                DocumentBuilder b = f.newDocumentBuilder();
                b.setErrorHandler(null);
                return b.parse(in);
            } catch (SAXException | IOException e) {
                throw new LevelFormatException(path + ": not valid XML: " + e.getMessage(), e);
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException(e);
            }
        }

        private Tileset readTileset(Element ts) {
            if (!ts.getAttribute("source").isEmpty()) {
                throw fail("external tilesets (.tsx) are not supported; embed the tileset in the map");
            }
            requireTileSize(ts, "tileset");
            Element image = children(ts).stream().filter(e -> e.getTagName().equals("image")).findFirst()
                    .orElseThrow(() -> fail("tileset has no <image>"));
            String source = image.getAttribute("source");
            String imagePath;
            try {
                imagePath = URI.create(path).resolve(source).getPath();
            } catch (IllegalArgumentException e) {
                throw fail("bad tileset image path '" + source + "'");
            }
            BufferedImage img;
            try (InputStream in = TmxLoader.class.getResourceAsStream(imagePath)) {
                if (in == null) throw fail("tileset image not found: " + imagePath);
                img = ImageIO.read(in);
            } catch (IOException e) {
                throw fail("can't read tileset image " + imagePath + ": " + e.getMessage());
            }
            if (img == null) throw fail("tileset image is not a readable image: " + imagePath);
            try {
                return new Tileset(intAttr(ts, "firstgid"), intAttr(ts, "tilecount"), intAttr(ts, "columns"), img);
            } catch (IllegalArgumentException e) {
                throw fail(e.getMessage());
            }
        }

        private TileLayer readLayer(Element layer, int mapW, int mapH) {
            String name = layer.getAttribute("name");
            if (!Level.TILE_LAYERS.contains(name)) {
                throw fail("unknown layer '" + name + "'; allowed tile layers are " + Level.TILE_LAYERS);
            }
            Element data = children(layer).stream().filter(e -> e.getTagName().equals("data")).findFirst()
                    .orElseThrow(() -> fail("layer '" + name + "' has no <data>"));
            if (!data.getAttribute("encoding").equals("csv") || !data.getAttribute("compression").isEmpty()) {
                throw fail("layer '" + name + "' is not CSV; in Tiled set Map Properties > Tile Layer Format to CSV");
            }
            String text = data.getTextContent().trim();
            String[] cells = text.isEmpty() ? new String[0] : text.split("[,\\s]+");
            if (cells.length != mapW * mapH) {
                throw fail("layer '" + name + "' has " + cells.length + " tiles, expected " + mapW + "x" + mapH);
            }
            int[] gids = new int[cells.length];
            for (int i = 0; i < cells.length; i++) {
                try {
                    gids[i] = (int) (Long.parseLong(cells[i]) & GID_MASK);
                } catch (NumberFormatException e) {
                    throw fail("layer '" + name + "' has a non-number tile '" + cells[i] + "'");
                }
            }
            return new TileLayer(name, mapW, mapH, gids);
        }

        private List<LevelObject> readObjects(Element group) {
            String name = group.getAttribute("name");
            if (!name.equals(Level.ENTITIES)) {
                throw fail("unknown object layer '" + name + "'; the only object layer is '" + Level.ENTITIES + "'");
            }
            List<LevelObject> out = new ArrayList<>();
            for (Element o : children(group)) {
                if (!o.getTagName().equals("object")) continue;
                String type = o.getAttribute("type");
                if (type.isEmpty()) type = o.getAttribute("class");
                boolean point = children(o).stream().anyMatch(e -> e.getTagName().equals("point"));
                double w = point ? 0 : doubleAttr(o, "width", 0), h = point ? 0 : doubleAttr(o, "height", 0);
                Rect r = new Rect(doubleAttr(o, "x", 0), doubleAttr(o, "y", 0), w, h);
                out.add(new LevelObject(type, r, readProperties(o)));
            }
            return out;
        }

        private Map<String, String> readProperties(Element owner) {
            Map<String, String> props = new HashMap<>();
            for (Element ps : children(owner)) {
                if (!ps.getTagName().equals("properties")) continue;
                for (Element p : children(ps)) {
                    if (!p.getTagName().equals("property")) continue;
                    String value = p.hasAttribute("value") ? p.getAttribute("value") : p.getTextContent();
                    props.put(p.getAttribute("name"), value);
                }
            }
            return props;
        }

        private void requireTileSize(Element e, String what) {
            int tw = intAttr(e, "tilewidth"), th = intAttr(e, "tileheight");
            if (tw != GameConfig.TILE || th != GameConfig.TILE) {
                throw fail(what + " tiles are " + tw + "x" + th + ", must be " + GameConfig.TILE + "x" + GameConfig.TILE);
            }
        }

        private int intAttr(Element e, String name) {
            String v = e.getAttribute(name);
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException ex) {
                throw fail("<" + e.getTagName() + "> attribute '" + name + "' should be a whole number, got '" + v + "'");
            }
        }

        private double doubleAttr(Element e, String name, double fallback) {
            String v = e.getAttribute(name);
            if (v.isEmpty()) return fallback;
            try {
                return Double.parseDouble(v.trim());
            } catch (NumberFormatException ex) {
                throw fail("<" + e.getTagName() + "> attribute '" + name + "' should be a number, got '" + v + "'");
            }
        }

        private String baseName() {
            String file = path.substring(path.lastIndexOf('/') + 1);
            int dot = file.lastIndexOf('.');
            return dot > 0 ? file.substring(0, dot) : file;
        }
    }

    private static List<Element> children(Element parent) {
        List<Element> out = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) out.add(e);
        }
        return out;
    }
}
