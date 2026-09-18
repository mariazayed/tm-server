package trademind.tm_server.expense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import trademind.tm_server.expense.Expense;

public interface ExpenseSearchRepository
        extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {
}
