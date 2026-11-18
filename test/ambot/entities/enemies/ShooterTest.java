package ambot.entities.enemies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ambot.entities.Facing;
import ambot.entities.Projectile;
import ambot.entities.player.PlayerTuning;
import ambot.level.Level;
import ambot.level.LevelObject;
import ambot.level.Rect;
import ambot.level.TestLevels;

class ShooterTest {
    /** 40 tiles wide, floor top at y=48, shooter standing at tile (4,2). */
    static final Level RANGE = TestLevels.ascii(
            ".".repeat(40),
            ".".repeat(40),
            "....s" + ".".repeat(35),
            "#".repeat(40));
    static final double FLOOR = 48;

    private final List<Projectile> shots = new ArrayList<>();

    private Shooter shooter() {
        return (Shooter) EnemyFactory.create(RANGE.objects(LevelObject.ENEMY).get(0));
    }

    /** A standing 14x28 player whose centre is {@code dx} px from the shooter's centre. */
    private EnemyWorld world(Shooter s, double dx) {
        double cx = s.x() + s.hitbox().w() / 2 + dx;
        Rect player = new Rect(cx - 7, FLOOR - PlayerTuning.HITBOX_H, 14, PlayerTuning.HITBOX_H);
        return new EnemyWorld(player, RANGE.collision(), shots::add);
    }

    private static void step(Shooter s, EnemyWorld w, int n) {
        for (int i = 0; i < n; i++) s.update(w);
    }

    @Test
    void firesAfterDelayWhileInRange() {
        Shooter s = shooter();
        EnemyWorld w = world(s, 100);
        step(s, w, Shooter.FIRST_SHOT_DELAY - 1);
        assertEquals(0, shots.size());
        step(s, w, 1);
        assertEquals(1, shots.size());
        step(s, w, Shooter.FIRE_INTERVAL - 1);
        assertEquals(1, shots.size());
        step(s, w, 1);
        assertEquals(2, shots.size());
    }

    @Test
    void doesNotFireOutOfRange() {
        Shooter s = shooter();
        step(s, world(s, 200), 300);
        assertEquals(0, shots.size());
    }

    @Test
    void facesPlayer() {
        Shooter s = shooter();
        step(s, world(s, -100), 1);
        assertEquals(Facing.LEFT, s.facing());
        step(s, world(s, 100), 1);
        assertEquals(Facing.RIGHT, s.facing());
    }

    @Test
    void boltFliesAtStandingHeight() {
        Shooter s = shooter();
        step(s, world(s, 100), Shooter.FIRST_SHOT_DELAY);
        Projectile bolt = shots.get(0);
        Rect standing = new Rect(0, FLOOR - PlayerTuning.HITBOX_H, 14, PlayerTuning.HITBOX_H);
        Rect crouched = new Rect(0, FLOOR - PlayerTuning.CROUCH_H, 14, PlayerTuning.CROUCH_H);
        Rect boltAtX0 = bolt.hitbox().withX(0);
        assertTrue(boltAtX0.intersects(standing), "a standing player is hit: bolt " + bolt.hitbox());
        assertTrue(!boltAtX0.intersects(crouched), "a crouching player is missed: bolt " + bolt.hitbox());
        assertTrue(bolt.vx() > 0, "fired toward the player");
        assertEquals(s.hitbox().right(), bolt.x(), "starts at the front edge");
    }

    @Test
    void factoryCreatesShooter() {
        Enemy e = EnemyFactory.create(RANGE.objects(LevelObject.ENEMY).get(0));
        assertInstanceOf(Shooter.class, e);
        assertEquals(FLOOR, e.hitbox().bottom());
        assertEquals(Shooter.W, e.hitbox().w());
    }
}
