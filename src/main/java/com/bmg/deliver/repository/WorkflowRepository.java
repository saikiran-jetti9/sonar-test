package com.bmg.deliver.repository;

import com.bmg.deliver.model.Workflow;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
	Page<Workflow> findByName(String identifier, Pageable pageable);

	@Query("SELECT w FROM Workflow w JOIN Bookmark b ON b.workflowId = w.id WHERE b.remoteUser.username = :username")
	Page<Workflow> findWorkflowsByUsername(@Param("username") String username, Pageable pageable);

	@Query("""
			SELECT w FROM Workflow w
			JOIN Bookmark b ON b.workflowId = w.id
			WHERE b.remoteUser.username = :username
			AND (:enabled IS NULL OR w.enabled = :enabled)
			""")
	Page<Workflow> findFilteredWorkflowsByEnabled(@Param("username") String username, @Param("enabled") Boolean enabled,
			Pageable pageable);

	@Query("""
			SELECT w FROM Workflow w
			JOIN Bookmark b ON b.workflowId = w.id
			WHERE b.remoteUser.username = :username
			AND w.created BETWEEN :startDate AND :endDate
			""")
	Page<Workflow> findFilteredWorkflowsByDateRange(@Param("username") String username,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate, Pageable pageable);

	@Query("""
			SELECT w FROM Workflow w
			JOIN Bookmark b ON b.workflowId = w.id
			WHERE b.remoteUser.username = :username
			AND w.created BETWEEN :startDate AND :endDate
			AND (:enabled IS NULL OR w.enabled = :enabled)
			""")
	Page<Workflow> findFilteredWorkflowsByDateRangeAndEnabled(@Param("username") String username,
			@Param("enabled") Boolean enabled, @Param("startDate") Date startDate, @Param("endDate") Date endDate,
			Pageable pageable);

	Optional<Workflow> findByAlias(String alias);

	Page<Workflow> findByNameContainingIgnoreCase(String search, Pageable pageable);

	Page<Workflow> findByEnabled(boolean enabled, Pageable pageable);

	Page<Workflow> findByNameContainingIgnoreCaseAndEnabled(String name, boolean enabled, Pageable pageable);

	Page<Workflow> findByCreatedBetween(Date startDate, Date endDate, Pageable pageable);

	Page<Workflow> findByNameContainingIgnoreCaseAndEnabledAndCreatedBetween(String search, boolean enabled,
			Date startDate, Date endDate, Pageable pageable);

	Page<Workflow> findByNameContainingIgnoreCaseAndCreatedBetween(String search, Date startDate, Date endDate,
			Pageable pageable);

	Page<Workflow> findByEnabledAndCreatedBetween(boolean enabled, Date startDate, Date endDate, Pageable pageable);

}
