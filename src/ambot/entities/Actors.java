package ambot.entities;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ambot.entities.enemies.Enemy;
import ambot.entities.enemies.EnemyFactory;
import ambot.entities.enemies.EnemyWorld;
import ambot.entities.player.Player;
import ambot.level.Level;
import ambot.level.LevelObject;
import ambot.level.Rect;
import ambot.level.Spot;
import ambot.level.TileMap;

/**
 * Everything in a level besides the player: enemies, projectiles and pickups. Each tick it
 * moves them and settles every hit between them and the player, in a fixed order.
 */
public final class Actors {
    private static final int VIEW_MARGIN = 32;

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<HeartPickup> pickups = new ArrayList<>();

    /** Spawns every enemy and heart pickup the level places. */
    public Actors(Level level) {
        for (LevelObject o : level.objects(LevelObject.ENEMY)) enemies.add(EnemyFactory.create(o));
        for (LevelObject o : level.objects(LevelObject.PICKUP)) {
            if ("heart".equals(o.property("type"))) pickups.add(new HeartPickup(feet(o.bounds())));
        }
    }

    public void update(Player player, TileMap map) {
        Rect body = player.hitbox();
        double playerX = body.x() + body.w() / 2;
        EnemyWorld world = new EnemyWorld(body, map, projectiles::add);

        // 1-3. move everything
        for (Enemy e : enemies) e.update(world);
        for (Projectile p : projectiles) p.update(map);
        for (HeartPickup h : pickups) h.update();

        // 4. the sword: each swing hits each enemy at most once
        player.attackBox().ifPresent(blade -> {
            for (Enemy e : enemies) {
                if (!e.isDead() && e.hitbox().intersects(blade) && e.hitBySwing(player.swingId())) {
                    e.damage(1, playerX);
                }
            }
        });

        // 5. touching an enemy
        for (Enemy e : enemies) {
            if (!e.isDead() && e.hitbox().intersects(body)) {
                player.damage(e.contactDamage(), centreX(e.hitbox()));
            }
        }

        // 6. bolts: used up on contact, even when the player is invincible
        for (Projectile p : projectiles) {
            if (p.alive() && p.hitbox().intersects(body)) {
                player.damage(p.damage(), centreX(p.hitbox()));
                p.kill();
            }
        }

        // 7. pickups
        for (HeartPickup h : pickups) {
            if (h.hitbox().intersects(body)) h.tryCollect(player);
        }

        // 8. clean up; defeated enemies may leave a heart where they stood
        projectiles.removeIf(p -> !p.alive());
        pickups.removeIf(HeartPickup::collected);
        for (Iterator<Enemy> it = enemies.iterator(); it.hasNext();) {
            Enemy e = it.next();
            if (e.isRemovable()) {
                if ("heart".equals(e.drop())) pickups.add(new HeartPickup(feet(e.hitbox())));
                it.remove();
            }
        }
    }

    /** Adds a projectile, e.g. one fired by something outside this list. */
    public void spawn(Projectile p) {
        projectiles.add(p);
    }

    public void render(Graphics2D g, Rect view) {
        Rect area = new Rect(view.x() - VIEW_MARGIN, view.y() - VIEW_MARGIN, view.w() + 2 * VIEW_MARGIN,
                view.h() + 2 * VIEW_MARGIN);
        for (HeartPickup h : pickups) if (h.hitbox().intersects(area)) h.render(g);
        for (Enemy e : enemies) if (e.hitbox().intersects(area)) e.render(g);
        for (Projectile p : projectiles) if (p.hitbox().intersects(area)) p.render(g);
    }

    public List<Enemy> enemies() {
        return Collections.unmodifiableList(enemies);
    }

    public List<Projectile> projectiles() {
        return Collections.unmodifiableList(projectiles);
    }

    public List<HeartPickup> pickups() {
        return Collections.unmodifiableList(pickups);
    }

    private static double centreX(Rect r) {
        return r.x() + r.w() / 2;
    }

    private static Spot feet(Rect b) {
        return new Spot(b.x() + b.w() / 2, b.bottom());
    }
}
