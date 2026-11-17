package ambot.core;

/**
 * Decides what a window focus loss means. Input is always released (a key-up that
 * happens while unfocused would otherwise be lost and the key would stay "held").
 * The game pauses too, except while the window is being re-created for the
 * fullscreen toggle; that period ends when the window has focus again.
 * Used on the Swing event thread only.
 */
public final class FocusGuard {
    public enum Loss { RELEASE_ONLY, RELEASE_AND_PAUSE }

    private boolean toggling;

    public void beginToggle() {
        toggling = true;
    }

    public void focusGained() {
        toggling = false;
    }

    public Loss focusLost() {
        return toggling ? Loss.RELEASE_ONLY : Loss.RELEASE_AND_PAUSE;
    }
}
