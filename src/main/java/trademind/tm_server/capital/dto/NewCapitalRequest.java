package trademind.tm_server.capital.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.CapitalReasonEnum;
import trademind.tm_server.enums.TradeDirectionEnum;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCapitalRequest {
    @NotNull(message = "Required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateTime;

    @NotNull(message = "Required")
    private double amount;

    @NotNull(message = "Required")
    private CapitalReasonEnum reason;

    private String notes;
}
