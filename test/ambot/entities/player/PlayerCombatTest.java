package ambot.entities.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ambot.entities.Facing;
import ambot.gfx.SpriteSheet;
import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.TileMap;

class PlayerCombatTest {
    static final TileMap ROOM = PlayerTest.ROOM; // floor top at y=176, pit at x 160..224
    static final double STANDING_Y = PlayerTest.STANDING_Y;
    static SpriteSheet sheet;

    @BeforeAll
    static void loadSheet() {
        sheet = SpriteSheet.load(PlayerAnimations.SHEET_PATH, PlayerAnimations.CELL_W, PlayerAnimations.CELL_H);
    }

    static Controls held(Action... a) {
        return Controls.of(Set.of(a), Set.of(), Set.of());
    }

    static void step(Player p, Controls c, int n) {
        for (int i = 0; i < n; i++) p.update(c, ROOM);
    }

    static Player standing(double x) {
        Player p = new Player(x, STANDING_Y, sheet);
        step(p, Controls.NONE, 3);
        return p;
    }

    static double centre(Player p) {
        return p.x() + p.hitbox().w() / 2;
    }

    @Test
    void damageCostsOneHeartAndKnocksBackAway() {
        Player p = standing(100);
        assertEquals(3, p.health());
        assertTrue(p.damage(1, centre(p) + 10));
        assertEquals(2, p.health());
        assertEquals(PlayerState.HURT, p.state());
        assertEquals(-PlayerTuning.KNOCKBACK_X, p.vx());
        assertTrue(p.vy() < 0, "knocked upward, vy=" + p.vy());
    }

    @Test
    void hurtIgnoresInputUntilItEnds() {
        Player p = standing(100);
        p.damage(1, centre(p) + 10);
        double before = p.x();
        step(p, held(Action.RIGHT), PlayerTuning.HURT_TICKS - 1);
        assertEquals(PlayerState.HURT, p.state());
        assertEquals(Facing.RIGHT, p.facing());
        assertTrue(p.x() < before, "knocked left despite holding RIGHT");
        step(p, held(Action.RIGHT), 1);
        double afterHurt = p.x();
        step(p, held(Action.RIGHT), 10);
        assertTrue(p.x() > afterHurt, "RIGHT works again once the hurt ends");
    }

    @Test
    void invincibleIgnoresFurtherDamage() {
        Player p = standing(100);
        p.damage(1, centre(p) + 10);
        step(p, Controls.NONE, PlayerTuning.INVINCIBLE_TICKS - 1);
        assertTrue(p.isInvincible());
        assertFalse(p.damage(1, centre(p) + 10));
        assertEquals(2, p.health());
        step(p, Controls.NONE, 1);
        assertFalse(p.isInvincible());
        assertTrue(p.damage(1, centre(p) + 10));
        assertEquals(1, p.health());
    }

    @Test
    void blinksWhileInvincible() {
        Player p = standing(100);
        p.damage(1, centre(p) + 10);
        boolean sawHidden = false, sawShown = false;
        for (int i = 0; i < 8; i++) {
            step(p, Controls.NONE, 1);
            int opaque = opaquePixels(p);
            sawHidden |= opaque == 0;
            sawShown |= opaque > 0;
        }
        assertTrue(sawHidden && sawShown, "hidden " + sawHidden + ", shown " + sawShown);
    }

    private static int opaquePixels(Player p) {
        BufferedImage img = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        p.render(g);
        g.dispose();
        int n = 0;
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                if ((img.getRGB(x, y) >>> 24) != 0) n++;
        return n;
    }

    @Test
    void thirdHitKills() {
        Player p = standing(100);
        p.damage(1, centre(p) + 10);
        step(p, Controls.NONE, PlayerTuning.INVINCIBLE_TICKS);
        p.damage(1, centre(p) + 10);
        step(p, Controls.NONE, PlayerTuning.INVINCIBLE_TICKS);
        assertTrue(p.damage(1, centre(p) + 10));
        assertTrue(p.isDead());
        assertEquals(PlayerState.DEAD, p.state());
        assertFalse(p.deathFinished());
        double x = p.x();
        step(p, held(Action.RIGHT), PlayerTuning.DEATH_TICKS - 1);
        assertFalse(p.deathFinished());
        assertEquals(x, p.x(), "no movement while dead");
        assertEquals(PlayerState.DEAD, p.state());
        step(p, Controls.NONE, 1);
        assertTrue(p.deathFinished());
    }

    @Test
    void killIgnoresInvincibility() {
        Player p = standing(100);
        p.damage(1, centre(p) + 10);
        p.kill();
        assertEquals(0, p.health());
        assertEquals(PlayerState.DEAD, p.state());
    }

    @Test
    void respawnClearsHurtAndDeath() {
        Player p = standing(100);
        p.damage(1, centre(p) + 10);
        p.kill();
        p.respawn(32, STANDING_Y);
        p.restoreHealth();
        assertEquals(3, p.health());
        assertEquals(PlayerState.IDLE, p.state());
        assertFalse(p.isInvincible());
        assertFalse(p.isDead());
        assertFalse(p.deathFinished());
    }

    static Controls press(Action a) {
        return Controls.of(Set.of(a), Set.of(a), Set.of());
    }

    @Test
    void crouchShrinksHitboxKeepingFeet() {
        Player p = standing(32);
        double bottom = p.hitbox().bottom();
        step(p, held(Action.DOWN), 3);
        assertEquals(PlayerTuning.CROUCH_H, p.hitbox().h());
        assertEquals(bottom, p.hitbox().bottom());
        step(p, Controls.NONE, 1);
        assertEquals(PlayerTuning.HITBOX_H, p.hitbox().h());
        assertEquals(bottom, p.hitbox().bottom());
    }

    @Test
    void attackBoxOnlyInActiveFramesAndInFront() {
        Player p = standing(32);
        p.update(press(Action.ATTACK), ROOM);
        for (int n = 1; n <= 25; n++) {
            if (n > 1) p.update(Controls.NONE, ROOM);
            boolean active = n >= 10 && n <= 19;
            assertEquals(active, p.attackBox().isPresent(), "after update " + n);
            if (active) {
                assertEquals(p.hitbox().right(), p.attackBox().get().x());
                assertEquals(p.hitbox().y() + 4, p.attackBox().get().y());
            }
        }
        Player q = standing(100);
        step(q, held(Action.LEFT), 1);
        q.update(press(Action.ATTACK), ROOM);
        step(q, Controls.NONE, 9);
        assertEquals(q.hitbox().x(), q.attackBox().orElseThrow().right(), "facing left, the blade is on the left");
    }

    @Test
    void swingIdIncrementsPerAttack() {
        Player p = standing(32);
        int first = p.swingId();
        p.update(press(Action.ATTACK), ROOM);
        assertEquals(first + 1, p.swingId());
        step(p, Controls.NONE, 40);
        p.update(press(Action.ATTACK), ROOM);
        assertEquals(first + 2, p.swingId());
    }

    @Test
    void standingUpNeverEmbedsInCeiling() {
        TileMap corridor = TileMap.fromAscii(
                "##########",
                "..........",
                "..........",
                "##########"); // 32 px tall corridor, floor top at y=48
        Player p = new Player(40, 48 - PlayerTuning.HITBOX_H, sheet);
        for (int i = 0; i < 3; i++) p.update(Controls.NONE, corridor);
        for (int i = 0; i < 5; i++) p.update(held(Action.DOWN), corridor);
        p.update(Controls.NONE, corridor);
        assertFalse(corridor.overlaps(p.hitbox(), TileMap.SOLID), "hitbox " + p.hitbox());
        assertEquals(PlayerTuning.HITBOX_H, p.hitbox().h());
    }

    @Test
    void healCapsAtMax() {
        Player p = standing(100);
        p.damage(1, centre(p) + 10);
        p.heal(5);
        assertEquals(3, p.health());
    }
}
