package ambot.core;

import java.awt.Dimension;
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
    private Runnable focusLostHandler = () -> { };
    private boolean fullscreen;
    private boolean toggling;
    private Rectangle windowedBounds;

    public GameWindow(Screen screen, String title) {
        this.screen = screen;
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(screen);
        frame.setResizable(true);
        frame.pack();
        Insets in = frame.getInsets();
        frame.setMinimumSize(new Dimension(GameConfig.LOGICAL_WIDTH + in.left + in.right,
                GameConfig.LOGICAL_HEIGHT + in.top + in.bottom));
        frame.setLocationRelativeTo(null); // after pack(), so the real size is centred
        frame.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                if (!toggling) focusLostHandler.run();
            }
        });
        frame.setVisible(true);
    }

    public void onFocusLost(Runnable handler) {
        focusLostHandler = handler;
    }

    /** Switches between the normal window and borderless fullscreen on the current monitor. */
    public void toggleFullscreen() {
        toggling = true;
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
        SwingUtilities.invokeLater(() -> {
            screen.requestFocusInWindow();
            toggling = false;
        });
    }

    public void dispose() {
        frame.dispose();
    }
}
