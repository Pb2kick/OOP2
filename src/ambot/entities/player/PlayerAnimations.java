package ambot.entities.player;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ambot.gfx.AnimationClip;

/**
 * The player's clips on the team's reworked sheet (one animation per row).
 * Rows, columns and foot lines were measured from the art on 2026-11-17.
 */
public final class PlayerAnimations {
    private PlayerAnimations() {
    }

    public static final String SHEET_PATH = "/Betteradventurer-Sheet.png";
    public static final int CELL_W = 50, CELL_H = 37;

    //                                                   name      row col frames ticks loop  footY
    public static final AnimationClip IDLE   = new AnimationClip("IDLE",   0, 0, 4, 10, true,  36);
    public static final AnimationClip RUN    = new AnimationClip("RUN",    1, 0, 6, 6,  true,  34);
    public static final AnimationClip JUMP   = new AnimationClip("JUMP",   2, 2, 2, 6,  false, 34);
    public static final AnimationClip FALL   = new AnimationClip("FALL",   2, 8, 2, 8,  true,  30);
    public static final AnimationClip CROUCH = new AnimationClip("CROUCH", 9, 0, 4, 10, true,  32);
    public static final AnimationClip ATTACK = new AnimationClip("ATTACK", 6, 0, 7, 5,  false, 36);

    public static final List<AnimationClip> ALL = List.of(IDLE, RUN, JUMP, FALL, CROUCH, ATTACK);

    private static final Map<PlayerState, AnimationClip> BY_STATE = new EnumMap<>(Map.of(
            PlayerState.IDLE, IDLE,
            PlayerState.RUN, RUN,
            PlayerState.JUMP, JUMP,
            PlayerState.FALL, FALL,
            PlayerState.CROUCH, CROUCH,
            PlayerState.ATTACK, ATTACK));

    public static AnimationClip forState(PlayerState state) {
        return BY_STATE.get(state);
    }
}
