package trademind.tm_server.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.expenseReason.ExpenseReason;

import java.time.LocalDateTime;

// A personal expense entry: date + amount (USD) + the reason it was spent,
// picked from the user-managed ExpenseReason lookup table. Deliberately kept
// separate from Capital (deposits/withdraws/trade P&L) -- this is a
// standalone log and doesn't touch the capital balance or dashboard.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_EXPENSES)
public class Expense extends BaseEntity {

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private double amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_reason_id", nullable = false)
    private ExpenseReason expenseReason;
}
