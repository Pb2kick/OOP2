package ambot.states;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ambot.core.GameConfig;
import ambot.input.Action;
import ambot.input.Controls;

class CreditsTest {

    /** The Author cell of every data row of the first table in docs/ASSETS.md. */
    static List<String> assetAuthors() throws IOException {
        List<String> authors = new ArrayList<>();
        boolean inTable = false;
        for (String line : Files.readAllLines(Path.of("docs/ASSETS.md"), StandardCharsets.UTF_8)) {
            if (line.startsWith("| File |")) {
                inTable = true;
                continue;
            }
            if (!inTable) continue;
            if (!line.startsWith("|")) break;
            if (line.startsWith("|---")) continue;
            String[] cells = line.split("\\|");
            authors.add(cells[3].trim());
        }
        return authors;
    }

    @Test
    void creditsListEveryAssetAuthor() throws IOException {
        List<String> authors = assetAuthors();
        assertFalse(authors.isEmpty(), "no asset rows found");
        String credits = String.join("\n", CreditsState.lines());
        for (String author : authors) {
            if (author.equals("own work (restoration)")) continue;
            assertTrue(credits.contains(author), "credits.txt is missing the author '" + author + "'");
        }
    }

    @Test
    void creditsMentionVersion() {
        assertTrue(CreditsState.lines().stream().anyMatch(l -> l.contains(GameConfig.VERSION)));
    }

    @Test
    void backPops() {
        FakeContext ctx = new FakeContext();
        ctx.states().replaceAll(new MainMenuState(ctx));
        ctx.states().update(Controls.NONE);
        ctx.states().push(new CreditsState(ctx));
        ctx.states().update(Controls.NONE);
        for (int i = 0; i < 100; i++) ctx.states().update(Controls.NONE); // scrolling never throws
        ctx.states().update(Controls.of(Set.of(Action.BACK), Set.of(Action.BACK), Set.of()));
        ctx.states().update(Controls.NONE);
        assertInstanceOf(MainMenuState.class, ctx.states().top());
    }
}
