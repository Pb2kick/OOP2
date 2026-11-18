package ambot.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ambot.level.Rect;
import ambot.level.Spot;

class HeartPickupTest {
    /** A minimal Damageable with 3 health. */
    static final class Body implements Damageable {
        int health = 3;
        @Override public int health() { return health; }
        @Override public int maxHealth() { return 3; }
        @Override public boolean isDead() { return health <= 0; }
        @Override public boolean damage(int amount, double fromX) { health -= amount; return true; }
        @Override public void heal(int amount) { health = Math.min(3, health + amount); }
    }

    @Test
    void healsOnlyWhenHurt() {
        HeartPickup heart = new HeartPickup(new Spot(40, 64));
        Body body = new Body();
        assertFalse(heart.tryCollect(body), "full health: leave the heart for later");
        assertFalse(heart.collected());
        body.damage(1, 0);
        assertTrue(heart.tryCollect(body));
        assertEquals(3, body.health());
        assertTrue(heart.collected());
        body.damage(1, 0);
        assertFalse(heart.tryCollect(body), "a collected heart can't be used twice");
    }

    @Test
    void standsOnItsSpot() {
        Rect box = new HeartPickup(new Spot(40, 64)).hitbox();
        assertEquals(64, box.bottom());
        assertEquals(40, box.x() + box.w() / 2);
    }

    @Test
    void bobKeepsHitboxStill() {
        HeartPickup heart = new HeartPickup(new Spot(40, 64));
        Rect before = heart.hitbox();
        for (int i = 0; i < 30; i++) heart.update();
        assertEquals(before, heart.hitbox());
    }
}
