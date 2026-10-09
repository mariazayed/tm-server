package trademind.tm_server.invest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;

/**
 * One field of the plan that changed in one save. {@code createdAt} (from the base class) is when. See
 * V29: the settings row itself is overwritten, so this is the only record that a value used to be different.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_SETTINGS_CHANGES)
public class SettingsChange extends BaseEntity {

    public static final String MONTHLY_AMOUNT = "MONTHLY_AMOUNT";
    public static final String TICKER = "TICKER";
    public static final String BUY_DAY = "BUY_DAY";

    @Column(name = "field_name", nullable = false, length = 32)
    private String fieldName;

    @Column(length = 64)
    private String oldValue;

    @Column(nullable = false, length = 64)
    private String newValue;

    public SettingsChange(String fieldName, String oldValue, String newValue) {
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
