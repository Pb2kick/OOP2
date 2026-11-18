package ambot.states;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import ambot.input.Controls;
import ambot.level.LevelCatalog;
import ambot.level.Rect;

/**
 * Retry puts the player at the last checkpoint with every enemy back. Standing still there must
 * be safe: no enemy, revived or not, may reach or shoot a player waiting at a checkpoint.
 */
class CheckpointSafetyTest {
    static final int WAIT_TICKS = 300;

    @Test
    void standingStillAtEveryCheckpointIsSafe() {
        LevelCatalog catalog = LevelCatalog.placeholder();
        for (int i = 0; i < catalog.size(); i++) {
            List<Rect> checkpoints = catalog.load(i).checkpoints();
            for (Rect cp : checkpoints) {
                FakeContext ctx = new FakeContext();
                PlayingState playing = new PlayingState(ctx, catalog, i);
                ctx.states().replaceAll(playing);
                ctx.states().update(Controls.NONE);
                playing.player().respawn(cp.x() + cp.w() / 2 - 7, cp.bottom() - 28);
                for (int t = 0; t < WAIT_TICKS; t++) ctx.states().update(Controls.NONE);
                assertEquals(3, playing.player().health(),
                        "level " + (i + 1) + " checkpoint at x=" + (int) cp.x() + ": hurt while standing still");
            }
        }
    }
}
