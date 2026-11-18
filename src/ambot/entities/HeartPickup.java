package ambot.entities;

import java.awt.Graphics2D;

import ambot.level.Spot;
import ambot.ui.HeartIcon;

/** A heart refill standing on a spot, bobbing gently. Heals 1, and only when the collector is hurt. */
public final class HeartPickup extends Entity {
    public static final int W = 10, H = 9;
    private static final int BOB_PERIOD = 60;

    private int age;
    private boolean collected;

    public HeartPickup(Spot feet) {
        super(feet.x() - W / 2.0, feet.y() - H, W, H);
    }

    /** Advances the bob animation; the hitbox never moves. */
    public void update() {
        age++;
    }

    public boolean tryCollect(Damageable who) {
        if (collected || who.isDead() || who.health() >= who.maxHealth()) return false;
        who.heal(1);
        collected = true;
        return true;
    }

    public boolean collected() {
        return collected;
    }

    @Override
    public void render(Graphics2D g) {
        if (collected) return;
        int bob = (int) Math.round(2 * Math.sin(2 * Math.PI * age / BOB_PERIOD));
        HeartIcon.draw(g, (int) Math.round(x) + (W - HeartIcon.W) / 2, (int) Math.round(y) + (H - HeartIcon.H) + bob, true);
    }
}
