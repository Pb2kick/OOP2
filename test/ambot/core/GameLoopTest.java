package ambot.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class GameLoopTest {

    @Test
    void crashGoesToHandlerAndLoopKeepsRendering() throws Exception {
        AtomicInteger ticks = new AtomicInteger(), renders = new AtomicInteger(), crashes = new AtomicInteger();
        CountDownLatch crashed = new CountDownLatch(1);
        GameLoop loop = new GameLoop(new FixedStep(60, 5), () -> {
            if (ticks.incrementAndGet() == 3) throw new IllegalStateException("boom");
        }, renders::incrementAndGet, e -> {
            crashes.incrementAndGet();
            crashed.countDown();
        });
        loop.start();
        try {
            assertTrue(crashed.await(2, TimeUnit.SECONDS), "the handler was called");
            int afterCrash = renders.get();
            Thread.sleep(300);
            assertTrue(renders.get() > afterCrash, "still rendering after the crash");
            assertEquals(1, crashes.get());
            assertTrue(loop.isRunning());
        } finally {
            loop.stop();
        }
    }

    @Test
    void secondCrashWithinASecondStopsLoop() throws Exception {
        AtomicInteger crashes = new AtomicInteger();
        GameLoop loop = new GameLoop(new FixedStep(60, 5), () -> {
            throw new IllegalStateException("always");
        }, () -> { }, e -> crashes.incrementAndGet());
        loop.start();
        try {
            for (int i = 0; i < 100 && loop.isRunning(); i++) Thread.sleep(10);
            assertFalse(loop.isRunning(), "a crash loop must stop the game loop");
            assertEquals(1, crashes.get(), "the handler runs for the first crash only");
        } finally {
            loop.stop();
        }
    }
}
