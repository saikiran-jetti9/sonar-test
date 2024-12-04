package com.bmg.trigon.repository;

import com.bmg.trigon.common.model.WorkbenchBatch;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkbenchBatchRepository extends JpaRepository<WorkbenchBatch, UUID> {
  List<WorkbenchBatch> findByWorkbenchCriteriaId(UUID id);
}
