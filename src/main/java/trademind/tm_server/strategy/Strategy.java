package trademind.tm_server.strategy;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.StrategyTagEnum;

import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_STRATEGIES)
public class Strategy extends BaseEntity {
    private Long id;
    private String name;
    private String link;
    private List<StrategyTagEnum> tags;
}