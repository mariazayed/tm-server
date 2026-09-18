package trademind.tm_server.expenseReason.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import trademind.tm_server.expenseReason.ExpenseReason;

public interface ExpenseReasonSearchRepository
        extends JpaRepository<ExpenseReason, Long>, JpaSpecificationExecutor<ExpenseReason> {
}
