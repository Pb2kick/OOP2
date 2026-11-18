package ambot.core;

import java.util.function.Consumer;

/**
 * Runs game updates at a fixed rate on its own thread and renders after each batch
 * of updates. Sleeps between updates so it does not occupy a whole CPU core.
 */
public final class GameLoop implements Runnable {
    private static final long CRASH_WINDOW_NANOS = 1_000_000_000L;

    private final FixedStep step;
    private final Runnable tick, render;
    private final Consumer<RuntimeException> onCrash;
    private volatile boolean running;
    private volatile int ups, fps;
    private Thread thread;
    private long lastCrash;
    private boolean crashedBefore;

    /** A loop whose exceptions end it (they propagate out of the loop thread). */
    public GameLoop(FixedStep step, Runnable tick, Runnable render) {
        this(step, tick, render, e -> {
            throw e;
        });
    }

    /**
     * A loop that hands a RuntimeException from {@code tick} or {@code render} to {@code onCrash}
     * and keeps running; a second one within a second stops the loop instead.
     */
    public GameLoop(FixedStep step, Runnable tick, Runnable render, Consumer<RuntimeException> onCrash) {
        this.step = step;
        this.tick = tick;
        this.render = render;
        this.onCrash = onCrash;
    }

    public boolean isRunning() {
        return running;
    }

    private void crashed(RuntimeException e) {
        long now = System.nanoTime();
        if (crashedBefore && now - lastCrash < CRASH_WINDOW_NANOS) {
            running = false;
            e.printStackTrace();
            return;
        }
        crashedBefore = true;
        lastCrash = now;
        onCrash.accept(e);
    }

    public void start() {
        running = true;
        thread = new Thread(this, "game-loop");
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null && Thread.currentThread() != thread) {
            thread.interrupt();
        }
    }

    /** Updates counted over the last full second. */
    public int ups() {
        return ups;
    }

    /** Frames counted over the last full second. */
    public int fps() {
        return fps;
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        long secondStart = last;
        int updates = 0, frames = 0;
        while (running) {
            long now = System.nanoTime();
            int n = step.advance(now - last);
            last = now;
            for (int i = 0; i < n && running; i++) {
                try {
                    tick.run();
                } catch (RuntimeException e) {
                    crashed(e);
                }
                updates++;
            }
            if (n > 0 && running) {
                try {
                    render.run();
                } catch (RuntimeException e) {
                    crashed(e);
                }
                frames++;
            }
            if (now - secondStart >= 1_000_000_000L) {
                ups = updates;
                fps = frames;
                updates = 0;
                frames = 0;
                secondStart = now;
            }
            long wait = step.nanosUntilNextStep();
            if (wait > 2_000_000) {
                try {
                    Thread.sleep((wait - 1_000_000) / 1_000_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } else {
                Thread.onSpinWait();
            }
        }
    }
}
