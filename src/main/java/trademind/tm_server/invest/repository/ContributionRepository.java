package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.Contribution;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {
    List<Contribution> findByDeletedAtIsNullOrderByContributedOnDesc();

    Optional<Contribution> findByIdAndDeletedAtIsNull(Long id);

    List<Contribution> findByDeletedAtIsNullAndContributedOnBetweenOrderByContributedOnAsc(
            LocalDate from, LocalDate to);
}
