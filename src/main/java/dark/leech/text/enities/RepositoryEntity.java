package dark.leech.text.enities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RepositoryEntity {
    private String link;
    private String author;
    private String description;
}
