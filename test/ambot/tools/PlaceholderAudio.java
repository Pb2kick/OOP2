package ambot.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ambot.audio.Music;
import ambot.audio.Sfx;

/**
 * Synthesizes the placeholder sound effects and music (16-bit mono 22050 Hz WAV), used until
 * M4b brings free-licensed packs. Deterministic, so a test can check the committed files are
 * current. Not shipped: run through {@code .\build.cmd levels}.
 */
public final class PlaceholderAudio {
    static final int RATE = 22050;
    private static final double AMP = 0.35;

    private PlaceholderAudio() {
    }

    private enum Wave { SINE, SQUARE }

    /** One tone: a frequency sweep from f0 to f1 over a duration, with a short attack and linear fade. */
    private record Tone(double f0, double f1, double seconds, Wave wave) {
    }

    public static byte[] render(Sfx s) {
        Tone[] tones = switch (s) {
            case JUMP -> new Tone[] { new Tone(300, 620, 0.12, Wave.SQUARE) };
            case SWING -> new Tone[] { new Tone(900, 260, 0.09, Wave.SINE) };
            case HIT -> new Tone[] { new Tone(190, 90, 0.10, Wave.SQUARE) };
            case HURT -> new Tone[] { new Tone(420, 140, 0.24, Wave.SQUARE) };
            case PICKUP -> new Tone[] { new Tone(660, 660, 0.06, Wave.SINE), new Tone(880, 880, 0.06, Wave.SINE),
                    new Tone(1320, 1320, 0.08, Wave.SINE) };
            case SHOOT -> new Tone[] { new Tone(1200, 600, 0.07, Wave.SQUARE) };
            case DEATH -> new Tone[] { new Tone(320, 60, 0.38, Wave.SQUARE) };
            case LEVEL_CLEAR -> new Tone[] { new Tone(523, 523, 0.08, Wave.SQUARE), new Tone(659, 659, 0.08, Wave.SQUARE),
                    new Tone(784, 784, 0.08, Wave.SQUARE), new Tone(1046, 1046, 0.12, Wave.SQUARE) };
            case MENU_MOVE -> new Tone[] { new Tone(880, 880, 0.04, Wave.SINE) };
            case MENU_SELECT -> new Tone[] { new Tone(660, 990, 0.08, Wave.SINE) };
        };
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        for (Tone t : tones) writeTone(pcm, t, AMP);
        return wav(pcm.toByteArray());
    }

    public static byte[] render(Music m) {
        // MIDI note numbers; 0 is a rest
        int[] melody;
        double noteSeconds;
        Wave wave;
        double amp;
        switch (m) {
            case MENU -> {
                melody = new int[] { 60, 64, 67, 72, 71, 67, 64, 62, 60, 64, 67, 69, 67, 64, 62, 0 };
                noteSeconds = 0.5;
                wave = Wave.SINE;
                amp = 0.22;
            }
            case WORLD1 -> {
                melody = new int[] { 64, 67, 71, 67, 72, 71, 67, 64, 62, 65, 69, 65, 71, 69, 65, 62,
                        60, 64, 67, 64, 69, 67, 64, 60, 62, 65, 67, 71, 72, 0, 67, 0 };
                noteSeconds = 0.25;
                wave = Wave.SQUARE;
                amp = 0.14;
            }
            default -> throw new IllegalArgumentException(m.name());
        }
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        for (int note : melody) {
            if (note == 0) {
                writeSilence(pcm, noteSeconds);
            } else {
                double f = 440 * Math.pow(2, (note - 69) / 12.0);
                writeTone(pcm, new Tone(f, f, noteSeconds, wave), amp);
            }
        }
        return wav(pcm.toByteArray());
    }

    private static void writeTone(ByteArrayOutputStream out, Tone t, double amp) {
        int n = (int) Math.round(t.seconds() * RATE);
        int attack = Math.max(1, RATE / 200);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double progress = (double) i / n;
            double f = t.f0() + (t.f1() - t.f0()) * progress;
            phase += 2 * Math.PI * f / RATE;
            double v = t.wave() == Wave.SINE ? Math.sin(phase) : (Math.sin(phase) >= 0 ? 1 : -1);
            double env = Math.min(1.0, (double) i / attack) * (1.0 - progress);
            writeSample(out, v * env * amp);
        }
    }

    private static void writeSilence(ByteArrayOutputStream out, double seconds) {
        int n = (int) Math.round(seconds * RATE);
        for (int i = 0; i < n; i++) writeSample(out, 0);
    }

    private static void writeSample(ByteArrayOutputStream out, double v) {
        int s = (int) Math.round(Math.max(-1, Math.min(1, v)) * 32767);
        out.write(s & 0xFF);
        out.write((s >> 8) & 0xFF);
    }

    /** Wraps 16-bit mono PCM in a WAV (RIFF) header. */
    private static byte[] wav(byte[] pcm) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + pcm.length);
        ascii(out, "RIFF");
        int32(out, 36 + pcm.length);
        ascii(out, "WAVE");
        ascii(out, "fmt ");
        int32(out, 16);
        int16(out, 1); // PCM
        int16(out, 1); // mono
        int32(out, RATE);
        int32(out, RATE * 2);
        int16(out, 2);
        int16(out, 16);
        ascii(out, "data");
        int32(out, pcm.length);
        out.writeBytes(pcm);
        return out.toByteArray();
    }

    private static void ascii(ByteArrayOutputStream out, String s) {
        for (char c : s.toCharArray()) out.write(c);
    }

    private static void int16(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
    }

    private static void int32(ByteArrayOutputStream out, int v) {
        int16(out, v & 0xFFFF);
        int16(out, (v >>> 16) & 0xFFFF);
    }

    public static void main(String[] args) throws IOException {
        Path res = Path.of(args.length > 0 ? args[0] : "resources");
        for (Sfx s : Sfx.values()) write(res, s.path(), render(s));
        for (Music m : Music.values()) write(res, m.path(), render(m));
        System.out.println("placeholder audio written to " + res.resolve("audio"));
    }

    private static void write(Path res, String resourcePath, byte[] data) throws IOException {
        Path out = res.resolve(resourcePath.substring(1));
        Files.createDirectories(out.getParent());
        Files.write(out, data);
    }
}
