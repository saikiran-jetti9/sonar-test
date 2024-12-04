package com.bmg.trigon.repository;

import com.bmg.trigon.common.enums.DirectFIStatus;
import com.bmg.trigon.common.enums.PostApprovalStatus;
import com.bmg.trigon.common.enums.PostingCategory;
import com.bmg.trigon.common.model.WorkbenchBatchData;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkbenchBatchDataRepository extends JpaRepository<WorkbenchBatchData, UUID> {

  Page<WorkbenchBatchData> findByPostApprovalStatusOrderByWorkbenchBatch(
      PostApprovalStatus postApprovalStatus, PageRequest pageRequest);

  /**
   * Retrieves distinct clearing accounting documents by post-approval status, used for ZP document
   * type API
   *
   * @param postApprovalStatus The post-approval status to filter the results by.
   * @param pageRequest Page request for pagination.
   * @return A page containing distinct clearing accounting documents.
   */
  @Query(
      "SELECT DISTINCT wbd.clearingAccountingDocument FROM WorkbenchBatchData wbd WHERE wbd.postApprovalStatus = :postApprovalStatus AND wbd.clearingAccountingDocument IS NOT NULL AND wbd.clearingAccountingDocument <> '' ORDER BY wbd.clearingAccountingDocument")
  Page<String> findDistinctClearingAccountingDocumentsByPostApprovalStatus(
      PostApprovalStatus postApprovalStatus, PageRequest pageRequest);

  /**
   * Retrieves workbench batch data by clearing accounting documents and post-approval status. This
   * query fetches workbench batch data based on a list of clearing accounting documents and
   * post-approval status. used for ZP document type API
   *
   * @param clearingAccountingDocuments List of clearing accounting documents.
   * @param postApprovalStatus The post-approval status to filter the results by.
   * @param batchPageRequest Page request for pagination.
   * @return A page containing workbench batch data.
   */
  Page<WorkbenchBatchData> findByClearingAccountingDocumentInAndPostApprovalStatus(
      List<String> clearingAccountingDocuments,
      PostApprovalStatus postApprovalStatus,
      PageRequest batchPageRequest);

  /**
   * Retrieves distinct URRI prefixes by post-approval status and clearing accounting document. This
   * query fetches distinct URRI prefixes associated with a specific post-approval status and where
   * a clearing accounting document is empty or null. Used for HG document type
   *
   * @param postApprovalStatus The post-approval status to filter the results by.
   * @param pageRequest Page request for pagination.
   * @return A page containing distinct URRI prefixes.
   */
  // Add one more filter w.isHGDocumentTypeCompletedForNonPayouts = false because it should not
  // track for non-payouts
  // once after HG is completed
  @Query(
      "SELECT DISTINCT SPLIT_PART(w.royaltyRunIdentifierUrri, ' ', 1) FROM WorkbenchBatchData w WHERE w.postApprovalStatus = :postApprovalStatus AND (w.clearingAccountingDocument = '' or w.clearingAccountingDocument is null) AND w.isHGDocumentTypeCompletedForNonPayouts = false")
  Page<String> findDistinctURRIPrefixesByPostApprovalStatusAndClearingAccountingDocument(
      PostApprovalStatus postApprovalStatus, PageRequest pageRequest);

  /**
   * Retrieves workbench batch data by post-approval status, clearing accounting document, and URRI
   * prefix. This query fetches workbench batch data based on post-approval status, clearing
   * accounting document, and URRI prefix.
   *
   * @param postApprovalStatus The post-approval status to filter the results by.
   * @param urriPrefix The URRI prefix to filter the results by.
   * @param pageRequest Page request for pagination.
   * @return A page containing workbench batch data.
   */
  // Add one more filter w.isHGDocumentTypeCompletedForNonPayouts = false because it should not
  // track for non-payouts
  // once after HG is completed
  @Query(
      "SELECT w FROM WorkbenchBatchData w WHERE w.postApprovalStatus = :postApprovalStatus AND (w.clearingAccountingDocument = '' or w.clearingAccountingDocument is null) AND w.isHGDocumentTypeCompletedForNonPayouts = false AND w.royaltyRunIdentifierUrri LIKE :urriPrefix order by w.fiPostedTime")
  Page<WorkbenchBatchData> findByPostApprovalStatusAndClearingAccountingDocumentAndURRIPrefix(
      PostApprovalStatus postApprovalStatus, String urriPrefix, PageRequest pageRequest);

  @Query(
      "SELECT w FROM WorkbenchBatchData w WHERE w.directFiStatus = :directFiStatus AND w.finalCategory IN :postingCategories")
  Page<WorkbenchBatchData> findByDirectFIStatusAndCategories(
      @Param("directFiStatus") DirectFIStatus directFiStatus,
      @Param("postingCategories") List<PostingCategory> postingCategories,
      PageRequest pageRequest);

  // Get all the URRI based on post approval status and clearing accounting document and URRI prefix
  // for HG document type API call
  @Query(
      "SELECT w.royaltyRunIdentifierUrri FROM WorkbenchBatchData w WHERE w.postApprovalStatus = :postApprovalStatus AND (w.clearingAccountingDocument = '' or w.clearingAccountingDocument is null) AND w.isHGDocumentTypeCompletedForNonPayouts = false AND w.royaltyRunIdentifierUrri LIKE :urriPrefix order by w.fiPostedTime")
  List<String> getURRIBasedOnPostApprovalStatusAndClearingAccountingDocumentAndURRIPrefix(
      PostApprovalStatus postApprovalStatus, String urriPrefix);

  // Get all the sap vendor codes from the DB for given territories
  @Query(
      "SELECT w.sapVendorCode FROM WorkbenchBatchData w WHERE w.territory = :territory AND w.sapVendorCode IS NOT NULL GROUP BY w.sapVendorCode ORDER BY MIN(w.sapClientCheck)")
  Page<String> findByTerritory(@Param("territory") String territory, PageRequest pageRequest);
}
