package trademind.tm_server.invest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import trademind.tm_server.enums.JournalEntryType;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_JOURNAL_ENTRIES)
public class JournalEntry extends BaseEntity {

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private JournalEntryType entryType;

    /** 1 (very worried) to 5 (very calm); optional. */
    private Short mood;

    /** Markdown. */
    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @ManyToOne
    @JoinColumn(name = "contribution_id")
    private Contribution contribution;

    @ManyToMany
    @JoinTable(name = AppConstants.DB_INVEST_JOURNAL_ENTRY_TAGS,
            joinColumns = @JoinColumn(name = "entry_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<InvestTag> tags = new LinkedHashSet<>();
}
