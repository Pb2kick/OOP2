package ambot.core;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import ambot.entities.player.PlayerAnimations;
import ambot.gfx.SpriteSheet;
import ambot.input.InputState;
import ambot.input.KeyBindings;
import ambot.input.SwingInputAdapter;
import ambot.states.GameContext;
import ambot.states.MainMenuState;
import ambot.states.StateManager;

/**
 * Wires the window, input, states and loop together. Construct on the Swing event thread.
 * All game logic runs on the loop thread; the event thread only feeds input and paints frames.
 */
public final class Game implements GameContext {
    private static final int W = GameConfig.LOGICAL_WIDTH, H = GameConfig.LOGICAL_HEIGHT;

    private final InputState input = new InputState(KeyBindings.defaults());
    private final StateManager states = new StateManager();
    private final SpriteSheet playerSheet =
            SpriteSheet.load(PlayerAnimations.SHEET_PATH, PlayerAnimations.CELL_W, PlayerAnimations.CELL_H);
    private final Screen screen = new Screen(W, H);
    private final GameWindow window;
    private final GameLoop loop;
    private final BufferedImage[] buffers = {
            new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB),
            new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB) };
    private int back;
    private final AtomicBoolean focusLost = new AtomicBoolean();
    private volatile boolean debugOverlay;

    public Game() {
        SwingInputAdapter adapter = new SwingInputAdapter(input);
        screen.addKeyListener(adapter);
        screen.addMouseListener(adapter);
        screen.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) window.toggleFullscreen();
                if (e.getKeyCode() == KeyEvent.VK_F3) debugOverlay = !debugOverlay;
            }
        });
        window = new GameWindow(screen, GameConfig.TITLE);
        window.onFocusLost(() -> focusLost.set(true));
        loop = new GameLoop(new FixedStep(GameConfig.UPS, GameConfig.MAX_CATCH_UP), this::tick, this::render);
        states.replaceAll(new MainMenuState(this));
    }

    public void start() {
        screen.requestFocusInWindow();
        loop.start();
    }

    private void tick() {
        if (focusLost.getAndSet(false)) {
            input.releaseAll();
            states.focusLost();
        }
        states.update(input.poll());
    }

    private void render() {
        BufferedImage buffer = buffers[back];
        back ^= 1;
        Graphics2D g = buffer.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, H);
        states.render(g);
        if (debugOverlay) {
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            g.setColor(Color.YELLOW);
            g.drawString("UPS " + loop.ups() + "  FPS " + loop.fps(), W - 110, 12);
        }
        g.dispose();
        screen.present(buffer);
    }

    @Override
    public StateManager states() {
        return states;
    }

    @Override
    public SpriteSheet playerSheet() {
        return playerSheet;
    }

    @Override
    public void quit() {
        loop.stop();
        SwingUtilities.invokeLater(window::dispose);
        System.exit(0);
    }

    public InputState input() {
        return input;
    }

    public Screen screen() {
        return screen;
    }

    public GameLoop loop() {
        return loop;
    }
}
