package trademind.tm_server.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.asset.Asset;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByDeletedAtIsNull();

    Optional<Asset> findByIdAndDeletedAtIsNull(Long id);
}