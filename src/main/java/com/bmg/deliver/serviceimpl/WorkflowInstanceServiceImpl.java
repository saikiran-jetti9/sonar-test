package com.bmg.deliver.serviceimpl;

import com.bmg.deliver.dto.*;
import com.bmg.deliver.dto.responsedto.ResponseWorkflowInstanceDTO;
import com.bmg.deliver.dto.responsedto.WorkflowInstanceFilterDTO;
import com.bmg.deliver.enums.DeliveryType;
import com.bmg.deliver.enums.Priority;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.exceptions.WorkflowInstanceIdNotFoundException;
import com.bmg.deliver.exceptions.WorkflowInstancesNotFoundException;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.repository.WorkflowInstanceRepository;
import com.bmg.deliver.repository.WorkflowRepository;
import com.bmg.deliver.service.WorkflowInstanceService;
import com.bmg.deliver.utils.AppConstants;
import com.nimbusds.jose.shaded.gson.JsonObject;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WorkflowInstanceServiceImpl implements WorkflowInstanceService {

	@Autowired
	private WorkflowInstanceRepository workflowInstanceRepository;

	@Autowired
	private WorkflowRepository workflowRepository;

	@Override
	public WorkflowInstance createWorkFlowInstance(JsonObject triggerData, Workflow workflow) {
		WorkflowInstance workflowInstance = new WorkflowInstance();
		JsonObject releaseProduct = getReleaseProduct(triggerData);

		Priority priority = getPriority(releaseProduct);
		DeliveryType deliveryType = getDeliveryType(releaseProduct);

		workflowInstance.setWorkflow(workflow);
		workflowInstance.setPriority(priority);
		workflowInstance.setDeliveryType(deliveryType);

		workflowInstance.setStatus(WorkflowInstanceStatus.CREATED);
		workflowInstance.setIdentifier(
				releaseProduct.has(AppConstants.BARCODE) ? releaseProduct.get(AppConstants.BARCODE).getAsString() : "");
		workflowInstance.setTriggerData(triggerData != null ? triggerData.toString() : "");

		return workflowInstanceRepository.save(workflowInstance);
	}

	private JsonObject getReleaseProduct(JsonObject triggerData) {
		return (triggerData != null && triggerData.has(AppConstants.RELEASE_PRODUCT))
				? triggerData.getAsJsonObject(AppConstants.RELEASE_PRODUCT)
				: new JsonObject();
	}

	private Priority getPriority(JsonObject releaseProduct) {
		try {
			if (releaseProduct.has(AppConstants.PRODUCT_SUMMERY)
					&& releaseProduct.getAsJsonObject(AppConstants.PRODUCT_SUMMERY).has(AppConstants.PRIORITY)) {
				String priorityStr = releaseProduct.getAsJsonObject(AppConstants.PRODUCT_SUMMERY)
						.get(AppConstants.PRIORITY).getAsString().toUpperCase();
				if (AppConstants.LOW.equals(priorityStr) || AppConstants.MINOR.equals(priorityStr)
						|| AppConstants.MEDIUM.equals(priorityStr) || AppConstants.HIGH.equals(priorityStr)) {
					return AppConstants.MINOR.equals(priorityStr) ? Priority.LOW : Priority.valueOf(priorityStr);
				}
			}
		} catch (Exception e) {
			log.error("Invalid priority value in trigger data", e);
		}
		return Priority.LOW;
	}

	private DeliveryType getDeliveryType(JsonObject releaseProduct) {
		if (releaseProduct.has(AppConstants.DELIVERY_TYPE)
				&& !releaseProduct.get(AppConstants.DELIVERY_TYPE).isJsonNull()) {
			String deliveryType = releaseProduct.get(AppConstants.DELIVERY_TYPE).getAsString();
			return DeliveryType.fromValue(deliveryType);

		} else if (releaseProduct.has(AppConstants.IS_DATA_ONLY_TRIGGER)
				&& !releaseProduct.get(AppConstants.IS_DATA_ONLY_TRIGGER).isJsonNull()) {
			return releaseProduct.get(AppConstants.IS_DATA_ONLY_TRIGGER).getAsBoolean()
					? DeliveryType.DATA_ONLY
					: DeliveryType.FULL_DELIVERY;
		}
		return DeliveryType.NONE;
	}

	@Override
	public Page<ResponseWorkflowInstanceDTO> listWorkflowInstances(Long id, Pageable pageable,
			WorkflowInstanceFilterDTO filter) {

		Date[] dates = {filter.getStartDate(), filter.getEndDate(), filter.getCompletedStart(),
				filter.getCompletedEnd()};
		setFilterDates(dates);
		filter.setStartDate(dates[0]);
		filter.setEndDate(dates[1]);
		filter.setCompletedStart(dates[2]);
		filter.setCompletedEnd(dates[3]);

		boolean isFilterApplied = filter.getStartDate() != null || filter.getEndDate() != null
				|| filter.getCompletedStart() != null || filter.getCompletedEnd() != null
				|| filter.getDeliveryType() != null || filter.getStatus() != null || filter.getPriority() != null
				|| filter.getDuration() != null || filter.getIdentifier() != null;

		Page<WorkflowInstance> workflowInstances;
		if (!isFilterApplied) {
			workflowInstances = workflowInstanceRepository.findByWorkflowId(id, pageable);
		} else {
			workflowInstances = workflowInstanceRepository.findWorkflowInstancesWithFilters(id, filter, pageable);
		}

		List<ResponseWorkflowInstanceDTO> responseWorkflowInstanceDTOS = new ArrayList<>();
		for (WorkflowInstance workflowInstance : workflowInstances.getContent()) {
			ResponseWorkflowInstanceDTO responseWorkflowInstanceDTO = new ResponseWorkflowInstanceDTO();
			responseWorkflowInstanceDTO.setId(workflowInstance.getId());
			responseWorkflowInstanceDTO.setWorkflowId(workflowInstance.getWorkflow().getId());
			responseWorkflowInstanceDTO.setStatus(workflowInstance.getStatus());
			responseWorkflowInstanceDTO.setCompleted(workflowInstance.getCompleted());
			responseWorkflowInstanceDTO.setDuration(workflowInstance.getDuration());
			responseWorkflowInstanceDTO.setIdentifier(workflowInstance.getIdentifier());
			responseWorkflowInstanceDTO.setPriority(workflowInstance.getPriority());
			responseWorkflowInstanceDTO.setDeliveryType(workflowInstance.getDeliveryType());
			responseWorkflowInstanceDTO.setCreated(workflowInstance.getCreated());
			responseWorkflowInstanceDTO.setModified(workflowInstance.getModified());
			responseWorkflowInstanceDTOS.add(responseWorkflowInstanceDTO);
		}
		return new PageImpl<>(responseWorkflowInstanceDTOS, pageable, workflowInstances.getTotalElements());
	}

	/**
	 * Helper method to set and format start and end dates for filtering. Adjusts
	 * the time components of dates to cover the entire day if only the date is
	 * provided.
	 *
	 * @param dates
	 *            Array containing startDate, endDate, completedStart, and
	 *            completedEnd.
	 */
	private void setFilterDates(Date[] dates) {
		if (dates[0] != null && dates[1] == null) {
			dates[1] = dates[0]; // If endDate is null, set it to startDate.
		}

		if (dates[2] != null && dates[3] == null) {
			dates[3] = dates[2]; // If completedEnd is null, set it to completedStart.
		}

		// Set startDate to beginning of the day
		if (dates[0] != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dates[0]);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			dates[0] = cal.getTime();
		}

		// Set endDate to end of the day
		if (dates[1] != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dates[1]);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			dates[1] = cal.getTime();
		}

		// Set completedStart to beginning of the day
		if (dates[2] != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dates[2]);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			dates[2] = cal.getTime();
		}

		// Set completedEnd to end of the day
		if (dates[3] != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dates[3]);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			dates[3] = cal.getTime();
		}
	}

	@Override
	public Optional<WorkflowInstance> getWorkflowInstanceById(Long instanceId) {
		Optional<WorkflowInstance> workflowInstanceOptional = workflowInstanceRepository.findById(instanceId);
		if (workflowInstanceOptional.isEmpty()) {
			throw new WorkflowInstanceIdNotFoundException(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND + instanceId);
		}
		return workflowInstanceOptional;
	}

	@Override
	public String getLogsOfWorkflowInstance(Long instanceId) {
		Optional<WorkflowInstance> workflowInstance = workflowInstanceRepository.findById(instanceId);
		if (workflowInstance.isPresent()) {
			return workflowInstance.get().getLog();
		} else {
			throw new WorkflowInstancesNotFoundException(AppConstants.WORKFLOW_INSTANCES_NOT_FOUND);
		}
	}

	@Override
	public Page<ResponseWorkflowInstanceDTO> getWorkflowsByIdentifier(String identifier, Pageable pageable) {
		Page<WorkflowInstance> workflowInstances = workflowInstanceRepository.findByIdentifier(identifier, pageable);
		List<ResponseWorkflowInstanceDTO> responseWorkflowInstanceDTOS = new ArrayList<>();
		for (WorkflowInstance workflowInstance : workflowInstances.getContent()) {
			ResponseWorkflowInstanceDTO responseWorkflowInstanceDTO = new ResponseWorkflowInstanceDTO();
			responseWorkflowInstanceDTO.setId(workflowInstance.getId());
			responseWorkflowInstanceDTO.setWorkflowId(workflowInstance.getWorkflow().getId());
			responseWorkflowInstanceDTO.setStatus(workflowInstance.getStatus());
			responseWorkflowInstanceDTO.setCompleted(workflowInstance.getCompleted());
			responseWorkflowInstanceDTO.setDuration(workflowInstance.getDuration());
			responseWorkflowInstanceDTO.setIdentifier(workflowInstance.getIdentifier());
			responseWorkflowInstanceDTO.setPriority(workflowInstance.getPriority());
			responseWorkflowInstanceDTO.setDeliveryType(workflowInstance.getDeliveryType());
			responseWorkflowInstanceDTO.setCreated(workflowInstance.getCreated());
			responseWorkflowInstanceDTO.setModified(workflowInstance.getModified());
			responseWorkflowInstanceDTOS.add(responseWorkflowInstanceDTO);
		}
		return new PageImpl<>(responseWorkflowInstanceDTOS, pageable, workflowInstances.getTotalElements());
	}
	//
	// @Override
	// public Page<WorkflowInstance> getAllInstances(Pageable pageable,
	// WorkflowInstanceStatus status) {
	// List<WorkflowInstanceStatus> statuses;
	// if (status.equals(WorkflowInstanceStatus.PENDING)) {
	// statuses = Arrays.asList(WorkflowInstanceStatus.CREATED,
	// WorkflowInstanceStatus.QUEUED,
	// WorkflowInstanceStatus.PAUSED);
	// } else {
	// statuses = Arrays.asList(status);
	// }
	// return workflowInstanceRepository.findByStatusIn(pageable, statuses);
	// }

	@Override
	public Page<ResponseWorkflowInstanceDTO> getWorkflowInstancesByStatus(Pageable pageable,
			WorkflowInstanceStatus status) {
		List<WorkflowInstanceStatus> statuses;

		if (status.equals(WorkflowInstanceStatus.PENDING)) {
			statuses = Arrays.asList(WorkflowInstanceStatus.CREATED, WorkflowInstanceStatus.QUEUED,
					WorkflowInstanceStatus.PAUSED);
			Page<WorkflowInstance> workflowInstances = workflowInstanceRepository
					.findAllByStatusOrderByPriorityAscCreatedDateDESC(statuses, pageable);
			return convertToResponseWorkflowInstanceDTO(workflowInstances);
		} else if (status.equals(WorkflowInstanceStatus.RUNNING)) {
			statuses = Collections.singletonList(WorkflowInstanceStatus.RUNNING);
			Page<WorkflowInstance> workflowInstances = workflowInstanceRepository
					.findAllByStatusOrderByCreatedDesc(statuses, pageable);
			return convertToResponseWorkflowInstanceDTO(workflowInstances);
		}
		return null;
	}

	private Page<ResponseWorkflowInstanceDTO> convertToResponseWorkflowInstanceDTO(
			Page<WorkflowInstance> workflowInstances) {
		List<ResponseWorkflowInstanceDTO> responseWorkflowInstanceDTOS = new ArrayList<>();
		for (WorkflowInstance workflowInstance : workflowInstances.getContent()) {
			ResponseWorkflowInstanceDTO responseWorkflowInstanceDTO = new ResponseWorkflowInstanceDTO();
			responseWorkflowInstanceDTO.setId(workflowInstance.getId());
			responseWorkflowInstanceDTO.setWorkflowId(workflowInstance.getWorkflow().getId());
			responseWorkflowInstanceDTO.setWorkflowName(workflowInstance.getWorkflow().getName());
			responseWorkflowInstanceDTO.setStatus(workflowInstance.getStatus());
			responseWorkflowInstanceDTO.setCompleted(workflowInstance.getCompleted());
			responseWorkflowInstanceDTO.setDuration(workflowInstance.getDuration());
			responseWorkflowInstanceDTO.setIdentifier(workflowInstance.getIdentifier());
			responseWorkflowInstanceDTO.setPriority(workflowInstance.getPriority());
			responseWorkflowInstanceDTO.setDeliveryType(workflowInstance.getDeliveryType());
			responseWorkflowInstanceDTO.setStarted(workflowInstance.getStarted());
			responseWorkflowInstanceDTO.setCreated(workflowInstance.getCreated());
			responseWorkflowInstanceDTO.setModified(workflowInstance.getModified());
			responseWorkflowInstanceDTOS.add(responseWorkflowInstanceDTO);
		}
		return new PageImpl<>(responseWorkflowInstanceDTOS, workflowInstances.getPageable(),
				workflowInstances.getTotalElements());
	}

	@Override
	public StatisticsDTO getWorkflowsStatistics(Long id) {
		Long totalInstancesSuccessful = workflowInstanceRepository.countByWorkflowIdAndStatus(id,
				WorkflowInstanceStatus.COMPLETED);
		Long totalInstancesFailed = workflowInstanceRepository.countByWorkflowIdAndStatus(id,
				WorkflowInstanceStatus.FAILED);

		StatisticsDTO statsDTO = new StatisticsDTO();
		statsDTO.setTotalSuccessfulInstances(totalInstancesSuccessful);
		statsDTO.setTotalFailedInstances(totalInstancesFailed);

		Map<String, DeliveryTypeStatsDTO> deliveryTypeStatsMap = new HashMap<>();
		for (DeliveryType deliveryType : DeliveryType.values()) {
			Long successful = workflowInstanceRepository.countByWorkflowIdAndDeliveryTypeAndStatus(id, deliveryType,
					WorkflowInstanceStatus.COMPLETED);
			Long failed = workflowInstanceRepository.countByWorkflowIdAndDeliveryTypeAndStatus(id, deliveryType,
					WorkflowInstanceStatus.FAILED);

			DeliveryTypeStatsDTO deliveryTypeStatsDTO = new DeliveryTypeStatsDTO();
			deliveryTypeStatsDTO.setSuccessful(successful);
			deliveryTypeStatsDTO.setFailures(failed);

			deliveryTypeStatsMap.put(deliveryType.toString(), deliveryTypeStatsDTO);
		}
		statsDTO.setDeliveryTypeStats(deliveryTypeStatsMap);

		return statsDTO;
	}

	@Override
	public TotalStatusCountDTO retrieveTotalWorkflowsStatusCount() {
		Long runningCount = workflowInstanceRepository.countByStatus(WorkflowInstanceStatus.RUNNING);
		List<WorkflowInstanceStatus> pendingStatuses = Arrays.asList(WorkflowInstanceStatus.CREATED,
				WorkflowInstanceStatus.QUEUED, WorkflowInstanceStatus.PAUSED);
		Long pendingCount = workflowInstanceRepository.countByStatusIn(pendingStatuses);

		TotalStatusCountDTO statusCount = new TotalStatusCountDTO();
		statusCount.setPendingCount(pendingCount);
		statusCount.setRunningCount(runningCount);
		return statusCount;
	}

	@Override
	public TotalStatusCountDTO getStatusCountByworkflow(Long id) {
		Long runningCount = workflowInstanceRepository.countByWorkflowIdAndStatus(id, WorkflowInstanceStatus.RUNNING);
		Long pendingCount = workflowInstanceRepository.countByWorkflowIdAndPendingStatuses(id);
		TotalStatusCountDTO workflowStatusCountDTO = new TotalStatusCountDTO();
		workflowStatusCountDTO.setRunningCount(runningCount);
		workflowStatusCountDTO.setPendingCount(pendingCount);
		return workflowStatusCountDTO;
	}

	@Override
	public void deleteWorkflowInstance(Long id) {
		Optional<WorkflowInstance> workflowInstanceOptional = workflowInstanceRepository.findById(id);
		if (workflowInstanceOptional.isPresent()) {
			workflowInstanceRepository.deleteById(id);
		} else {
			throw new WorkflowInstanceIdNotFoundException(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND + id);
		}
	}

	@Override
	public List<WorkflowStatusCountDTO> retrieveStatusCountByWorkflow() {
		List<Workflow> workflows = workflowRepository.findAll();
		List<WorkflowStatusCountDTO> workflowInstanceCounts = new ArrayList<>();
		for (Workflow workflow : workflows) {
			WorkflowStatusCountDTO workflowInstanceCount = new WorkflowStatusCountDTO();

			workflowInstanceCount.setWorkflowName(workflow.getName());
			workflowInstanceCount.setPaused(workflow.isPaused());
			workflowInstanceCount.setWorkflowId(workflow.getId());
			Optional<WorkflowInstance> optionalInstance = workflowInstanceRepository
					.findTopByStatusAndWorkflowIdOrderByCompletedDesc(workflow.getId());
			optionalInstance.ifPresent(instance -> {
				workflowInstanceCount.setCompleted(instance.getCompleted());
				workflowInstanceCount.setStarted(instance.getStarted());
			});

			TotalStatusCountDTO workflowStatusCountDTO = getStatusCountByworkflow(workflow.getId());
			workflowInstanceCount.setTotalInstances(getStatusCountByworkflow(workflow.getId()));

			if (workflowStatusCountDTO.getPendingCount() > 0 || workflowStatusCountDTO.getRunningCount() > 0) {
				workflowInstanceCounts.add(workflowInstanceCount);
			}
		}

		workflowInstanceCounts.sort(Comparator.comparing(WorkflowStatusCountDTO::getWorkflowName));
		return workflowInstanceCounts;
	}

	/**
	 * This method is used to update the status of instances based on the pause of a
	 * specific workflow.
	 *
	 * @param id
	 * @param workflowDTO
	 */
	@Override
	public void updateWorkflowInstanceStatus(Long id, WorkflowDTO workflowDTO) {
		boolean isPaused = workflowDTO.getPaused();
		if (isPaused) {
			List<WorkflowInstanceStatus> statuses = Arrays.asList(WorkflowInstanceStatus.CREATED);
			List<WorkflowInstance> queuedInstances = workflowInstanceRepository.findByWorkflowIdAndStatus(id, statuses);
			queuedInstances.forEach(instance -> {
				instance.setStatus(WorkflowInstanceStatus.PAUSED);
				workflowInstanceRepository.save(instance);
				log.info("The instance with id {} is paused", instance.getId());
			});
		} else {
			List<WorkflowInstanceStatus> statuses = Arrays.asList(WorkflowInstanceStatus.PAUSED);
			List<WorkflowInstance> pausedInstances = workflowInstanceRepository.findByWorkflowIdAndStatus(id, statuses);
			pausedInstances.forEach(instance -> {
				instance.setStatus(WorkflowInstanceStatus.CREATED);
				workflowInstanceRepository.save(instance);
				// masterService.processOnApi(instance);
				log.info("The instance with id {} is resumed", instance.getId());
			});

		}
	}

}
