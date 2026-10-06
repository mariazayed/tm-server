package trademind.tm_server.invest.reminder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trademind.tm_server.constants.AppConstants;

import java.time.LocalDateTime;

/** Proof that a reminder went out for a month; unique per (month, kind). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_REMINDER_LOG)
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 7)
    private String month;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private ReminderKind kind;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    public ReminderLog(String month, ReminderKind kind, LocalDateTime sentAt) {
        this.month = month;
        this.kind = kind;
        this.sentAt = sentAt;
    }
}
