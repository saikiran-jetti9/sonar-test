package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.config.MasterCondition;
import com.bmg.deliver.dto.WorkflowDTO;
import com.bmg.deliver.model.Bookmark;
import com.bmg.deliver.model.RemoteUser;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.repository.BookmarkRepository;
import com.bmg.deliver.repository.WorkflowInstanceRepository;
import com.bmg.deliver.repository.WorkflowRepository;
import com.bmg.deliver.service.BookmarkService;
import com.bmg.deliver.service.RemoteUserService;
import com.bmg.deliver.utils.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Conditional(MasterCondition.class)
@Service
public class BookmarkServiceImpl implements BookmarkService {

	private final BookmarkRepository bookmarkRepository;

	private final RemoteUserService remoteUserService;

	private final WorkflowInstanceRepository workflowInstanceRepository;

	private final WorkflowRepository workflowRepository;

	public BookmarkServiceImpl(BookmarkRepository bookmarkRepository, RemoteUserService remoteUserService,
			WorkflowInstanceRepository workflowInstanceRepository, WorkflowRepository workflowRepository) {
		this.bookmarkRepository = bookmarkRepository;
		this.remoteUserService = remoteUserService;
		this.workflowInstanceRepository = workflowInstanceRepository;
		this.workflowRepository = workflowRepository;
	}

	@Override
	public Bookmark createBookmark(Long id, RemoteUser user) {
		Bookmark bookmark = new Bookmark();
		bookmark.setWorkflowId(id);
		bookmark.setRemoteUser(user);
		return bookmarkRepository.save(bookmark);
	}

	@Override
	public boolean deleteBookmark(Long workflowId, Long userId) {
		try {
			Optional<Bookmark> bookmark = bookmarkRepository.findByWorkflowIdAndRemoteUser_Id(workflowId, userId);
			if (bookmark.isPresent()) {
				bookmarkRepository.delete(bookmark.get());
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			throw new RuntimeException(AppConstants.ERROR_DELETING_BOOKMARK + e.getMessage());
		}
	}

	@Override
	public List<Bookmark> getBookmarksByUsername(String username) {
		return bookmarkRepository.findBookmarksByUsername(username);
	}

	@Override
	public Page<WorkflowDTO> getWorkflowsByUsername(String username, Pageable pageable, Boolean enabled, Date startDate,
			Date endDate) {
		Optional<RemoteUser> user = remoteUserService.getUserByUsername(username);
		Date[] dates = {startDate, endDate};
		setFilterDates(dates);
		startDate = dates[0];
		endDate = dates[1];

		if (user.isEmpty()) {
			throw new IllegalArgumentException("Remote user not found by username");
		}
		Page<Workflow> workflows;
		if (enabled != null && startDate != null && endDate != null) {
			workflows = workflowRepository.findFilteredWorkflowsByDateRangeAndEnabled(username, enabled, startDate,
					endDate, pageable);
		} else if (enabled != null) {
			workflows = workflowRepository.findFilteredWorkflowsByEnabled(username, enabled, pageable);
		} else if (startDate != null && endDate != null) {
			workflows = workflowRepository.findFilteredWorkflowsByDateRange(username, startDate, endDate, pageable);
		} else {
			workflows = workflowRepository.findWorkflowsByUsername(username, pageable);
		}

		if (workflows.isEmpty()) {
			throw new IllegalArgumentException("No workflows found for user");
		}

		return workflows.map(this::convertToWorkflowDTO);
	}

	/**
	 * Helper Method to Format Dates
	 *
	 * @param dates
	 */
	private void setFilterDates(Date[] dates) {

		if (dates[0] != null && dates[1] == null) {
			dates[1] = dates[0];
		}

		if (dates[0] != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dates[0]);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			dates[0] = cal.getTime();
		}

		if (dates[1] != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dates[1]);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			dates[1] = cal.getTime(); // Set adjusted completed date
		}
	}

	/**
	 * Helper Method to convert workflow to dto
	 *
	 * @param workflow
	 */
	private WorkflowDTO convertToWorkflowDTO(Workflow workflow) {
		WorkflowDTO workflowDTO = new WorkflowDTO();
		workflowDTO.setId(workflow.getId());
		workflowDTO.setName(workflow.getName());
		workflowDTO.setDescription(workflow.getDescription());
		workflowDTO.setEnabled(workflow.isEnabled());
		workflowDTO.setThrottleLimit(workflow.getThrottleLimit());
		workflowDTO.setCreated(workflow.getCreated());
		workflowDTO.setModified(workflow.getModified());
		workflowDTO.setIsTaskChainIsValid(workflow.isTaskChainIsValid());
		workflowDTO.setAlias(workflow.getAlias());
		workflowDTO.setPaused(workflow.isPaused());

		Optional<WorkflowInstance> optionalInstance = workflowInstanceRepository
				.findTopByStatusAndWorkflowIdOrderByCompletedDesc(workflow.getId());
		optionalInstance.ifPresent(instance -> workflowDTO.setStatus(instance.getStatus().toString()));

		return workflowDTO;
	}
}
