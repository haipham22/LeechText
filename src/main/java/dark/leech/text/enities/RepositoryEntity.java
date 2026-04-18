package dark.leech.text.enities;

import com.google.gson.annotations.SerializedName;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = {"link"})
public class RepositoryEntity {
    private String uuid;
    private String link;
    private String author;
    private String description;

    @SerializedName("enabled")
    private boolean isEnabled;
}
