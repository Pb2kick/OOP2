package ambot.states;

import ambot.audio.Audio;
import ambot.audio.Sfx;
import ambot.entities.enemies.Enemy;

/**
 * Turns what changed during one tick of play into sound effects, so the player, enemies and
 * pickups never need to know about audio. Compare a snapshot taken before the tick with one
 * taken after it.
 */
public final class SoundCues {
    private SoundCues() {
    }

    /** The bits of play state that sounds react to. */
    public record Snapshot(int health, int swingId, boolean airborneUp, int enemyHealthTotal, int pickups,
            int projectiles, boolean dead) {

        public static Snapshot of(PlayingState p) {
            int enemyHealth = 0;
            for (Enemy e : p.actors().enemies()) enemyHealth += e.health();
            boolean up = !p.player().onGround() && p.player().vy() < 0;
            return new Snapshot(p.player().health(), p.player().swingId(), up, enemyHealth,
                    p.actors().pickups().size(), p.actors().projectiles().size(), p.player().isDead());
        }
    }

    public static void observe(Snapshot before, Snapshot after, Audio audio) {
        boolean lostHealth = after.health() < before.health();
        if (after.dead() && !before.dead()) audio.play(Sfx.DEATH);
        else if (lostHealth) audio.play(Sfx.HURT);
        if (after.airborneUp() && !before.airborneUp() && !lostHealth && !after.dead()) audio.play(Sfx.JUMP);
        if (after.swingId() > before.swingId()) audio.play(Sfx.SWING);
        if (after.enemyHealthTotal() < before.enemyHealthTotal()) audio.play(Sfx.HIT);
        if (after.pickups() < before.pickups() && after.health() > before.health()) audio.play(Sfx.PICKUP);
        if (after.projectiles() > before.projectiles()) audio.play(Sfx.SHOOT);
    }
}
