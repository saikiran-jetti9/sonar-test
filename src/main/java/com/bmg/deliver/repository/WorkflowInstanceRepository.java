package com.bmg.deliver.repository;

import com.bmg.deliver.dto.responsedto.WorkflowInstanceFilterDTO;
import com.bmg.deliver.enums.DeliveryType;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.model.WorkflowInstance;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {
	Page<WorkflowInstance> findByWorkflowId(Long id, Pageable pageable);

	void deleteAllByWorkflowId(Long workflowId);

	@Query("SELECT w FROM WorkflowInstance w WHERE w.workflow.id = :workflowId and w.status IN :statuses " + "ORDER BY "
			+ "CASE w.priority " + "  WHEN 'HIGH' THEN 1 " + "  WHEN 'MEDIUM' THEN 2 " + "  WHEN 'LOW' THEN 3 "
			+ "END, w.id ASC")
	List<WorkflowInstance> findAllByStatusOrderByPriorityAscIdAsc(List<WorkflowInstanceStatus> statuses,
			Long workflowId);

	Page<WorkflowInstance> findByIdentifier(String identifier, Pageable pageable);

	Page<WorkflowInstance> findByStatusIn(Pageable pageable, List<WorkflowInstanceStatus> statuses);
	List<WorkflowInstance> findByStatusIn(List<WorkflowInstanceStatus> statuses);

	@Query("SELECT w FROM WorkflowInstance w WHERE w.workflow.id = :workflowId AND w.status In :statuses")
	List<WorkflowInstance> findByWorkflowIdAndStatus(@Param("workflowId") Long workflowId,
			@Param("statuses") List<WorkflowInstanceStatus> statuses);

	@Query(value = "SELECT * FROM public.workflow_instance WHERE workflow_id = :workflowId AND status NOT IN ('FAILED', 'TERMINATED') AND identifier = :identifier AND id < :id ORDER BY id DESC LIMIT 1", nativeQuery = true)
	Optional<WorkflowInstance> findLastInstanceByWorkflowIdAndStatusNative(@Param("workflowId") Long workflowId,
			@Param("identifier") String identifier, @Param("id") Long instanceId);

	@Query("SELECT w FROM WorkflowInstance w WHERE w.status IN ('COMPLETED', 'FAILED') AND w.workflow.id = :workflowId ORDER BY w.completed DESC LIMIT 1")
	Optional<WorkflowInstance> findTopByStatusAndWorkflowIdOrderByCompletedDesc(@Param("workflowId") Long workflowId);

	Long countByWorkflowIdAndStatus(Long id, WorkflowInstanceStatus workflowInstanceStatus);

	Long countByWorkflowIdAndDeliveryTypeAndStatus(Long id, DeliveryType deliveryType,
			WorkflowInstanceStatus workflowInstanceStatus);

	@Query("SELECT w FROM WorkflowInstance w " + "WHERE w.workflow.id = :workflowId "
			+ "AND (COALESCE(:#{#filter.startDate}, w.created) = w.created OR w.created >= :#{#filter.startDate}) "
			+ "AND (COALESCE(:#{#filter.endDate}, w.created) = w.created OR w.created <= :#{#filter.endDate}) "
			+ "AND (COALESCE(:#{#filter.completedStart}, w.completed) = w.completed OR w.completed >= :#{#filter.completedStart}) "
			+ "AND (COALESCE(:#{#filter.completedEnd}, w.completed) = w.completed OR w.completed <= :#{#filter.completedEnd}) "
			+ "AND (:#{#filter.deliveryType} IS NULL OR w.deliveryType IN :#{#filter.deliveryType}) "
			+ "AND (:#{#filter.status} IS NULL OR w.status IN :#{#filter.status}) "
			+ "AND (:#{#filter.priority} IS NULL OR w.priority IN :#{#filter.priority}) "
			+ "AND (COALESCE(:#{#filter.duration}, w.duration) = w.duration OR w.duration <= :#{#filter.duration}) "
			+ "AND (:#{#filter.identifier} IS NULL OR w.identifier IN :#{#filter.identifier})")
	Page<WorkflowInstance> findWorkflowInstancesWithFilters(@Param("workflowId") Long workflowId,
			@Param("filter") WorkflowInstanceFilterDTO filter, Pageable pageable);

	Long countByStatus(WorkflowInstanceStatus workflowInstanceStatus);

	Long countByStatusIn(List<WorkflowInstanceStatus> pendingStatuses);

	@Query("SELECT COUNT(w) FROM WorkflowInstance w WHERE w.workflow.id = :workflowId AND w.status IN ('CREATED', 'QUEUED','PAUSED')")
	Long countByWorkflowIdAndPendingStatuses(@Param("workflowId") Long workflowId);

	@Query("SELECT w FROM WorkflowInstance w " + "WHERE w.status IN :statuses " + "ORDER BY " + "CASE w.priority "
			+ "  WHEN com.bmg.deliver.enums.Priority.HIGH THEN 1 "
			+ "  WHEN com.bmg.deliver.enums.Priority.MEDIUM THEN 2 "
			+ "  WHEN com.bmg.deliver.enums.Priority.LOW THEN 3 " + "END, w.created DESC")
	Page<WorkflowInstance> findAllByStatusOrderByPriorityAscCreatedDateDESC(
			@Param("statuses") List<WorkflowInstanceStatus> statuses, Pageable pageable);

	@Query("SELECT w FROM WorkflowInstance w " + "WHERE w.status IN :statuses " + "ORDER BY w.created DESC")
	Page<WorkflowInstance> findAllByStatusOrderByCreatedDesc(@Param("statuses") List<WorkflowInstanceStatus> statuses,
			Pageable pageable);

}
