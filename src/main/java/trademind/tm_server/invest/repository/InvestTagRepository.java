package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.InvestTag;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestTagRepository extends JpaRepository<InvestTag, Long> {
    List<InvestTag> findByDeletedAtIsNullOrderByNameAsc();

    Optional<InvestTag> findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    interface TagUsage {
        String getName();

        long getCount();
    }

    /** Every live tag with how many live journal entries use it (0 for unused). */
    @Query(value = """
            select t.name as name, count(e.id) as count
            from invest_tags t
            left join invest_journal_entry_tags jt on jt.tag_id = t.id
            left join invest_journal_entries e on e.id = jt.entry_id and e.deleted_at is null
            where t.deleted_at is null
            group by t.id, t.name
            order by lower(t.name)
            """, nativeQuery = true)
    List<TagUsage> findUsage();
}
