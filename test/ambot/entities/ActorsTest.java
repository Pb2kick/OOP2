package ambot.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ambot.entities.enemies.Enemy;
import ambot.entities.player.Player;
import ambot.entities.player.PlayerAnimations;
import ambot.entities.player.PlayerTuning;
import ambot.gfx.SpriteSheet;
import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.Level;
import ambot.level.Rect;
import ambot.level.TestLevels;
import ambot.level.TileMap;

class ActorsTest {
    static SpriteSheet sheet;
    /** Every test level has its floor top at y=32. */
    static final double FLOOR = 32;

    @BeforeAll
    static void loadSheet() {
        sheet = SpriteSheet.load(PlayerAnimations.SHEET_PATH, PlayerAnimations.CELL_W, PlayerAnimations.CELL_H);
    }

    static Player playerAt(double x) {
        return new Player(x, FLOOR - PlayerTuning.HITBOX_H, sheet);
    }

    static Controls press(Action a) {
        return Controls.of(Set.of(a), Set.of(a), Set.of());
    }

    static final Controls DOWN = Controls.of(Set.of(Action.DOWN), Set.of(), Set.of());

    /** One tick of play: the player moves, then the actors resolve. */
    static void tick(Player p, Actors a, TileMap map, Controls c) {
        p.update(c, map);
        a.update(p, map);
    }

    /** A walker boxed into a one-tile pocket (x 96..112) so it can't wander. */
    static final Level POCKET = TestLevels.ascii(
            "..............................",
            ".....#w#......................",
            "##############################");

    @Test
    void swingHitsEachEnemyOnce() {
        Actors a = new Actors(POCKET);
        TileMap map = POCKET.collision();
        Player p = playerAt(66); // right edge against the pocket wall at x=80; the blade reaches x=100
        Enemy walker = a.enemies().get(0);
        tick(p, a, map, press(Action.ATTACK));
        for (int i = 0; i < 40; i++) tick(p, a, map, Controls.NONE);
        assertEquals(1, walker.health(), "one swing, one hit");
        tick(p, a, map, press(Action.ATTACK));
        for (int i = 0; i < 40; i++) tick(p, a, map, Controls.NONE);
        assertTrue(walker.isDead(), "a second swing finishes it");
    }

    @Test
    void swingMissesEnemyBehind() {
        Actors a = new Actors(POCKET);
        TileMap map = POCKET.collision();
        Player p = playerAt(128); // right of the pocket, facing right: the walker is behind
        tick(p, a, map, press(Action.ATTACK));
        for (int i = 0; i < 40; i++) tick(p, a, map, Controls.NONE);
        assertEquals(2, a.enemies().get(0).health());
    }

    @Test
    void contactDamagesPlayer() {
        Level open = TestLevels.ascii(
                "..............................",
                "........w.....................",
                "##############################");
        Actors a = new Actors(open);
        Player p = playerAt(40);
        for (int i = 0; i < 200 && p.health() == 3; i++) tick(p, a, open.collision(), Controls.NONE);
        assertEquals(2, p.health());
    }

    @Test
    void twoSourcesSameTickCostOneHeart() {
        Level open = TestLevels.ascii(
                "..............................",
                "...w..........................",
                "##############################");
        Actors a = new Actors(open);
        Player p = playerAt(50); // body 50..64 overlaps the walker at 48..64
        Projectile bolt = new Projectile(55, FLOOR - 20, -2.5);
        a.spawn(bolt);
        a.update(p, open.collision());
        assertEquals(2, p.health());
        assertTrue(a.projectiles().isEmpty(), "the bolt is used up");
    }

    static final Level RANGE = TestLevels.ascii(
            "..............................",
            "..s...........................",
            "##############################");

    @Test
    void boltHitsStandingPlayerAndIsRemoved() {
        Actors a = new Actors(RANGE);
        Player p = playerAt(140);
        int ticks = 0;
        while (p.health() == 3 && ticks++ < 150) tick(p, a, RANGE.collision(), Controls.NONE);
        assertEquals(2, p.health(), "hit by a bolt");
        assertTrue(a.projectiles().isEmpty());
    }

    @Test
    void crouchingPlayerDodgesBolt() {
        Actors a = new Actors(RANGE);
        Player p = playerAt(140);
        double furthest = 0;
        for (int i = 0; i < 200; i++) {
            tick(p, a, RANGE.collision(), DOWN);
            for (Projectile b : a.projectiles()) furthest = Math.max(furthest, b.x());
        }
        assertEquals(3, p.health());
        assertTrue(furthest > p.hitbox().right(), "a bolt flew past, furthest x=" + furthest);
    }

    @Test
    void pickupHealsHurtPlayerOnly() {
        Level hearts = TestLevels.ascii(
                "..............................",
                "...h..........................",
                "##############################");
        Actors a = new Actors(hearts);
        Player p = playerAt(50);
        a.update(p, hearts.collision());
        assertEquals(1, a.pickups().size(), "left alone at full health");
        p.damage(1, 0);
        a.update(p, hearts.collision());
        assertEquals(3, p.health());
        assertTrue(a.pickups().isEmpty());
    }

    @Test
    void deadEnemyWithHeartDropLeavesPickup() {
        Level dropper = TestLevels.ascii(
                "..............................",
                ".....#W#......................",
                "##############################");
        Actors a = new Actors(dropper);
        Player p = playerAt(400);
        Enemy walker = a.enemies().get(0);
        Rect body = walker.hitbox();
        walker.damage(1, 0);
        for (int i = 0; i < Enemy.STUN_TICKS + 1; i++) a.update(p, dropper.collision());
        walker.damage(1, 0);
        for (int i = 0; i < Enemy.REMOVE_TICKS; i++) a.update(p, dropper.collision());
        assertTrue(a.enemies().isEmpty());
        assertEquals(1, a.pickups().size());
        Rect heart = a.pickups().get(0).hitbox();
        assertEquals(body.bottom(), heart.bottom());
        assertEquals(body.x() + body.w() / 2, heart.x() + heart.w() / 2);
    }
}
