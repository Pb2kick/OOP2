package ambot.ui;

/** One selectable line in a {@link Menu}. */
public record MenuItem(String label, Runnable action, boolean enabled) {

    public static MenuItem of(String label, Runnable action) {
        return new MenuItem(label, action, true);
    }

    public static MenuItem disabled(String label) {
        return new MenuItem(label, () -> { }, false);
    }
}
