package trademind.tm_server.strategy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.StrategyTagEnum;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrategyResponse {
    private Long id;
    private String name;
    private String link;
    private List<StrategyTagEnum> tags;
}
