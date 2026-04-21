package dark.leech.text.plugin.js.loader;

/**
 * Enum defining the types of vBook plugin loaders. Each loader type has its own isolated JavaScript
 * scope for better separation of concerns.
 */
public enum LoaderType {
    /** List loader - loads table of contents/chapter list */
    LIST("list", "tocGetter"),

    /** Detail loader - loads book metadata/information */
    DETAIL("detail", "detailGetter"),

    /** Text loader - loads chapter content */
    TEXT("text", "chapGetter"),

    /** Gen loader - loads paginated lists (novels, search results) */
    GEN("gen", "genGetter");

    private final String type;
    private final String scriptName;

    LoaderType(String type, String scriptName) {
        this.type = type;
        this.scriptName = scriptName;
    }

    /**
     * Get the string identifier for this loader type.
     *
     * @return Loader type identifier
     */
    public String getType() {
        return type;
    }

    /**
     * Get the default script name for this loader type.
     *
     * @return Script name (e.g., "tocGetter", "detailGetter")
     */
    public String getScriptName() {
        return scriptName;
    }
}
