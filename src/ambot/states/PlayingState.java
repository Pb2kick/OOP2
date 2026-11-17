package ambot.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Arrays;

import ambot.core.GameConfig;
import ambot.entities.player.Player;
import ambot.entities.player.PlayerTuning;
import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.AsciiLevel;
import ambot.level.TileMap;

/** The game itself. In M1 this is a single hard-coded test room. */
public final class PlayingState implements GameState {
    private static final Color SKY = new Color(0x29366F);
    private static final Color TILE = new Color(0x5F574F);
    private static final Color TILE_TOP = new Color(0x83769C);
    private static final Color HINT = new Color(0xC2C3C7);
    private static final int T = GameConfig.TILE;

    /** 40x22 test room: floor with a 4-tile pit, side walls, three stepped ledges. */
    public static final String[] TEST_ROOM = buildTestRoom();

    private final GameContext ctx;
    private final AsciiLevel level;
    private final Player player;

    public PlayingState(GameContext ctx) {
        this(ctx, AsciiLevel.parse(TEST_ROOM));
    }

    public PlayingState(GameContext ctx, AsciiLevel level) {
        this.ctx = ctx;
        this.level = level;
        this.player = new Player(spawnX(), spawnY(), ctx.playerSheet());
    }

    // The hitbox is taller than a tile, so stand it on the bottom-centre of the spawn tile.
    private double spawnX() {
        return level.spawnX() + (T - PlayerTuning.HITBOX_W) / 2.0;
    }

    private double spawnY() {
        return level.spawnY() + T - PlayerTuning.HITBOX_H;
    }

    @Override
    public void update(Controls c) {
        if (c.wasPressed(Action.PAUSE)) {
            ctx.states().push(new PausedState(ctx));
            return;
        }
        player.update(c, level.map());
        if (player.y() > level.map().heightPx() + 64) {
            player.respawn(spawnX(), spawnY());
        }
    }

    @Override
    public void onFocusLost() {
        ctx.states().push(new PausedState(ctx));
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(SKY);
        g.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
        TileMap map = level.map();
        for (int ty = 0; ty < map.heightTiles(); ty++) {
            for (int tx = 0; tx < map.widthTiles(); tx++) {
                if (map.tileAt(tx, ty) == TileMap.SOLID) {
                    g.setColor(TILE);
                    g.fillRect(tx * T, ty * T, T, T);
                    if (!map.isSolid(tx, ty - 1)) {
                        g.setColor(TILE_TOP);
                        g.fillRect(tx * T, ty * T, T, 2);
                    }
                }
            }
        }
        player.render(g);
        g.setColor(HINT);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        g.drawString("Esc: pause", 22, 14);
    }

    public Player player() {
        return player;
    }

    public AsciiLevel level() {
        return level;
    }

    private static String[] buildTestRoom() {
        int w = 40, h = 22;
        char[][] r = new char[h][w];
        for (char[] row : r) Arrays.fill(row, '.');
        for (int y = 0; y < h; y++) {
            r[y][0] = '#';
            r[y][w - 1] = '#';
        }
        for (int x = 0; x < w; x++) r[h - 1][x] = (x >= 24 && x <= 27) ? '.' : '#'; // floor with a pit
        fill(r, 19, 6, 11);  // ledge 2 tiles above the floor
        fill(r, 17, 13, 18); // 4 tiles up
        fill(r, 15, 20, 25); // 6 tiles up
        r[h - 2][2] = 'P';
        String[] rows = new String[h];
        for (int y = 0; y < h; y++) rows[y] = new String(r[y]);
        return rows;
    }

    private static void fill(char[][] r, int row, int fromCol, int toCol) {
        for (int x = fromCol; x <= toCol; x++) r[row][x] = '#';
    }
}
