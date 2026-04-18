package dark.leech.text.plugin.vbook.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a vBook plugin repository source. Matches the metadata section of vBook's plugin.json
 * registry.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VBookRepositoryEntity {

    /** URL to the plugin.json registry */
    private String link;

    /** Repository author (e.g., "vBook") */
    private String author;

    /** Repository description */
    private String description;

    /** Whether this repository is enabled for auto-download */
    @Builder.Default private boolean enabled = true;
}
