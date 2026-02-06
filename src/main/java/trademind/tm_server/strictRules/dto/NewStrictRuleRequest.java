package trademind.tm_server.strictRules.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewStrictRuleRequest {
    @NotNull(message = "Required")
    private String text;
}
