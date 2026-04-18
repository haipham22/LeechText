package dark.leech.text.plugin.security;

/** Exception thrown when cache capacity is exceeded. */
public class CacheCapacityException extends Exception {

    public CacheCapacityException(String message) {
        super(message);
    }
}
