package dark.leech.text.models;

/**
 * Represents a chapter in a book or text document.
 *
 * <p>The Chapter class encapsulates all the information about a single chapter, including its URL,
 * name, completion status, and various metadata. This class is used throughout the application to
 * track download progress, manage chapter information, and organize content structure.
 *
 * <p>Chapters can be part of a larger book structure and may contain multiple parts or volumes. The
 * class implements Cloneable to allow for easy duplication of chapter objects during processing.
 *
 * @author LeechText Development Team
 * @version 1.0
 * @since 1.0
 * @see BookEntity
 * @see ChapterEntity
 */
public class Chapter implements Cloneable {

    /** The URL where the chapter content can be downloaded from */
    private String url;

    /** The name of the part/volume this chapter belongs to */
    private String partName;

    /** The name/title of the chapter */
    private String chapName;

    /** Indicates whether the chapter has been completely downloaded */
    private boolean completed;

    /** Indicates whether an error occurred during download */
    private boolean error;

    /** Indicates whether the chapter content is empty */
    private boolean empty;

    /** Indicates whether this chapter contains images */
    private boolean imageChapter;

    /** Unique identifier for the chapter */
    private String id;

    /** Indicates whether this chapter requires purchase */
    private boolean purchase;

    /** Default constructor that creates a chapter with an empty URL. */
    public Chapter() {
        this("");
    }

    /**
     * Creates a chapter with the specified URL.
     *
     * @param url The URL where the chapter content can be found
     */
    public Chapter(String url) {
        this(url, "");
    }

    /**
     * Creates a chapter with the specified URL and name.
     *
     * @param url The URL where the chapter content can be found
     * @param name The name of the chapter
     */
    public Chapter(String url, String name) {
        this(url, -1, "", name);
    }

    /**
     * Creates a chapter with the specified URL, ID, part name, and chapter name.
     *
     * @param url The URL where the chapter content can be found
     * @param id The numeric ID of the chapter
     * @param partName The name of the part/volume this chapter belongs to
     * @param chapName The name of the chapter
     */
    public Chapter(String url, int id, String partName, String chapName) {
        this(url, id, partName, chapName, false, false);
    }

    /**
     * Creates a chapter with all the specified parameters.
     *
     * @param url The URL where the chapter content can be found
     * @param id The numeric ID of the chapter
     * @param partName The name of the part/volume this chapter belongs to
     * @param chapName The name of the chapter
     * @param completed Whether the chapter has been completely downloaded
     * @param error Whether an error occurred during download
     */
    public Chapter(
            String url,
            int id,
            String partName,
            String chapName,
            boolean completed,
            boolean error) {
        this.url = url;
        this.id = "C" + id;
        this.partName = partName;
        this.chapName = chapName;
        this.error = error;
        this.completed = completed;
    }

    /**
     * Creates a chapter with the specified URL, ID, and chapter name.
     *
     * @param url The URL where the chapter content can be found
     * @param id The numeric ID of the chapter
     * @param chapName The name of the chapter
     */
    public Chapter(String url, int id, String chapName) {
        this(url, id, null, chapName);
    }

    /**
     * Creates a chapter with the specified URL, ID, part name, and chapter name.
     *
     * @param url The URL where the chapter content can be found
     * @param id The string ID of the chapter
     * @param partName The name of the part/volume this chapter belongs to
     * @param chapName The name of the chapter
     */
    public Chapter(String url, String id, String partName, String chapName) {
        this.url = url;
        this.partName = partName;
        this.chapName = chapName;
        this.id = id;
    }

    /**
     * Gets the unique identifier for this chapter.
     *
     * @return The chapter ID as a string
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this chapter.
     *
     * @param id The new chapter ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the unique identifier for this chapter using a numeric value. The ID will be prefixed
     * with "C" to create a string identifier.
     *
     * @param id The numeric ID for the chapter
     */
    public void setId(int id) {
        this.id = "C" + id;
    }

    /**
     * Gets the URL where the chapter content can be downloaded from.
     *
     * @return The chapter URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL where the chapter content can be downloaded from.
     *
     * @param url The new chapter URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the name of the chapter.
     *
     * @return The chapter name, or empty string if null
     */
    public String getChapName() {
        if (chapName == null) chapName = "";
        return chapName;
    }

    /**
     * Sets the name of the chapter.
     *
     * @param chapName The new chapter name
     */
    public void setChapName(String chapName) {
        this.chapName = chapName;
    }

    /**
     * Gets the name of the part/volume this chapter belongs to.
     *
     * @return The part name, or empty string if null
     */
    public String getPartName() {
        if (partName == null) partName = "";
        return partName;
    }

    /**
     * Sets the name of the part/volume this chapter belongs to.
     *
     * @param partName The new part name
     */
    public void setPartName(String partName) {
        this.partName = partName;
    }

    /**
     * Checks if an error occurred during the download of this chapter.
     *
     * @return true if an error occurred, false otherwise
     */
    public boolean isError() {
        return error;
    }

    /**
     * Sets the error status for this chapter.
     *
     * @param error true if an error occurred, false otherwise
     */
    public void setError(boolean error) {
        this.error = error;
    }

    /**
     * Checks if the chapter has been completely downloaded.
     *
     * @return true if completed, false otherwise
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Sets the completion status for this chapter.
     *
     * @param completed true if completed, false otherwise
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * Checks if this chapter contains images.
     *
     * @return true if it contains images, false otherwise
     */
    public boolean isImageChapter() {
        return imageChapter;
    }

    /**
     * Sets whether this chapter contains images.
     *
     * @param imageChapter true if it contains images, false otherwise
     */
    public void setImageChapter(boolean imageChapter) {
        this.imageChapter = imageChapter;
    }

    /**
     * Checks if this chapter requires purchase.
     *
     * @return true if it requires purchase, false otherwise
     */
    public boolean isPurchase() {
        return purchase;
    }

    /**
     * Sets whether this chapter requires purchase.
     *
     * @param purchase true if it requires purchase, false otherwise
     */
    public void setPurchase(boolean purchase) {
        this.purchase = purchase;
    }

    /**
     * Checks if the chapter content is empty.
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Sets whether the chapter content is empty.
     *
     * @param empty true if empty, false otherwise
     */
    public void setEmpty(boolean empty) {
        this.empty = empty;
    }
}
