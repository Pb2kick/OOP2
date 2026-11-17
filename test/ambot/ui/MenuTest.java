package ambot.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import ambot.input.Action;
import ambot.input.Controls;

class MenuTest {
    private final AtomicInteger creditsRuns = new AtomicInteger();

    private Menu standardMenu() {
        return new Menu(List.of(
                MenuItem.of("New Game", () -> { }),
                MenuItem.disabled("Options"),
                MenuItem.of("Credits", creditsRuns::incrementAndGet),
                MenuItem.of("Quit", () -> { })));
    }

    private static Controls press(Action a) {
        return Controls.of(Set.of(), Set.of(a), Set.of());
    }

    @Test
    void startsOnFirstEnabled() {
        assertEquals(0, standardMenu().selectedIndex());
        Menu m = new Menu(List.of(MenuItem.disabled("X"), MenuItem.of("Y", () -> { })));
        assertEquals(1, m.selectedIndex());
    }

    @Test
    void downSkipsDisabled() {
        Menu m = standardMenu();
        m.update(press(Action.DOWN));
        assertEquals(2, m.selectedIndex());
    }

    @Test
    void wrapsBothWays() {
        Menu m = standardMenu();
        m.update(press(Action.UP));
        assertEquals(3, m.selectedIndex());
        m.update(press(Action.DOWN));
        assertEquals(0, m.selectedIndex());
    }

    @Test
    void confirmRunsSelectedAction() {
        Menu m = standardMenu();
        m.update(press(Action.DOWN));
        m.update(press(Action.CONFIRM));
        assertEquals(1, creditsRuns.get());
    }

    @Test
    void rejectsAllDisabled() {
        assertThrows(IllegalArgumentException.class, () -> new Menu(List.of(MenuItem.disabled("A"))));
    }
}
