package ambot.level;

/** A level file that can't be loaded or isn't valid. The message starts with the file's resource path. */
public class LevelFormatException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LevelFormatException(String message) {
        super(message);
    }

    public LevelFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
