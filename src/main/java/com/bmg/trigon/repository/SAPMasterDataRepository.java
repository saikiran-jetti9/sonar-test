package com.bmg.trigon.repository;

import com.bmg.trigon.model.SAPMasterData;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SAPMasterDataRepository extends JpaRepository<SAPMasterData, Long> {

  @Query(
      "SELECT s FROM SAPMasterData s WHERE s.accountNumberOfVendorOrCreditor = :accountNumberOfVendor")
  List<SAPMasterData> findAllByAccountNumberOfVendorOrCreditor(Long accountNumberOfVendor);
}
