package ambot.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FocusGuardTest {

    @Test
    void normalFocusLossReleasesAndPauses() {
        assertEquals(FocusGuard.Loss.RELEASE_AND_PAUSE, new FocusGuard().focusLost());
    }

    @Test
    void focusLossDuringFullscreenToggleStillReleasesInput() {
        FocusGuard f = new FocusGuard();
        f.beginToggle();
        assertEquals(FocusGuard.Loss.RELEASE_ONLY, f.focusLost(), "keys must still be released so none stay stuck");
    }

    @Test
    void lateFocusLossBeforeRegainStillCountsAsToggle() {
        FocusGuard f = new FocusGuard();
        f.beginToggle();
        f.focusLost();
        assertEquals(FocusGuard.Loss.RELEASE_ONLY, f.focusLost(), "the toggle ends only when focus comes back");
    }

    @Test
    void pausingResumesAfterFocusComesBack() {
        FocusGuard f = new FocusGuard();
        f.beginToggle();
        f.focusGained();
        assertEquals(FocusGuard.Loss.RELEASE_AND_PAUSE, f.focusLost());
    }
}
