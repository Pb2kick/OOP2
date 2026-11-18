package ambot.ui;

import java.awt.Graphics2D;

import ambot.entities.Damageable;

/** The in-game heads-up display: one heart per point of the player's maximum health. */
public final class Hud {
    static final int X = 8, Y = 8, SPACING = 12;

    private Hud() {
    }

    public static void render(Graphics2D g, Damageable player) {
        for (int i = 0; i < player.maxHealth(); i++) {
            HeartIcon.draw(g, X + i * SPACING, Y, i < player.health());
        }
    }
}
