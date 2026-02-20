package trademind.tm_server.strategy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.StrategyTagEnum;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewStrategyRequest {
    @NotNull(message = "Required")
    private String name;

    @NotNull(message = "Required")
    private String link;

    @NotNull(message = "Required")
    private List<StrategyTagEnum> tags;
}
