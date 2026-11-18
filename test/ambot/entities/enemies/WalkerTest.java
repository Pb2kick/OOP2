package ambot.entities.enemies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ambot.level.Level;
import ambot.level.LevelObject;
import ambot.level.Rect;
import ambot.level.TestLevels;

class WalkerTest {
    static final Rect FAR_AWAY = new Rect(5000, 5000, 1, 1);

    static EnemyWorld world(Level level) {
        return new EnemyWorld(FAR_AWAY, level.collision(), p -> { });
    }

    static Enemy first(Level level) {
        return EnemyFactory.create(level.objects(LevelObject.ENEMY).get(0));
    }

    static void step(Enemy e, EnemyWorld w, int n) {
        for (int i = 0; i < n; i++) e.update(w);
    }

    static final Level FLOOR = TestLevels.ascii(
            "....................",
            "..w.................",
            "####################");

    @Test
    void patrolsAndTurnsAtWall() {
        Enemy e = first(FLOOR);
        EnemyWorld w = world(FLOOR);
        double start = e.x(), min = start;
        for (int i = 0; i < 200; i++) {
            e.update(w);
            min = Math.min(min, e.x());
        }
        assertTrue(min < start, "walked left first");
        assertEquals(0, min, 0.001, "reached the left wall");
        assertTrue(e.x() > min, "turned around at the wall, x=" + e.x());
    }

    @Test
    void turnsAtLedgeInsteadOfFalling() {
        Level ledge = TestLevels.ascii(
                "............",
                "............",
                "....w.......",
                "..######....",
                "............",
                "............");
        Enemy e = first(ledge);
        EnemyWorld w = world(ledge);
        double y = e.y();
        for (int i = 0; i < 600; i++) {
            e.update(w);
            assertEquals(y, e.y(), "fell off at tick " + i + ", x=" + e.x());
        }
    }

    @Test
    void twoSwingsKill() {
        Enemy e = first(FLOOR);
        EnemyWorld w = world(FLOOR);
        assertTrue(e.damage(1, e.x() - 10));
        assertEquals(1, e.health());
        step(e, w, Enemy.STUN_TICKS + 1);
        assertTrue(e.damage(1, e.x() - 10));
        assertTrue(e.isDead());
        step(e, w, Enemy.REMOVE_TICKS - 1);
        assertFalse(e.isRemovable());
        step(e, w, 1);
        assertTrue(e.isRemovable());
    }

    @Test
    void stunnedWalkerIsPushedAway() {
        Level wide = TestLevels.ascii(
                "....................",
                ".........w..........",
                "####################");
        Enemy e = first(wide);
        EnemyWorld w = world(wide);
        e.damage(1, e.x() - 10); // hit from its left
        double last = e.x();
        for (int i = 0; i < Enemy.STUN_TICKS; i++) {
            e.update(w);
            assertTrue(e.x() > last, "pushed right at tick " + i);
            last = e.x();
        }
    }

    @Test
    void hitBySwingOncePerSwing() {
        Enemy e = first(FLOOR);
        assertTrue(e.hitBySwing(1));
        assertFalse(e.hitBySwing(1));
        assertTrue(e.hitBySwing(2));
    }

    @Test
    void factoryPlacesFeetOnSpot() {
        Enemy plain = EnemyFactory.create(
                new LevelObject(LevelObject.ENEMY, new Rect(40, 64, 0, 0), Map.of("type", "walker")));
        assertEquals(64, plain.hitbox().bottom());
        assertEquals(40, plain.hitbox().x() + plain.hitbox().w() / 2);
        assertNull(plain.drop());
        Enemy dropper = EnemyFactory.create(
                new LevelObject(LevelObject.ENEMY, new Rect(40, 64, 0, 0), Map.of("type", "walker", "drop", "heart")));
        assertEquals("heart", dropper.drop());
        assertEquals(1, plain.contactDamage());
    }
}
