package ambot.core;

/**
 * Runs game updates at a fixed rate on its own thread and renders after each batch
 * of updates. Sleeps between updates so it does not occupy a whole CPU core.
 */
public final class GameLoop implements Runnable {
    private final FixedStep step;
    private final Runnable tick, render;
    private volatile boolean running;
    private volatile int ups, fps;
    private Thread thread;

    public GameLoop(FixedStep step, Runnable tick, Runnable render) {
        this.step = step;
        this.tick = tick;
        this.render = render;
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
            for (int i = 0; i < n; i++) {
                tick.run();
                updates++;
            }
            if (n > 0) {
                render.run();
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
