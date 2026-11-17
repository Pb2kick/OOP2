package ambot.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScaleMathTest {

    @Test
    void fits1280x720AtTwo() {
        assertEquals(new ScaleMath.Viewport(2, 0, 0, 1280, 720), ScaleMath.fit(1280, 720, 640, 360));
    }

    @Test
    void fits1080pAtThree() {
        assertEquals(new ScaleMath.Viewport(3, 0, 0, 1920, 1080), ScaleMath.fit(1920, 1080, 640, 360));
    }

    @Test
    void fits1366x768Centered() {
        assertEquals(new ScaleMath.Viewport(2, 43, 24, 1280, 720), ScaleMath.fit(1366, 768, 640, 360));
    }

    @Test
    void tinyWindowKeepsScaleOne() {
        assertEquals(new ScaleMath.Viewport(1, -70, -30, 640, 360), ScaleMath.fit(500, 300, 640, 360));
    }
}
