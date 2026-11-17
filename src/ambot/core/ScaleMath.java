package ambot.core;

/** Fits the fixed-size logical canvas into a window at the largest whole-number scale. */
public final class ScaleMath {
    private ScaleMath() {
    }

    /** Where and how big the scaled canvas is drawn inside the window. */
    public record Viewport(int scale, int offsetX, int offsetY, int width, int height) {
    }

    /**
     * Fits in real screen pixels when Windows display scaling is active (e.g. 125%):
     * Swing reports component sizes in scaled units, so convert to device pixels first.
     * The returned viewport is in device pixels.
     */
    public static Viewport fitDevice(int componentW, int componentH, double scaleX, double scaleY,
            int logicalW, int logicalH) {
        return fit((int) Math.round(componentW * scaleX), (int) Math.round(componentH * scaleY), logicalW, logicalH);
    }

    /**
     * The largest whole scale (at least 1, at most {@code maxScale}) at which the window,
     * including its decorations ({@code chromeW} x {@code chromeH}), fits the usable screen area.
     */
    public static int initialWindowScale(int usableW, int usableH, int chromeW, int chromeH,
            int logicalW, int logicalH, int maxScale) {
        int fits = Math.min((usableW - chromeW) / logicalW, (usableH - chromeH) / logicalH);
        return Math.max(1, Math.min(maxScale, fits));
    }

    public static Viewport fit(int windowW, int windowH, int logicalW, int logicalH) {
        int scale = Math.max(1, Math.min(windowW / logicalW, windowH / logicalH));
        int width = logicalW * scale;
        int height = logicalH * scale;
        return new Viewport(scale, (windowW - width) / 2, (windowH - height) / 2, width, height);
    }
}
