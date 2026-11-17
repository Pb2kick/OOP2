package ambot.states;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.LevelCatalog;

/**
 * Playability check (spec section 5.6): replays the scripted inputs in
 * levels-src/placeholder/p&lt;N&gt;.route on catalog level N-1 and expects the level to be
 * finished without dying. Route lines are {@code <ticks> <ACTION+ACTION | ->}; '#' starts a comment.
 */
class RouteTest {
    static final Path ROUTES = Path.of("levels-src/placeholder");

    record Step(int ticks, Set<Action> held) {
    }

    @TestFactory
    Stream<DynamicTest> placeholderLevelsCanBeFinished() throws IOException {
        List<Path> routes;
        try (Stream<Path> s = Files.list(ROUTES)) {
            routes = s.filter(p -> p.getFileName().toString().matches("p\\d+\\.route")).sorted().toList();
        }
        assertTrue(routes.size() >= LevelCatalog.PLACEHOLDER_PATHS.size(), "every placeholder level needs a route");
        return routes.stream().map(r -> DynamicTest.dynamicTest(r.getFileName().toString(), () -> {
            String name = r.getFileName().toString();
            int index = Integer.parseInt(name.substring(1, name.indexOf('.'))) - 1;
            play(index, parse(r));
        }));
    }

    static List<Step> parse(Path file) throws IOException {
        List<Step> steps = new ArrayList<>();
        for (String raw : Files.readAllLines(file)) {
            int hash = raw.indexOf('#');
            String line = (hash >= 0 ? raw.substring(0, hash) : raw).trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            Set<Action> held = EnumSet.noneOf(Action.class);
            if (!parts[1].equals("-")) {
                for (String a : parts[1].split("\\+")) held.add(Action.valueOf(a));
            }
            steps.add(new Step(Integer.parseInt(parts[0]), held));
        }
        return steps;
    }

    static void play(int index, List<Step> steps) {
        FakeContext ctx = new FakeContext();
        PlayingState playing = new PlayingState(ctx, LevelCatalog.placeholder(), index);
        ctx.states().replaceAll(playing);
        Set<Action> previous = Set.of();
        int tick = 0;
        for (Step step : steps) {
            for (int i = 0; i < step.ticks(); i++, tick++) {
                Set<Action> pressed = new HashSet<>(step.held());
                pressed.removeAll(previous);
                Set<Action> released = new HashSet<>(previous);
                released.removeAll(step.held());
                double x = playing.player().x(), y = playing.player().y();
                ctx.states().update(Controls.of(step.held(), pressed, released));
                previous = step.held();
                if (playing.deaths() > 0) {
                    fail("died at tick " + tick + " near x=" + x + ", y=" + y + " (right edge " + (x + 14) + ")");
                }
                if (ctx.states().top() instanceof LevelCompleteState) return;
            }
        }
        fail("route ended at tick " + tick + " without reaching the exit; player at x=" + playing.player().x()
                + ", y=" + playing.player().y());
    }
}
