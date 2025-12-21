package trademind.tm_server.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.asset.Asset;

// For dynamic search queries

@Repository
public interface AssetSearchRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {
}
