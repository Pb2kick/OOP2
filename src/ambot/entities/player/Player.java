package ambot.entities.player;

import static ambot.entities.player.PlayerTuning.COYOTE_TICKS;
import static ambot.entities.player.PlayerTuning.DROP_THROUGH_TICKS;
import static ambot.entities.player.PlayerTuning.HITBOX_H;
import static ambot.entities.player.PlayerTuning.HITBOX_W;
import static ambot.entities.player.PlayerTuning.JUMP_BUFFER_TICKS;
import static ambot.entities.player.PlayerTuning.JUMP_CUT_VELOCITY;
import static ambot.entities.player.PlayerTuning.JUMP_VELOCITY;
import static ambot.entities.player.PlayerTuning.MIN_JUMP_TICKS;
import static ambot.entities.player.PlayerTuning.RUN_SPEED;
import static ambot.entities.player.PlayerTuning.SPRITE_CENTER_X;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ambot.entities.Creature;
import ambot.entities.Facing;
import ambot.gfx.AnimationClip;
import ambot.gfx.SpriteSheet;
import ambot.input.Action;
import ambot.input.Controls;
import ambot.level.TileCollider;
import ambot.level.TileMap;

/** The adventurer: runs, jumps (with coyote time and jump buffering), crouches, drops through one-way platforms and attacks. */
public class Player extends Creature {
    private final SpriteSheet sheet;
    private PlayerState state = PlayerState.IDLE;
    private int coyote, buffer;
    /** Ticks since take-off, or -1 when not in a jump. */
    private int jumpTicks = -1;
    /** Ticks left during which one-way platforms are ignored. */
    private int dropTicks;

    public Player(double x, double y, SpriteSheet sheet) {
        super(x, y, HITBOX_W, HITBOX_H);
        this.sheet = sheet;
        animator.play(PlayerAnimations.IDLE);
    }

    public void update(Controls c, TileMap map) {
        // 0. down on a one-way platform drops through it (drop wins over crouch, spec section 7)
        if (c.wasPressed(Action.DOWN) && onGround && TileCollider.onOneWayOnly(hitbox(), map)) {
            dropTicks = DROP_THROUGH_TICKS;
        }

        // 1. grace timers
        coyote = onGround ? COYOTE_TICKS : Math.max(0, coyote - 1);
        buffer = c.wasPressed(Action.JUMP) ? JUMP_BUFFER_TICKS : Math.max(0, buffer - 1);

        // 2. attack starts
        if (c.wasPressed(Action.ATTACK) && state != PlayerState.ATTACK) {
            state = PlayerState.ATTACK;
            animator.play(PlayerAnimations.ATTACK);
        }
        boolean attacking = state == PlayerState.ATTACK;

        // 3. horizontal movement and facing
        int dir = (c.isHeld(Action.RIGHT) ? 1 : 0) - (c.isHeld(Action.LEFT) ? 1 : 0);
        boolean crouching = c.isHeld(Action.DOWN) && onGround && !attacking && dropTicks == 0;
        vx = (crouching || (attacking && onGround)) ? 0 : dir * RUN_SPEED;
        if (dir != 0 && !attacking) {
            facing = dir > 0 ? Facing.RIGHT : Facing.LEFT;
        }

        // 4. jump (buffered press + coyote window)
        if (buffer > 0 && coyote > 0 && !crouching && !attacking) {
            vy = JUMP_VELOCITY;
            buffer = 0;
            coyote = 0;
            jumpTicks = 0;
        }

        // 5. releasing jump early cuts the jump short (after a minimum rise)
        boolean pastMinimumRise = jumpTicks < 0 || jumpTicks >= MIN_JUMP_TICKS;
        if (!c.isHeld(Action.JUMP) && vy < JUMP_CUT_VELOCITY && pastMinimumRise) {
            vy = JUMP_CUT_VELOCITY;
        }
        if (jumpTicks >= 0) jumpTicks++;

        // 6. physics
        applyGravityAndMove(map, dropTicks > 0);
        if (dropTicks > 0) dropTicks--;
        if (onGround) jumpTicks = -1;

        // 7. state and animation
        if (state != PlayerState.ATTACK || animator.isFinished()) {
            if (!onGround) {
                state = vy < 0 ? PlayerState.JUMP : PlayerState.FALL;
            } else if (crouching) {
                state = PlayerState.CROUCH;
            } else if (vx != 0) {
                state = PlayerState.RUN;
            } else {
                state = PlayerState.IDLE;
            }
        }
        animator.play(PlayerAnimations.forState(state));
        animator.tick();
    }

    /** Puts the player back at a position, standing still. */
    public void respawn(double newX, double newY) {
        x = newX;
        y = newY;
        vx = 0;
        vy = 0;
        coyote = 0;
        buffer = 0;
        jumpTicks = -1;
        dropTicks = 0;
        onGround = false;
        state = PlayerState.IDLE;
        animator.play(PlayerAnimations.IDLE);
    }

    public PlayerState state() {
        return state;
    }

    @Override
    public void render(Graphics2D g) {
        AnimationClip clip = animator.clip();
        BufferedImage frame = sheet.frame(animator.column(), clip.row());
        int cellW = PlayerAnimations.CELL_W;
        int anchor = facing == Facing.RIGHT ? SPRITE_CENTER_X : cellW - SPRITE_CENTER_X;
        int drawX = (int) Math.round(x + width / 2 - anchor);
        int drawY = (int) Math.round(y + height - clip.footY());
        if (facing == Facing.RIGHT) {
            g.drawImage(frame, drawX, drawY, cellW, PlayerAnimations.CELL_H, null);
        } else {
            g.drawImage(frame, drawX + cellW, drawY, -cellW, PlayerAnimations.CELL_H, null);
        }
    }
}
