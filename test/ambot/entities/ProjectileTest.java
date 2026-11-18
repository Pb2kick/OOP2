package ambot.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ambot.level.TileMap;

class ProjectileTest {
    static final String OPEN = ".".repeat(40);
    static final TileMap WIDE = TileMap.fromAscii(OPEN, OPEN, OPEN, "#".repeat(40));

    @Test
    void fliesStraight() {
        Projectile p = new Projectile(32, 20, 2.5);
        for (int i = 0; i < 10; i++) p.update(WIDE);
        assertEquals(57, p.x());
        assertEquals(20, p.y());
        assertTrue(p.alive());
        assertEquals(1, p.damage());
    }

    @Test
    void stopsAtWall() {
        TileMap walled = TileMap.fromAscii(
                "......#...",
                "......#...",
                "##########"); // wall face at x=96
        Projectile p = new Projectile(96 - 48 - 6, 20, 2.5); // front 3 tiles from the wall
        int ticks = 0;
        while (p.alive() && ticks < 25) {
            p.update(walled);
            ticks++;
        }
        assertFalse(p.alive(), "still alive after " + ticks + " ticks at x=" + p.x());
    }

    @Test
    void expiresAfterMaxDistance() {
        Projectile p = new Projectile(10, 20, 2.5);
        int ticks = (int) (Projectile.MAX_DISTANCE / 2.5);
        for (int i = 0; i < ticks - 1; i++) p.update(WIDE);
        assertTrue(p.alive());
        p.update(WIDE);
        assertFalse(p.alive());
    }

    @Test
    void killStopsIt() {
        Projectile p = new Projectile(10, 20, -2.5);
        p.kill();
        assertFalse(p.alive());
        p.update(WIDE);
        assertEquals(10, p.x());
    }
}
