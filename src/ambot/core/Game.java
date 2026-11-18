package ambot.core;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import ambot.audio.Audio;
import ambot.audio.SampledAudio;
import ambot.entities.player.PlayerAnimations;
import ambot.level.Campaign;
import ambot.save.FileSaveStore;
import ambot.save.Progress;
import ambot.gfx.SpriteSheet;
import ambot.input.InputState;
import ambot.input.KeyBindings;
import ambot.input.KeyRepeatFilter;
import ambot.input.SwingInputAdapter;
import ambot.states.CrashState;
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
    private final AtomicBoolean pauseRequested = new AtomicBoolean();
    private final KeyRepeatFilter oneShotKeys = new KeyRepeatFilter();
    private volatile boolean debugOverlay;
    private final Progress progress = new Progress(new FileSaveStore(FileSaveStore.defaultFile()));
    private final Audio audio = SampledAudio.createOrSilent();
    private final Campaign campaign = Campaign.standard();

    public Game() {
        audio.setMusicVolume(progress.data().musicVolume());
        audio.setSfxVolume(progress.data().sfxVolume());
        SwingInputAdapter adapter = new SwingInputAdapter(input);
        screen.addKeyListener(adapter);
        screen.addMouseListener(adapter);
        screen.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                // ignore OS auto-repeat so holding F11 does not flicker in and out of fullscreen
                if (code == KeyEvent.VK_F11 && oneShotKeys.firstPress(code)) {
                    window.toggleFullscreen();
                    progress.setFullscreen(window.isFullscreen());
                }
                if (code == KeyEvent.VK_F3 && oneShotKeys.firstPress(code)) debugOverlay = !debugOverlay;
            }

            @Override
            public void keyReleased(KeyEvent e) {
                oneShotKeys.released(e.getKeyCode());
            }
        });
        window = new GameWindow(screen, GameConfig.TITLE);
        window.onFocusLost(() -> {
            input.releaseAll();
            oneShotKeys.reset();
        }, () -> pauseRequested.set(true));
        loop = new GameLoop(new FixedStep(GameConfig.UPS, GameConfig.MAX_CATCH_UP), this::tick, this::render, e -> {
            e.printStackTrace();
            states.reset(new CrashState(this, e, CrashLog.write(CrashLog.defaultDir(), e)));
        });
        states.replaceAll(new MainMenuState(this));
        if (progress.data().fullscreen()) window.toggleFullscreen();
    }

    public void start() {
        screen.requestFocusInWindow();
        loop.start();
    }

    private void tick() {
        if (pauseRequested.getAndSet(false)) {
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

    @Override
    public Progress progress() {
        return progress;
    }

    @Override
    public Audio audio() {
        return audio;
    }

    @Override
    public Campaign campaign() {
        return campaign;
    }

    /** Called from the game loop (Options); the window is changed on the Swing event thread. */
    @Override
    public void setFullscreen(boolean on) {
        SwingUtilities.invokeLater(() -> {
            if (window.isFullscreen() != on) window.toggleFullscreen();
        });
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
