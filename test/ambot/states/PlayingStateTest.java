package ambot.states;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ambot.entities.Projectile;
import ambot.entities.player.PlayerState;
import ambot.entities.player.PlayerTuning;
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
    void hazardCostsOneHeartAndKnocksBack() {
        PlayingState p = start(TestLevels.ascii(
                "....................",
                "....................",
                "....................",
                "....................",
                ".P....^...........E.",
                "####################"));
        for (int i = 0; i < 60 && p.player().health() == 3; i++) tick(RIGHT, 1);
        assertEquals(2, p.player().health());
        assertEquals(0, p.deaths());
        assertTrue(p.player().x() != 17, "not sent back to the spawn");
        assertTrue(p.player().vx() < 0, "knocked backwards");
    }

    static final Level PIT = TestLevels.ascii(
            "....................",
            "....................",
            "....................",
            "....................",
            ".P................E.",
            "####....############");

    private void runUntilGameOver(int maxTicks) {
        for (int i = 0; i < maxTicks && !(ctx.states().top() instanceof GameOverState); i++) tick(RIGHT, 1);
        assertInstanceOf(GameOverState.class, ctx.states().top());
    }

    @Test
    void pitIsGameOver() {
        PlayingState p = start(PIT);
        runUntilGameOver(PlayerTuning.DEATH_TICKS + 200);
        assertEquals(1, p.deaths());
    }

    @Test
    void pitWhileInvincibleIsStillGameOver() {
        PlayingState p = start(PIT);
        p.player().damage(1, 0); // knocked right, toward the pit, and invincible
        assertTrue(p.player().isInvincible());
        runUntilGameOver(PlayerTuning.DEATH_TICKS + 200);
        assertEquals(1, p.deaths());
    }

    @Test
    void gameOverOpensOnce() {
        PlayingState p = start(PIT);
        runUntilGameOver(PlayerTuning.DEATH_TICKS + 200);
        tick(Controls.NONE, 200);
        assertEquals(2, ctx.states().size());
        assertInstanceOf(GameOverState.class, ctx.states().top());
        assertEquals(1, p.deaths());
    }

    @Test
    void retryRestoresHeartsEnemiesAndClearsBolts() {
        Level level = TestLevels.ascii(
                "..............................",
                "..............................",
                "..............................",
                "..............................",
                ".P.........w....s..........E..",
                "##############################");
        PlayingState p = start(level);
        int enemies = p.actors().enemies().size();
        p.actors().enemies().get(0).damage(1, 0);
        p.actors().spawn(new Projectile(300, 40, 2.5));
        p.player().kill();
        for (int i = 0; i < PlayerTuning.DEATH_TICKS + 5 && !(ctx.states().top() instanceof GameOverState); i++) {
            tick(Controls.NONE, 1);
        }
        assertInstanceOf(GameOverState.class, ctx.states().top());
        tick(Controls.NONE, InputGrace.GRACE_TICKS);
        tick(press(Action.CONFIRM), 1); // Retry
        tick(Controls.NONE, 1);
        assertInstanceOf(PlayingState.class, ctx.states().top());
        assertEquals(3, p.player().health());
        assertEquals(PlayerState.IDLE, p.player().state());
        assertTrue(!p.player().isInvincible());
        assertEquals(enemies, p.actors().enemies().size());
        p.actors().enemies().forEach(e -> assertEquals(e.maxHealth(), e.health()));
        assertTrue(p.actors().projectiles().isEmpty());
        assertEquals(p.respawnPoint().x(), p.player().x() + 7, 2.5, "back at the respawn point");
    }

    @Test
    void exitIgnoredWhileDead() {
        PlayingState p = start(wide(20, 5, Map.of()));
        p.player().respawn(80 - 7, 80 - 28); // standing inside the exit
        p.player().kill();
        tick(Controls.NONE, 30);
        assertInstanceOf(PlayingState.class, ctx.states().top(), "no Level Complete for a dead player");
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
        tick(Controls.NONE, InputGrace.GRACE_TICKS);
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
        tick(Controls.NONE, InputGrace.GRACE_TICKS);
        tick(press(Action.CONFIRM), 1);
        tick(Controls.NONE, 1);
        assertInstanceOf(MainMenuState.class, ctx.states().top());
    }
}
