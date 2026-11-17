package ambot.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ambot.level.TileCollider.Move;

class TileColliderTest {
    private final TileMap map = TileMap.fromAscii(TileMapTest.ROOM); // floor top at y=64

    @Test
    void landsFlushOnFloor() {
        assertEquals(new Move(44, true), TileCollider.moveY(new Rect(16, 40, 14, 20), 6, map));
    }

    @Test
    void fallsFreelyAboveFloor() {
        assertEquals(new Move(36, false), TileCollider.moveY(new Rect(16, 30, 14, 20), 6, map));
    }

    @Test
    void bumpsCeiling() {
        assertEquals(new Move(48, true), TileCollider.moveY(new Rect(48, 50, 14, 20), -6, map));
    }

    @Test
    void stopsAtRightMapEdge() {
        assertEquals(new Move(114, true), TileCollider.moveX(new Rect(110, 40, 14, 20), 6, map));
    }

    @Test
    void stopsAtLeftMapEdge() {
        assertEquals(new Move(0, true), TileCollider.moveX(new Rect(2, 40, 14, 20), -6, map));
    }

    @Test
    void stopsAgainstSolidTileSideways() {
        // box beside the ## block (x 48..80, y 32..48), moving right into it
        assertEquals(new Move(34, true), TileCollider.moveX(new Rect(30, 34, 14, 10), 6, map));
    }

    @Test
    void rejectsMovesLargerThanATile() {
        assertThrows(IllegalArgumentException.class, () -> TileCollider.moveX(new Rect(16, 40, 14, 20), 16.5, map));
        assertThrows(IllegalArgumentException.class, () -> TileCollider.moveY(new Rect(16, 40, 14, 20), -16.5, map));
    }

    @Test
    void onGroundExactlyWhenTouching() {
        assertTrue(TileCollider.onGround(new Rect(16, 44, 14, 20), map));
        assertFalse(TileCollider.onGround(new Rect(16, 43, 14, 20), map));
    }

    @Test
    void noTunnelingAtMaxFall() {
        Rect box = new Rect(16, 0, 14, 20);
        for (int i = 0; i < 30; i++) {
            box = box.withY(TileCollider.moveY(box, 6, map).position());
        }
        assertEquals(64, box.bottom());
    }

    @Test
    void fallsOutBottomThroughGap() {
        TileMap gap = TileMap.fromAscii(
                "........",
                "........",
                "###..###");
        Rect box = new Rect(50, 0, 14, 20);
        for (int i = 0; i < 20; i++) {
            Move m = TileCollider.moveY(box, 6, gap);
            assertFalse(m.blocked());
            box = box.withY(m.position());
        }
        assertTrue(box.y() > gap.heightPx());
    }
}
