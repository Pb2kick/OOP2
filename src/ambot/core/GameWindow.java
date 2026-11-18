package ambot.core;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/** The game's JFrame: centred, resizable, with a borderless fullscreen toggle. Use on the event thread. */
public final class GameWindow {
    private final JFrame frame;
    private final Screen screen;
    private final FocusGuard focusGuard = new FocusGuard();
    private Runnable releaseInput = () -> { };
    private Runnable pause = () -> { };
    private boolean fullscreen;
    private Rectangle windowedBounds;

    public GameWindow(Screen screen, String title) {
        this.screen = screen;
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(screen);
        frame.setResizable(true);
        frame.pack(); // first pack makes the decorations' size known
        Insets in = frame.getInsets();
        int chromeW = in.left + in.right, chromeH = in.top + in.bottom;
        // Largest whole scale (max 2x) whose window fits above the taskbar, e.g. 1x on a 1366x768 laptop.
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int scale = ScaleMath.initialWindowScale(usable.width, usable.height, chromeW, chromeH,
                GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT, GameConfig.DEFAULT_SCALE);
        screen.setPreferredSize(new Dimension(GameConfig.LOGICAL_WIDTH * scale, GameConfig.LOGICAL_HEIGHT * scale));
        frame.pack();
        frame.setMinimumSize(new Dimension(GameConfig.LOGICAL_WIDTH + chromeW, GameConfig.LOGICAL_HEIGHT + chromeH));
        frame.setLocationRelativeTo(null); // after pack(), so the real size is centred
        frame.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                FocusGuard.Loss loss = focusGuard.focusLost();
                releaseInput.run();
                if (loss == FocusGuard.Loss.RELEASE_AND_PAUSE) pause.run();
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
                focusGuard.focusGained();
            }
        });
        frame.setVisible(true);
    }

    /**
     * Focus loss always releases input (so no key stays stuck); it also pauses,
     * except while the fullscreen toggle is re-creating the window.
     */
    public void onFocusLost(Runnable releaseInput, Runnable pause) {
        this.releaseInput = releaseInput;
        this.pause = pause;
    }

    /** Switches between the normal window and borderless fullscreen on the current monitor. */
    public void toggleFullscreen() {
        focusGuard.beginToggle();
        frame.dispose();
        if (!fullscreen) {
            windowedBounds = frame.getBounds();
            frame.setUndecorated(true);
            frame.setBounds(frame.getGraphicsConfiguration().getBounds());
        } else {
            frame.setUndecorated(false);
            frame.setBounds(windowedBounds);
        }
        fullscreen = !fullscreen;
        frame.setVisible(true);
        SwingUtilities.invokeLater(screen::requestFocusInWindow);
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void dispose() {
        frame.dispose();
    }
}
