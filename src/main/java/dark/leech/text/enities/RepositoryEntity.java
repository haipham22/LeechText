package dark.leech.text.enities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RepositoryEntity {
    private String link;
    private String author;
    private String description;
    private boolean selected;
}
