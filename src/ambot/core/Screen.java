package ambot.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Shows the latest finished frame, scaled up by a whole number and centred.
 * The game loop renders frames off-screen; this panel only copies them to the window.
 */
public final class Screen extends JPanel {
    private static final long serialVersionUID = 1L;

    private final int logicalW, logicalH;
    private transient volatile BufferedImage frame;

    public Screen(int logicalW, int logicalH) {
        this.logicalW = logicalW;
        this.logicalH = logicalH;
        setPreferredSize(new Dimension(logicalW * GameConfig.DEFAULT_SCALE, logicalH * GameConfig.DEFAULT_SCALE));
        setBackground(Color.BLACK);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
    }

    /** Called from the game loop thread with a fully drawn frame. */
    public void present(BufferedImage finished) {
        frame = finished;
        repaint();
    }

    public BufferedImage lastFrame() {
        return frame;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage f = frame;
        if (f == null) return;
        Graphics2D g2 = (Graphics2D) g;
        // With Windows display scaling (e.g. 125%) Swing pre-scales drawing; fit in real pixels instead,
        // then draw with an unscaled transform so every game pixel is a whole number of screen pixels.
        AffineTransform t = g2.getTransform();
        ScaleMath.Viewport v = ScaleMath.fitDevice(getWidth(), getHeight(), t.getScaleX(), t.getScaleY(), logicalW, logicalH);
        g2.setTransform(AffineTransform.getTranslateInstance(t.getTranslateX(), t.getTranslateY()));
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(f, v.offsetX(), v.offsetY(), v.width(), v.height(), null);
        g2.setTransform(t);
    }
}
