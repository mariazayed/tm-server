package trademind.tm_server.invest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_TAGS)
public class InvestTag extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String name;

    public InvestTag(String name) {
        this.name = name;
    }
}
