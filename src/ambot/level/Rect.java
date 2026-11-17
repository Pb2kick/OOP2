package ambot.level;

/** An axis-aligned box in logical pixels: (x, y) is the top-left corner. */
public record Rect(double x, double y, double w, double h) {

    public double right() {
        return x + w;
    }

    public double bottom() {
        return y + h;
    }

    public Rect withX(double newX) {
        return new Rect(newX, y, w, h);
    }

    public Rect withY(double newY) {
        return new Rect(x, newY, w, h);
    }

    public boolean intersects(Rect o) {
        return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom();
    }
}
