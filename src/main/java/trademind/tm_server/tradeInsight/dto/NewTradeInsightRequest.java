package trademind.tm_server.tradeInsight.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewTradeInsightRequest {
    @NotNull(message = "Required")
    private Long trade;

    @NotNull(message = "Required")
    private String model;

    @NotNull(message = "Required")
    private String promptVersion;

    private String summary;

    private String suggestedImprovement;

    private String detectedMistakeTags;

    private Double confidence;

    @NotNull(message = "Required")
    private LocalDateTime generatedAt;

    private String notes;
}
