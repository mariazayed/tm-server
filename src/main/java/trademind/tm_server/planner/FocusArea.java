package trademind.tm_server.planner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;

// A user-defined bucket of work (Backtesting, Journaling, Learning...).
// maxPercent caps the share of a week's/month's capacity that this area's
// planned hours should take -- a soft limit the UI warns about.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_FOCUS_AREAS)
public class FocusArea extends BaseEntity {

    @Column(nullable = false)
    private String name;

    // Hex string, e.g. "#6366F1".
    @Column(nullable = false)
    private String color;

    // Key of a Material icon from the dashboard's fixed icon set.
    private String icon;

    @Column(nullable = false)
    private Double maxPercent;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private boolean archived = false;
}
