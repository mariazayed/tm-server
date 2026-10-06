package trademind.tm_server.invest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_LEARNING_NOTES)
public class LearningNote extends BaseEntity {

    @Column(nullable = false)
    private String title;

    /** Markdown. */
    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @ManyToOne
    @JoinColumn(name = "source_id")
    private LearningSource source;

    @ManyToMany
    @JoinTable(name = AppConstants.DB_INVEST_LEARNING_NOTE_TAGS,
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<InvestTag> tags = new LinkedHashSet<>();
}
