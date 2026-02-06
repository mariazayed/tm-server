package trademind.tm_server.strictRules.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrictRuleResponse {
    private Long id;
    private String text;
}
