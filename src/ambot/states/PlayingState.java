package ambot.states;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import ambot.core.GameConfig;
import ambot.entities.Actors;
import ambot.entities.Facing;
import ambot.entities.player.Player;
import ambot.entities.player.PlayerTuning;
import ambot.gfx.ParallaxBackground;
import ambot.gfx.PlaceholderBackgrounds;
import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.Camera;
import ambot.level.Level;
import ambot.level.LevelCatalog;
import ambot.level.LevelRenderer;
import ambot.level.Rect;
import ambot.level.Spot;
import ambot.ui.Hud;

/**
 * Playing one level of a catalog: the player, enemies and pickups, a camera, checkpoints,
 * hazards, pits and the exit. Hazards cost a heart; pits and running out of hearts end in
 * Game Over, which can retry from the last checkpoint (spec section 5.4).
 */
public final class PlayingState implements GameState {
    private static final Color HINT = new Color(0xC2C3C7);
    private static final Font HINT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 10);
    private static final String HINT_TEXT = "Esc: pause";
    /** How far below the map the player may fall before it counts as a pit. */
    private static final int FALL_MARGIN = 64;

    private final GameContext ctx;
    private final LevelCatalog catalog;
    private final int index;
    private final Level level;
    private final Player player;
    private final Camera camera = new Camera(GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);
    private final LevelRenderer renderer;
    private final ParallaxBackground background;
    private Actors actors;
    private Spot respawn;
    private Rect activeCheckpoint;
    private int deaths;
    private boolean completed, gameOverOpen;

    /** New Game: the first placeholder level. */
    public PlayingState(GameContext ctx) {
        this(ctx, LevelCatalog.placeholder(), 0);
    }

    public PlayingState(GameContext ctx, LevelCatalog catalog, int index) {
        this.ctx = ctx;
        this.catalog = catalog;
        this.index = index;
        this.level = catalog.load(index);
        this.respawn = level.spawn();
        this.player = new Player(bodyX(respawn), bodyY(respawn), ctx.playerSheet());
        this.renderer = new LevelRenderer(level);
        this.background = PlaceholderBackgrounds.forWorld(level.world());
        this.actors = new Actors(level);
        camera.snapTo(player.hitbox(), level.widthPx(), level.heightPx());
    }

    // The hitbox stands with its bottom-centre on the spot.
    private static double bodyX(Spot s) {
        return s.x() - PlayerTuning.HITBOX_W / 2.0;
    }

    private static double bodyY(Spot s) {
        return s.y() - PlayerTuning.HITBOX_H;
    }

    @Override
    public void update(Controls c) {
        if (c.wasPressed(Action.PAUSE)) {
            ctx.states().push(new PausedState(ctx));
            return;
        }
        player.update(c, level.collision());
        actors.update(player, level.collision());
        Rect body = player.hitbox();
        if (!player.isDead()) {
            for (Rect cp : level.checkpoints()) {
                if (body.intersects(cp) && !cp.equals(activeCheckpoint)) {
                    activeCheckpoint = cp;
                    respawn = new Spot(cp.x() + cp.w() / 2, cp.bottom());
                }
            }
            if (level.collision().touchesHazard(body)) {
                double centre = body.x() + body.w() / 2;
                player.damage(1, centre + (player.facing() == Facing.RIGHT ? 8 : -8)); // knocked backwards
            }
            if (!player.isDead() && player.y() > level.heightPx() + FALL_MARGIN) player.kill();
        }
        if (player.deathFinished() && !gameOverOpen) {
            gameOverOpen = true;
            deaths++;
            ctx.states().push(new GameOverState(ctx, this));
        } else if (!player.isDead() && !completed && body.intersects(level.exit())) {
            completed = true;
            ctx.states().push(new LevelCompleteState(ctx, catalog, index, level.title()));
        }
        camera.follow(player.hitbox(), level.widthPx(), level.heightPx());
    }

    /** Back to the last checkpoint with full hearts; enemies and pickups start over. */
    public void retry() {
        player.respawn(bodyX(respawn), bodyY(respawn));
        player.restoreHealth();
        actors = new Actors(level);
        camera.snapTo(player.hitbox(), level.widthPx(), level.heightPx());
        gameOverOpen = false;
    }

    @Override
    public void onFocusLost() {
        ctx.states().push(new PausedState(ctx));
    }

    @Override
    public void render(Graphics2D g) {
        int cx = camera.x(), cy = camera.y();
        background.render(g, cx, Math.max(0, level.heightPx() - GameConfig.LOGICAL_HEIGHT - cy));
        g.translate(-cx, -cy);
        Rect view = camera.view();
        renderer.drawBehind(g, view);
        renderer.drawMarkers(g, activeCheckpoint);
        actors.render(g, view);
        player.render(g);
        renderer.drawFront(g, view);
        g.translate(cx, cy);
        Hud.render(g, player);
        g.setColor(HINT);
        g.setFont(HINT_FONT);
        int w = g.getFontMetrics().stringWidth(HINT_TEXT);
        g.drawString(HINT_TEXT, GameConfig.LOGICAL_WIDTH - w - 8, 14);
    }

    public Player player() {
        return player;
    }

    public Level level() {
        return level;
    }

    public Camera camera() {
        return camera;
    }

    public Actors actors() {
        return actors;
    }

    public Spot respawnPoint() {
        return respawn;
    }

    /** How many times this level ended in Game Over. */
    public int deaths() {
        return deaths;
    }
}
