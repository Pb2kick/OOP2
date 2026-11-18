package ambot.tools;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import ambot.core.Game;
import ambot.input.Action;

/**
 * Smoke test of the real game window (not a JUnit test): drives input through the game's
 * InputState, saves frames as PNGs, and reports update rate, frame rate and CPU use.
 * Run with: .\build.cmd smoke   (output in build\smoke)
 */
public final class SmokeRun {
    private static Game game;
    private static File out;
    private static final List<BufferedImage> shots = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        out = new File(args.length > 0 ? args[0] : "build/smoke");
        out.mkdirs();
        // never touch the player's real save in %APPDATA%
        System.setProperty("ambot.save", new File(out, "save.properties").getPath());
        new File(out, "save.properties").delete();
        AtomicReference<Game> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            Game g = new Game();
            g.start();
            ref.set(g);
        });
        game = ref.get();
        Thread.sleep(1500);
        snap("01-menu");

        tap(Action.CONFIRM);
        Thread.sleep(600);
        snap("02-room");

        hold(Action.RIGHT, 1000);
        snap("03-run");

        // keep running right: jump the first gap (level p1, columns 15-16) and the first step up (column 25)
        game.input().actionPressed(Action.RIGHT);
        Thread.sleep(520);
        hold(Action.JUMP, 350);
        Thread.sleep(800);
        hold(Action.JUMP, 350);
        Thread.sleep(600);
        snap("03b-scrolled");
        game.input().actionReleased(Action.RIGHT);

        game.input().actionPressed(Action.JUMP);
        Thread.sleep(250);
        snap("04-air");
        game.input().actionReleased(Action.JUMP);
        Thread.sleep(900);

        game.input().actionPressed(Action.DOWN);
        Thread.sleep(300);
        snap("05-crouch");
        game.input().actionReleased(Action.DOWN);
        Thread.sleep(200);

        tap(Action.ATTACK);
        for (int i = 1; i <= 6; i++) {
            Thread.sleep(100);
            snap("06-attack-" + i);
        }
        Thread.sleep(300);

        hold(Action.LEFT, 300);
        snap("07-left");

        tap(Action.PAUSE);
        Thread.sleep(300);
        snap("08-paused");

        com.sun.management.OperatingSystemMXBean os =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long cpu0 = os.getProcessCpuTime(), t0 = System.nanoTime();
        Thread.sleep(3000);
        double cores = (os.getProcessCpuTime() - cpu0) / (double) (System.nanoTime() - t0);
        System.out.printf("UPS %d  FPS %d  CPU %.2f%n", game.loop().ups(), game.loop().fps(), cores);

        writeContactSheet();
        System.exit(0);
    }

    private static void tap(Action a) throws InterruptedException {
        game.input().actionPressed(a);
        Thread.sleep(50);
        game.input().actionReleased(a);
    }

    private static void hold(Action a, long ms) throws InterruptedException {
        game.input().actionPressed(a);
        Thread.sleep(ms);
        game.input().actionReleased(a);
    }

    private static void snap(String name) throws IOException {
        BufferedImage live = game.screen().lastFrame();
        BufferedImage copy = new BufferedImage(live.getWidth(), live.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(live, 0, 0, null);
        g.dispose();
        ImageIO.write(copy, "png", new File(out, name + ".png"));
        shots.add(copy);
    }

    private static void writeContactSheet() throws IOException {
        int cols = 4, w = 640, h = 360;
        int rows = (shots.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(cols * w, rows * h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int i = 0; i < shots.size(); i++) {
            g.drawImage(shots.get(i), (i % cols) * w, (i / cols) * h, null);
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(out, "contact.png"));
    }
}
