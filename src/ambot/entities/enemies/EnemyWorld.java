package ambot.entities.enemies;

import java.util.function.Consumer;

import ambot.entities.Projectile;
import ambot.level.Rect;
import ambot.level.TileMap;

/** What an enemy may look at and do each tick: the player's body, the tiles, and firing shots. */
public record EnemyWorld(Rect player, TileMap map, Consumer<Projectile> fire) {
}
