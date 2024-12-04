package com.bmg.trigon.repository;

import com.bmg.trigon.model.SAPMasterDataSync;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SAPMasterDataSyncRepository extends JpaRepository<SAPMasterDataSync, Long> {

  Optional<SAPMasterDataSync> findTopByTerritoryOrderBySyncStartedAtDesc(String territory);
}
