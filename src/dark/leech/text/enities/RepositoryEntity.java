package dark.leech.text.enities;

import com.google.gson.annotations.SerializedName;
import dark.leech.text.util.SyntaxUtils;

import java.util.Objects;

public class RepositoryEntity {

    @SerializedName("link")
    private String link;

    @SerializedName("author")
    private String author;

    @SerializedName("description")
    private String description;

    @SerializedName("checked")
    private Boolean checked;

    public void apply(RepositoryEntity entity) {
        this.link = entity.link;
        this.author = entity.author;
        this.description = entity.description;
        this.checked = true;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(Boolean check) {
        this.checked = check;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepositoryEntity)) return false;
        RepositoryEntity that = (RepositoryEntity) o;
        return Objects.equals(link, that.link) && Objects.equals(author, that.author) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(link, author, description);
    }
}
