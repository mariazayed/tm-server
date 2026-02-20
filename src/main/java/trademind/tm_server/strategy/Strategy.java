package trademind.tm_server.strategy;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.StrategyTagEnum;
import trademind.tm_server.trade.Trade;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_STRATEGIES)
public class Strategy extends BaseEntity {

    private String name;
    private String link;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<StrategyTagEnum> tags;


    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trade> trades;
}