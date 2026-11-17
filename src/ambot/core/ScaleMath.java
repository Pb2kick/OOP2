package ambot.core;

/** Fits the fixed-size logical canvas into a window at the largest whole-number scale. */
public final class ScaleMath {
    private ScaleMath() {
    }

    /** Where and how big the scaled canvas is drawn inside the window. */
    public record Viewport(int scale, int offsetX, int offsetY, int width, int height) {
    }

    public static Viewport fit(int windowW, int windowH, int logicalW, int logicalH) {
        int scale = Math.max(1, Math.min(windowW / logicalW, windowH / logicalH));
        int width = logicalW * scale;
        int height = logicalH * scale;
        return new Viewport(scale, (windowW - width) / 2, (windowH - height) / 2, width, height);
    }
}
