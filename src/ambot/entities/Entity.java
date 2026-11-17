package ambot.entities;

import java.awt.Graphics2D;

import ambot.level.Rect;

/** Anything placed in a level. (x, y) is the top-left of its hitbox, in logical pixels. */
public abstract class Entity {
    protected double x, y;
    protected final double width, height;

    protected Entity(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rect hitbox() {
        return new Rect(x, y, width, height);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public abstract void render(Graphics2D g);
}
