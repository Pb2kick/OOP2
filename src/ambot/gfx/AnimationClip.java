package ambot.gfx;

/**
 * A run of frames on one sheet row.
 *
 * @param footY cell-local y just below the lowest visible pixel across the clip's frames;
 *              the renderer puts this line on the ground so feet never float or sink
 */
public record AnimationClip(String name, int row, int startCol, int frameCount, int ticksPerFrame,
        boolean loop, int footY) {
}
