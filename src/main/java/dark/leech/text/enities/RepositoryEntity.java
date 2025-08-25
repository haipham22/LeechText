package dark.leech.text.enities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = {"link"})
public class RepositoryEntity {
    private String link;
    private String author;
    private String description;
    private boolean selected;
}
