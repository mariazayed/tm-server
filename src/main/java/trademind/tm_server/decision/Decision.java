package trademind.tm_server.decision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.DecisionCategoryEnum;
import trademind.tm_server.enums.DecisionStatusEnum;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single entry in the personal "Decisions Log" -- a running record of
 * strategic decisions (Trading, Investing, Personal, System, Learning, Others)
 * with enough structure to see how later decisions revise or reverse earlier
 * ones over time. `notes` (inherited from BaseEntity) is used for a
 * follow-up outcome/reflection, separate from `reasoning` which captures the
 * "why" at the time the decision was made. `description` and `reasoning`
 * hold markdown text written by the Flutter app's MdEditorField -- stored as
 * plain `text` columns with no length limit or validation, markdown syntax
 * and all, and rendered back to markdown on read.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_DECISIONS)
public class Decision extends BaseEntity {

    // The sequence number from the original log (1, 2, 3...) -- independent
    // of the DB id, so it stays stable and human-readable even if rows are
    // ever re-imported or re-ordered. Server-assigned on create (see
    // DecisionService.create) -- never taken from the request.
    private Integer decisionNumber;

    @Column(nullable = false)
    private LocalDateTime decisionDate;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DecisionCategoryEnum category;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DecisionStatusEnum status;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    // The "why" -- motivations/reasons behind the decision, kept separate
    // from `description` so the two can be shown/filtered independently.
    @Column(columnDefinition = "text")
    private String reasoning;

    // decisionNumbers of earlier decisions this one reconsiders, revises,
    // or reverses. Empty if this decision doesn't build on prior ones.
    @Column(columnDefinition = "integer[]")
    private List<Integer> relatedDecisionNumbers = new ArrayList<>();
}
