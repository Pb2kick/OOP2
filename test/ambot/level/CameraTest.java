package ambot.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CameraTest {
    static final int LW = 2000, LH = 1000;
    private final Camera cam = new Camera(640, 360);

    /** A 14x28 box centred on (cx, cy). */
    static Rect at(double cx, double cy) {
        return new Rect(cx - 7, cy - 14, 14, 28);
    }

    @Test
    void snapCentresOnTarget() {
        cam.snapTo(at(1000, 500), LW, LH);
        assertEquals(680, cam.x());
        assertEquals(320, cam.y());
        assertEquals(new Rect(680, 320, 640, 360), cam.view());
    }

    @Test
    void clampsAtLeftAndTopEdges() {
        cam.snapTo(at(20, 20), LW, LH);
        assertEquals(0, cam.x());
        assertEquals(0, cam.y());
    }

    @Test
    void clampsAtRightAndBottomEdges() {
        cam.snapTo(at(1990, 990), LW, LH);
        assertEquals(1360, cam.x());
        assertEquals(640, cam.y());
    }

    @Test
    void centresLevelSmallerThanView() {
        cam.snapTo(at(100, 100), 320, 200);
        assertEquals(-160, cam.x());
        assertEquals(-80, cam.y());
        cam.follow(at(300, 190), 320, 200);
        assertEquals(-160, cam.x());
        assertEquals(-80, cam.y());
    }

    @Test
    void holdsStillInsideDeadZone() {
        cam.snapTo(at(1000, 500), LW, LH);
        cam.follow(at(1020, 500), LW, LH);
        assertEquals(680, cam.x());
    }

    @Test
    void followsPastDeadZone() {
        cam.snapTo(at(1000, 500), LW, LH);
        cam.follow(at(1040, 500), LW, LH);
        assertEquals(696, cam.x());
        cam.follow(at(940, 500), LW, LH);
        assertEquals(644, cam.x(), "dead zone left edge is 24 px left of centre");
    }

    @Test
    void followsVerticallyPastDeadZone() {
        cam.snapTo(at(1000, 500), LW, LH);
        cam.follow(at(1000, 530), LW, LH);
        assertEquals(320, cam.y(), "inside the 72 px tall dead zone");
        cam.follow(at(1000, 560), LW, LH);
        assertEquals(344, cam.y());
        cam.follow(at(1000, 430), LW, LH);
        assertEquals(286, cam.y());
    }

    @Test
    void roundsToWholePixels() {
        cam.snapTo(at(1000.6, 500.4), LW, LH);
        assertEquals(681, cam.x());
        assertEquals(320, cam.y());
    }
}
