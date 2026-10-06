package trademind.tm_server.invest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;

/** One line of the written investing strategy shown on the dashboard. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_RULES)
public class Rule extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String text;

    @Column(nullable = false)
    private int sortOrder;
}
