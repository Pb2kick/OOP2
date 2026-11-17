package ambot.entities.player;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ambot.entities.Facing;
import ambot.gfx.SpriteSheet;
import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.TileMap;

class PlayerTest {
    // 20x12 tiles; floor top at y=176; pit at columns 10-13 (x 160..224)
    static final TileMap ROOM = TileMap.fromAscii(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "##########....######");
    static final double STANDING_Y = 176 - 28;
    static SpriteSheet sheet;

    @BeforeAll
    static void loadSheet() {
        sheet = SpriteSheet.load(PlayerAnimations.SHEET_PATH, PlayerAnimations.CELL_W, PlayerAnimations.CELL_H);
    }

    static Controls held(Action... a) {
        return Controls.of(Set.of(a), Set.of(), Set.of());
    }

    /** Pressed this tick and still held. */
    static Controls press(Action... a) {
        return Controls.of(Set.of(a), Set.of(a), Set.of());
    }

    /** Pressed and released within one tick (a quick tap). */
    static Controls tap(Action a) {
        return Controls.of(Set.of(), Set.of(a), Set.of(a));
    }

    static void step(Player p, Controls c, int n) {
        for (int i = 0; i < n; i++) p.update(c, ROOM);
    }

    static Player standing(double x) {
        Player p = new Player(x, STANDING_Y, sheet);
        step(p, Controls.NONE, 3);
        return p;
    }

    @Test
    void settlesIdleOnFloor() {
        Player p = standing(32);
        assertTrue(p.onGround());
        assertEquals(PlayerState.IDLE, p.state());
        assertEquals(STANDING_Y, p.y());
    }

    @Test
    void runsRightAndFacesRight() {
        Player p = new Player(32, STANDING_Y, sheet);
        step(p, held(Action.RIGHT), 10);
        assertEquals(52, p.x());
        assertEquals(PlayerState.RUN, p.state());
        assertEquals(Facing.RIGHT, p.facing());
    }

    @Test
    void facesLeftWhenMovingLeft() {
        Player p = standing(100);
        step(p, held(Action.LEFT), 3);
        assertEquals(Facing.LEFT, p.facing());
        assertEquals(94, p.x());
    }

    @Test
    void heldJumpRisesAbout48px() {
        Player p = standing(32);
        p.update(press(Action.JUMP), ROOM);
        double minY = p.y();
        boolean sawFall = false;
        for (int i = 0; i < 120 && !(p.onGround() && i > 2); i++) {
            assertEquals(p.vy() < 0 ? PlayerState.JUMP : p.onGround() ? p.state() : PlayerState.FALL, p.state());
            sawFall |= p.state() == PlayerState.FALL;
            p.update(held(Action.JUMP), ROOM);
            minY = Math.min(minY, p.y());
        }
        double rise = STANDING_Y - minY;
        assertTrue(rise >= 45 && rise <= 55, "rise was " + rise);
        assertTrue(sawFall, "should pass through FALL");
        p.update(Controls.NONE, ROOM);
        assertEquals(PlayerState.IDLE, p.state());
        assertEquals(STANDING_Y, p.y());
    }

    @Test
    void tapJumpGivesShortButVisibleHop() {
        Player p = standing(32);
        p.update(tap(Action.JUMP), ROOM);
        double minY = p.y();
        for (int i = 0; i < 30; i++) {
            p.update(Controls.NONE, ROOM);
            minY = Math.min(minY, p.y());
        }
        double rise = STANDING_Y - minY;
        // a quick tap must clearly leave the ground (about a tile and a half), but stay well below a full jump
        assertTrue(rise >= 15 && rise <= 30, "tap rise was " + rise);
    }

    @Test
    void crouchStopsHorizontalMovement() {
        Player p = standing(32);
        step(p, held(Action.DOWN, Action.RIGHT), 5);
        assertEquals(PlayerState.CROUCH, p.state());
        assertEquals(32, p.x());
    }

    private Player runOffLedge() {
        Player p = standing(146);
        for (int i = 0; i < 20 && p.onGround(); i++) p.update(held(Action.RIGHT), ROOM);
        assertFalse(p.onGround(), "should have run off the ledge");
        return p;
    }

    @Test
    void coyoteJumpWithinGraceWindow() {
        Player p = runOffLedge();
        step(p, Controls.NONE, 3);
        p.update(press(Action.JUMP), ROOM);
        assertTrue(p.vy() < 0, "late jump inside the grace window should fire, vy=" + p.vy());

        Player q = runOffLedge();
        step(q, Controls.NONE, 6);
        q.update(press(Action.JUMP), ROOM);
        assertTrue(q.vy() > 0, "jump after the grace window must not fire, vy=" + q.vy());
    }

    @Test
    void jumpBufferedBeforeLanding() {
        Player p = new Player(32, 100, sheet);
        while (p.y() + 28 < 176 - 12) p.update(Controls.NONE, ROOM);
        p.update(press(Action.JUMP), ROOM);
        boolean jumped = false;
        for (int i = 0; i < 6 && !jumped; i++) {
            p.update(held(Action.JUMP), ROOM);
            jumped = p.vy() < -4;
        }
        assertTrue(jumped, "a jump pressed just before landing should fire on landing");
    }

    @Test
    void attackLasts35Ticks() {
        Player p = standing(32);
        p.update(press(Action.ATTACK), ROOM);
        step(p, Controls.NONE, 34);
        assertEquals(PlayerState.ATTACK, p.state());
        p.update(Controls.NONE, ROOM);
        assertEquals(PlayerState.IDLE, p.state());
    }

    @Test
    void secondPressDoesNotExtendAttack() {
        Player p = standing(32);
        p.update(press(Action.ATTACK), ROOM);
        step(p, Controls.NONE, 8);
        p.update(press(Action.ATTACK), ROOM);
        step(p, Controls.NONE, 26);
        assertEquals(PlayerState.IDLE, p.state());
    }

    @Test
    void groundAttackStopsRunning() {
        Player p = standing(32);
        p.update(press(Action.ATTACK, Action.RIGHT), ROOM);
        step(p, held(Action.RIGHT), 34);
        assertEquals(32, p.x());
    }

    // one-way ledge on row 8 (top y=128) at columns 3..9; tile (2,8) is solid; floor top at y=176
    static final TileMap LEDGE_ROOM = TileMap.fromAscii(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "..#-------..........",
            "....................",
            "....................",
            "####################");
    static final double ON_LEDGE_Y = 128 - 28;

    static void stepOn(TileMap map, Player p, Controls c, int n) {
        for (int i = 0; i < n; i++) p.update(c, map);
    }

    static Player standingOnLedge(double x) {
        Player p = new Player(x, ON_LEDGE_Y, sheet);
        stepOn(LEDGE_ROOM, p, Controls.NONE, 3);
        assertTrue(p.onGround(), "should start standing on the ledge");
        return p;
    }

    @Test
    void downOnOneWayDropsThrough() {
        Player p = standingOnLedge(80);
        p.update(press(Action.DOWN), LEDGE_ROOM);
        stepOn(LEDGE_ROOM, p, held(Action.DOWN), 10);
        assertTrue(p.y() > ON_LEDGE_Y, "y was " + p.y());
        assertEquals(PlayerState.FALL, p.state());
    }

    @Test
    void noDropWhenPartlyOnSolid() {
        Player p = standingOnLedge(40); // over the solid tile (2,8) and the one-way tile (3,8)
        p.update(press(Action.DOWN), LEDGE_ROOM);
        stepOn(LEDGE_ROOM, p, held(Action.DOWN), 10);
        assertEquals(PlayerState.CROUCH, p.state());
        assertEquals(ON_LEDGE_Y, p.y());
    }

    @Test
    void crouchOnSolidUnchanged() {
        Player p = new Player(200, STANDING_Y, sheet);
        stepOn(LEDGE_ROOM, p, Controls.NONE, 3);
        p.update(press(Action.DOWN), LEDGE_ROOM);
        stepOn(LEDGE_ROOM, p, held(Action.DOWN), 10);
        assertEquals(PlayerState.CROUCH, p.state());
        assertEquals(STANDING_Y, p.y());
    }

    @Test
    void landsOnOneWayFromAbove() {
        Player p = new Player(80, ON_LEDGE_Y - 48, sheet);
        stepOn(LEDGE_ROOM, p, Controls.NONE, 60);
        assertTrue(p.onGround());
        assertEquals(ON_LEDGE_Y, p.y());
    }

    @Test
    void fallsIntoPitWithoutError() {
        Player p = new Player(176, STANDING_Y, sheet);
        assertDoesNotThrow(() -> step(p, Controls.NONE, 60));
        assertTrue(p.y() > 192, "y was " + p.y());
    }
}
