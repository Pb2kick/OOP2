package ambot.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FixedStepTest {
    static final long STEP = 1_000_000_000L / 60; // 16_666_666

    @Test
    void oneStepPerStepTime() {
        assertEquals(1, new FixedStep(60, 5).advance(STEP));
    }

    @Test
    void accumulatesPartialTime() {
        FixedStep f = new FixedStep(60, 5);
        assertEquals(0, f.advance(STEP / 2));
        assertEquals(1, f.advance(STEP / 2 + 1));
    }

    @Test
    void capsCatchUpAndDropsBacklog() {
        FixedStep f = new FixedStep(60, 5);
        assertEquals(5, f.advance(1_000_000_000L));
        assertEquals(0, f.advance(0));
    }

    @Test
    void reportsTimeUntilNextStep() {
        FixedStep f = new FixedStep(60, 5);
        f.advance(STEP / 4);
        assertEquals(STEP - STEP / 4, f.nanosUntilNextStep());
    }

    @Test
    void negativeElapsedCountsAsZero() {
        assertEquals(0, new FixedStep(60, 5).advance(-5_000_000));
    }
}
