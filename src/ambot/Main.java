package ambot;

import javax.swing.SwingUtilities;

import ambot.core.Game;

/** Entry point: starts AMBOT. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Game().start());
    }
}
