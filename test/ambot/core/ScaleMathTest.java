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
    void initialWindowScaleFitsUsableScreen() {
        // chrome = window decorations (16 x 39 on Windows 10)
        assertEquals(2, ScaleMath.initialWindowScale(1920, 1040, 16, 39, 640, 360, 2), "1080p at 100%");
        assertEquals(1, ScaleMath.initialWindowScale(1366, 728, 16, 39, 640, 360, 2), "1366x768 laptop");
        assertEquals(1, ScaleMath.initialWindowScale(1280, 680, 16, 39, 640, 360, 2), "1080p at 150% scaling");
        assertEquals(2, ScaleMath.initialWindowScale(3840, 2120, 16, 39, 640, 360, 2), "never above the default");
        assertEquals(1, ScaleMath.initialWindowScale(500, 300, 16, 39, 640, 360, 2), "never below 1");
    }

    @Test
    void deviceScalingFitsRealPixels() {
        // a 1080p screen at 125% Windows scaling reports a 1536x864 component
        assertEquals(new ScaleMath.Viewport(3, 0, 0, 1920, 1080), ScaleMath.fitDevice(1536, 864, 1.25, 1.25, 640, 360));
        assertEquals(new ScaleMath.Viewport(2, 0, 0, 1280, 720), ScaleMath.fitDevice(1024, 576, 1.25, 1.25, 640, 360));
        assertEquals(ScaleMath.fit(1366, 768, 640, 360), ScaleMath.fitDevice(1366, 768, 1.0, 1.0, 640, 360));
    }

    @Test
    void tinyWindowKeepsScaleOne() {
        assertEquals(new ScaleMath.Viewport(1, -70, -30, 640, 360), ScaleMath.fit(500, 300, 640, 360));
    }
}
