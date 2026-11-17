package ambot.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import ambot.entities.player.PlayerTuning;
import ambot.level.Level;
import ambot.level.LevelCatalog;
import ambot.level.TmxLoader;
import ambot.level.LevelObject;
import ambot.level.LevelRenderer;
import ambot.level.Rect;
import ambot.level.Spot;

/** Renders whole levels to PNG for review (spec section 5.6). Not shipped. */
public final class LevelPreview {
    private static final Color SKY = new Color(0x29366F);
    private static final Color SPAWN = new Color(0x00E436);
    private static final Color THING = new Color(0xFF77A8);
    private static final Color ARENA = new Color(0xFF004D);

    private LevelPreview() {
    }

    /** Writes {@code <name>.png} for every catalog level into the folder {@code args[0]}. */
    public static void main(String[] args) throws IOException {
        File out = new File(args.length > 0 ? args[0] : "build/preview");
        out.mkdirs();
        for (String path : LevelCatalog.PLACEHOLDER_PATHS) {
            Level level = TmxLoader.load(path);
            File png = new File(out, level.name() + ".png");
            ImageIO.write(render(level), "png", png);
            System.out.println("wrote " + png + " (" + level.widthTiles() + "x" + level.heightTiles() + " tiles)");
        }
    }

    /** The whole level at 1x: tiles, markers, and outlines for the spawn, enemies, pickups and boss arena. */
    public static BufferedImage render(Level level) {
        BufferedImage img = new BufferedImage(level.widthPx(), level.heightPx(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(SKY);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        Rect all = new Rect(0, 0, img.getWidth(), img.getHeight());
        LevelRenderer r = new LevelRenderer(level);
        r.drawBehind(g, all);
        r.drawMarkers(g, null);
        r.drawFront(g, all);

        for (LevelObject o : level.objects(LevelObject.PLAYER_SPAWN)) {
            Rect b = o.bounds();
            Spot feet = new Spot(b.x() + b.w() / 2, b.bottom());
            g.setColor(SPAWN);
            g.drawRect((int) (feet.x() - PlayerTuning.HITBOX_W / 2.0), (int) feet.y() - PlayerTuning.HITBOX_H,
                    PlayerTuning.HITBOX_W - 1, PlayerTuning.HITBOX_H - 1);
        }
        g.setColor(THING);
        for (LevelObject o : level.objects()) {
            if (o.type().equals(LevelObject.ENEMY) || o.type().equals(LevelObject.PICKUP)) {
                Rect b = o.bounds();
                g.fillRect((int) (b.x() + b.w() / 2) - 3, (int) (b.y() + b.h() / 2) - 3, 6, 6);
            }
        }
        g.setColor(ARENA);
        for (LevelObject o : level.objects(LevelObject.BOSS_ARENA)) {
            Rect b = o.bounds();
            g.drawRect((int) b.x(), (int) b.y(), (int) b.w() - 1, (int) b.h() - 1);
        }
        g.dispose();
        return img;
    }
}
