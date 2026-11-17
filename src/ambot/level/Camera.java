package ambot.level;

/**
 * Follows a target with a small dead zone and stays inside the level (spec section 5.6).
 * A level smaller than the view is centred instead. Positions are whole pixels so tiles
 * never shimmer.
 */
public final class Camera {
    public static final int DEAD_ZONE_W = 48;
    public static final int DEAD_ZONE_H = 72;

    private final int viewW, viewH;
    private double x, y;

    public Camera(int viewW, int viewH) {
        this.viewW = viewW;
        this.viewH = viewH;
    }

    /** Moves only as far as needed to keep the target's centre inside the dead zone. */
    public void follow(Rect target, int levelW, int levelH) {
        double cx = target.x() + target.w() / 2, cy = target.y() + target.h() / 2;
        double left = (viewW - DEAD_ZONE_W) / 2.0, top = (viewH - DEAD_ZONE_H) / 2.0;
        if (cx < x + left) x = cx - left;
        else if (cx > x + left + DEAD_ZONE_W) x = cx - left - DEAD_ZONE_W;
        if (cy < y + top) y = cy - top;
        else if (cy > y + top + DEAD_ZONE_H) y = cy - top - DEAD_ZONE_H;
        clamp(levelW, levelH);
    }

    /** Centres on the target at once (level start, respawn). */
    public void snapTo(Rect target, int levelW, int levelH) {
        x = target.x() + target.w() / 2 - viewW / 2.0;
        y = target.y() + target.h() / 2 - viewH / 2.0;
        clamp(levelW, levelH);
    }

    private void clamp(int levelW, int levelH) {
        x = clampAxis(x, levelW, viewW);
        y = clampAxis(y, levelH, viewH);
    }

    private static double clampAxis(double v, int level, int view) {
        if (level <= view) return (level - view) / 2.0;
        return Math.max(0, Math.min(v, level - view));
    }

    public int x() {
        return (int) Math.round(x);
    }

    public int y() {
        return (int) Math.round(y);
    }

    /** The visible area in level pixels. */
    public Rect view() {
        return new Rect(x(), y(), viewW, viewH);
    }
}
