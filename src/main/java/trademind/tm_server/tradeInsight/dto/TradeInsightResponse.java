package trademind.tm_server.tradeInsight.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeInsightResponse {
    private Long id;
    private Long tradeId;
    private String model;
    private String promptVersion;
    private String summary;
    private String suggestedImprovement;
    private String detectedMistakeTags;
    private Double confidence;
    private LocalDateTime generatedAt;
    private String notes;
}
