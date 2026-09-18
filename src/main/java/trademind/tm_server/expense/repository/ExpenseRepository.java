package trademind.tm_server.expense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import trademind.tm_server.expense.Expense;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByDeletedAtIsNull();

    Optional<Expense> findByIdAndDeletedAtIsNull(Long id);
}
