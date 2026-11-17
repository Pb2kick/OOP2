package ambot.states;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.Level;
import ambot.level.LevelCatalog;
import ambot.level.Spot;
import ambot.level.TestLevels;

class PlayingStateTest {
    private final FakeContext ctx = new FakeContext();

    /** 20x6, floor top at y=80, spawn at tile (1,4), exit at tile (18,4). */
    static final Level FLAT = TestLevels.ascii(
            "....................",
            "....................",
            "....................",
            "....................",
            ".P................E.",
            "####################");

    static final Controls RIGHT = Controls.of(Set.of(Action.RIGHT), Set.of(), Set.of());

    static Controls press(Action a) {
        return Controls.of(Set.of(a), Set.of(a), Set.of());
    }

    private PlayingState start(Level... levels) {
        PlayingState p = new PlayingState(ctx, LevelCatalog.of(levels), 0);
        ctx.states().replaceAll(p);
        tick(Controls.NONE, 1);
        return p;
    }

    private void tick(Controls c, int n) {
        for (int i = 0; i < n; i++) ctx.states().update(c);
    }

    /** A one-row-of-floor level: P at column 1, E at {@code exitCol}, {@code width} tiles wide. */
    static Level wide(int width, int exitCol, Map<String, String> props) {
        char[] row = new char[width];
        Arrays.fill(row, '.');
        String empty = new String(row);
        row[1] = 'P';
        row[exitCol] = 'E';
        return TestLevels.ascii(props, empty, empty, empty, empty, new String(row), "#".repeat(width));
    }

    @Test
    void spawnsStandingOnSpawnSpot() {
        PlayingState p = start(FLAT);
        tick(Controls.NONE, 5);
        assertTrue(p.player().onGround());
        Spot spawn = FLAT.spawn();
        assertEquals(spawn.y(), p.player().y() + 28);
        assertEquals(spawn.x(), p.player().x() + 7);
    }

    @Test
    void cameraFollowsPlayer() {
        PlayingState p = start(wide(100, 97, Map.of()));
        assertEquals(0, p.camera().x());
        tick(RIGHT, 200);
        assertTrue(p.camera().x() > 0, "camera x was " + p.camera().x());
    }

    @Test
    void touchingCheckpointMovesRespawn() {
        PlayingState p = start(TestLevels.ascii(
                "....................",
                "....................",
                "....................",
                "....................",
                ".P..C.............E.",
                "####################"));
        assertEquals(FLAT.spawn(), p.respawnPoint());
        tick(RIGHT, 25); // hitbox from x=17 to x=67: overlaps the checkpoint at x 64..80
        assertEquals(new Spot(72, 80), p.respawnPoint(),
                "player at " + p.player().hitbox() + ", checkpoints " + p.level().checkpoints());
    }

    @Test
    void hazardSendsBackToRespawnPoint() {
        PlayingState p = start(TestLevels.ascii(
                "....................",
                "....................",
                "....................",
                "....................",
                ".P....^...........E.",
                "####################"));
        for (int i = 0; i < 60 && p.deaths() == 0; i++) tick(RIGHT, 1);
        assertEquals(1, p.deaths());
        assertEquals(24, p.player().x() + 7, "back at the spawn");
    }

    @Test
    void fallingOutOfMapRespawns() {
        PlayingState p = start(TestLevels.ascii(
                "....................",
                "....................",
                "....................",
                "....................",
                ".P................E.",
                "####....############"));
        for (int i = 0; i < 200 && p.deaths() == 0; i++) tick(RIGHT, 1);
        assertEquals(1, p.deaths());
        assertTrue(p.player().y() < p.level().heightPx(), "y was " + p.player().y());
    }

    @Test
    void exitOpensLevelComplete() {
        PlayingState p = start(wide(20, 5, Map.of()));
        for (int i = 0; i < 60 && !(ctx.states().top() instanceof LevelCompleteState); i++) tick(RIGHT, 1);
        assertInstanceOf(LevelCompleteState.class, ctx.states().top());
        double x = p.player().x();
        tick(RIGHT, 10);
        assertEquals(x, p.player().x(), "the level is frozen behind the Level Complete screen");
    }

    private void reachExit() {
        for (int i = 0; i < 60 && !(ctx.states().top() instanceof LevelCompleteState); i++) tick(RIGHT, 1);
        assertInstanceOf(LevelCompleteState.class, ctx.states().top());
    }

    @Test
    void confirmLoadsNextLevel() {
        start(wide(20, 5, Map.of("title", "A")), wide(30, 8, Map.of("title", "B")));
        reachExit();
        tick(press(Action.CONFIRM), 1);
        tick(Controls.NONE, 1);
        PlayingState next = assertInstanceOf(PlayingState.class, ctx.states().top());
        assertEquals("B", next.level().title());
        assertEquals(1, ctx.states().size());
    }

    @Test
    void confirmAfterLastLevelReturnsToMenu() {
        start(wide(20, 5, Map.of()));
        reachExit();
        tick(press(Action.CONFIRM), 1);
        tick(Controls.NONE, 1);
        assertInstanceOf(MainMenuState.class, ctx.states().top());
    }
}
