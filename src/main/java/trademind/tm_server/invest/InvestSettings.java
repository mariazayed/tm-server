package trademind.tm_server.invest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;

import java.time.LocalDate;

/** The one settings row; its id is pinned to 1 by the database. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_SETTINGS)
public class InvestSettings extends BaseEntity {

    public static final long SINGLETON_ID = 1L;

    @Column(nullable = false)
    private long monthlyAmountCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private short buyDay;

    @Column(nullable = false, length = 16)
    private String ticker;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(nullable = false)
    private boolean remindersEnabled;

    @Column(length = 254)
    private String reminderEmail;

    @Column(nullable = false)
    private short reminderFollowupDays = 3;
}
