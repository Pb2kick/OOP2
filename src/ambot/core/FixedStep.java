package ambot.core;

/**
 * Fixed-timestep accumulator: turns elapsed wall-clock time into a whole number
 * of game updates, so the game runs at the same speed on any machine.
 */
public final class FixedStep {
    private final long stepNanos;
    private final int maxStepsPerAdvance;
    private long accumulator;

    public FixedStep(int updatesPerSecond, int maxStepsPerAdvance) {
        if (updatesPerSecond <= 0 || maxStepsPerAdvance <= 0) {
            throw new IllegalArgumentException("rates must be positive");
        }
        this.stepNanos = 1_000_000_000L / updatesPerSecond;
        this.maxStepsPerAdvance = maxStepsPerAdvance;
    }

    /**
     * Adds elapsed time and returns how many updates to run now. After a long stall
     * the count is capped and the backlog dropped, so the game never fast-forwards.
     */
    public int advance(long elapsedNanos) {
        accumulator += Math.max(0, elapsedNanos);
        long steps = accumulator / stepNanos;
        if (steps > maxStepsPerAdvance) {
            accumulator = 0;
            return maxStepsPerAdvance;
        }
        accumulator -= steps * stepNanos;
        return (int) steps;
    }

    public long nanosUntilNextStep() {
        return stepNanos - accumulator;
    }

    public long stepNanos() {
        return stepNanos;
    }
}
