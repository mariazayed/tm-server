package trademind.tm_server.expenseReason;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;

// Lookup table backing the "expense reason" dropdown -- unlike the app's
// fixed enums (e.g. CapitalReasonEnum), these options are user-managed at
// runtime (add/rename/delete from the Expense Reasons screen or inline from
// the Add Expense sheet), so they live in their own table rather than a Java
// enum. Mirrors Asset.java's shape/CRUD pattern exactly.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_EXPENSE_REASONS)
public class ExpenseReason extends BaseEntity {

    @Column(nullable = false)
    private String name;
}
