package com.bmg.deliver.serviceimpl.master;

import com.bmg.deliver.config.MasterCondition;
import com.bmg.deliver.dto.TotalStatusCountDTO;
import com.bmg.deliver.dto.WorkflowInstanceMessageDTO;
import com.bmg.deliver.enums.DeliveryType;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.rabbitmq.master.MasterProducer;
import com.bmg.deliver.repository.SystemPropertiesRepository;
import com.bmg.deliver.repository.WorkflowConfigurationRepository;
import com.bmg.deliver.repository.WorkflowInstanceRepository;
import com.bmg.deliver.repository.WorkflowRepository;
import com.bmg.deliver.service.EmailService;
import com.bmg.deliver.service.WorkflowInstanceService;
import com.bmg.deliver.utils.AppConstants;
import com.bmg.deliver.utils.TimeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Conditional(MasterCondition.class)
@EnableScheduling
@Data
public class MasterService {
	private final WorkflowRepository workflowRepository;
	private final MasterProducer masterProducer;
	private final WorkflowInstanceRepository workflowInstanceRepository;
	private final WorkflowConfigurationRepository workflowConfigurationRepository;
	private final WorkflowInstanceService workflowInstanceService;

	private final SystemPropertiesRepository systemPropertiesRepository;

	private final ActiveMqService activeMqService;
	private final EmailService emailService;

	private final SimpMessagingTemplate messagingTemplate;

	private final Map<Long, List<WorkflowInstanceMessageDTO>> queuedInstances = new HashMap<>();
	private final Map<Long, List<WorkflowInstanceMessageDTO>> timeIntervalInstances = new HashMap<>();

	private final Map<Long, List<Long>> runningInstances = new HashMap<>();
	private final Set<WorkflowDTO> workflows = new HashSet<>();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private boolean isPaused = false;

	MasterService(WorkflowRepository workflowRepository, MasterProducer masterProducer,
			WorkflowInstanceRepository workflowInstanceRepository,
			WorkflowConfigurationRepository workflowConfigurationRepository,
			WorkflowInstanceService workflowInstanceService, SystemPropertiesRepository systemPropertiesRepository,
			ActiveMqService activeMqService, EmailService emailService, SimpMessagingTemplate messagingTemplate) {
		this.workflowRepository = workflowRepository;
		this.masterProducer = masterProducer;
		this.workflowInstanceRepository = workflowInstanceRepository;
		this.workflowConfigurationRepository = workflowConfigurationRepository;
		this.workflowInstanceService = workflowInstanceService;
		this.systemPropertiesRepository = systemPropertiesRepository;
		this.activeMqService = activeMqService;
		this.emailService = emailService;
		this.messagingTemplate = messagingTemplate;
	}

	@Data
	public static class WorkflowDTO {
		private Long id;
		private int throttleLimit;
		private boolean enabled;
		private boolean paused;
	}

	@PostConstruct
	public synchronized void init() {
		List<Workflow> workflowList = workflowRepository.findAll();
		workflowList.forEach(workflow -> {
			WorkflowDTO workflowDTO = new WorkflowDTO();
			workflowDTO.setId(workflow.getId());
			workflowDTO.setThrottleLimit(workflow.getThrottleLimit());
			workflowDTO.setEnabled(workflow.isEnabled());
			workflowDTO.setPaused(workflow.isPaused());
			this.workflows.add(workflowDTO);

			List<WorkflowInstanceStatus> statuses = Arrays.asList(WorkflowInstanceStatus.CREATED,
					WorkflowInstanceStatus.QUEUED, WorkflowInstanceStatus.RUNNING, WorkflowInstanceStatus.PAUSED);
			List<WorkflowInstance> workflowInstances = workflowInstanceRepository
					.findAllByStatusOrderByPriorityAscIdAsc(statuses, workflowDTO.getId());

			List<WorkflowInstanceMessageDTO> workflowInstanceMessageDTOS = new ArrayList<>();
			List<WorkflowInstanceMessageDTO> queuedWorkflowInstanceMessageDTOS = new ArrayList<>();
			List<WorkflowInstanceMessageDTO> timeIntervalInstanceMsgDTOS = new ArrayList<>();
			workflowInstances.forEach(workflowInstance -> {
				WorkflowInstanceMessageDTO messageDTO = new WorkflowInstanceMessageDTO();
				messageDTO.setId(workflowInstance.getId());
				messageDTO.setWorkflowId(workflowInstance.getWorkflow().getId());
				messageDTO.setPriority(workflowInstance.getPriority());

				long waitTime = calculateWaitTime(workflowInstance);
				if (workflowInstance.getStatus() == WorkflowInstanceStatus.QUEUED) {
					queuedWorkflowInstanceMessageDTOS.add(messageDTO);
				} else if (waitTime > 0) {
					timeIntervalInstanceMsgDTOS.add(messageDTO);
					// TODO added Running status to handle the case where the instance is running
					// and the system is restarted
				} else if ((workflowInstance.getStatus() == WorkflowInstanceStatus.CREATED)
						|| (workflowInstance.getStatus() == WorkflowInstanceStatus.RUNNING)
						|| (workflowInstance.getStatus() == WorkflowInstanceStatus.PAUSED)) {
					workflowInstanceMessageDTOS.add(messageDTO);
				}

			});
			Collections.sort(timeIntervalInstanceMsgDTOS);
			timeIntervalInstances.put(workflow.getId(), timeIntervalInstanceMsgDTOS);
			Collections.sort(workflowInstanceMessageDTOS);
			queuedInstances.put(workflow.getId(), workflowInstanceMessageDTOS);
			if (queuedWorkflowInstanceMessageDTOS.isEmpty()) {
				runningInstances.put(workflow.getId(), new ArrayList<>());
			} else {
				runningInstances.put(workflow.getId(), new ArrayList<>(
						queuedWorkflowInstanceMessageDTOS.stream().map(WorkflowInstanceMessageDTO::getId).toList()));
			}
			log.info("Workflow instances for workflow id {} found: {}", workflow.getId(),
					workflowInstances.stream().map(WorkflowInstance::getId).toList());
		});
		log.info("Workflows found during initialization: {}", workflows);
		processOnStartup();
		log.info("Initialization of MasterService is done!");
	}

	public void processOnStartup() {
		log.info("Processing on startup...");
		this.isPaused = systemPropertiesRepository.findByKey(AppConstants.PAUSED)
				.map(systemProperties -> Boolean.parseBoolean(systemProperties.getValue())).orElse(false);
		handlePausedWorkflowInstances(workflows);
		for (WorkflowDTO workflowDTO : workflows) {
			Long workflowId = workflowDTO.getId();
			if (workflowDTO.isEnabled()) {
				List<WorkflowInstanceMessageDTO> queuedInstancesForWorkflow = queuedInstances.get(workflowDTO.getId());

				List<WorkflowInstanceMessageDTO> processedInstances = new ArrayList<>();
				for (WorkflowInstanceMessageDTO workflowInstanceMessageDTO : queuedInstancesForWorkflow) {
					if (shouldPickUpWorkflowInstance(workflowId, workflowDTO.getThrottleLimit())) {
						pushToWorkQueue(workflowInstanceMessageDTO);
						processedInstances.add(workflowInstanceMessageDTO);
					} else {
						break;
					}
				}
				queuedInstancesForWorkflow.removeAll(processedInstances);
				queuedInstances.put(workflowDTO.getId(), queuedInstancesForWorkflow);
			}
		}
		log.info("Processing on startup is done!");
	}

	public void processOnApi(WorkflowInstance workflowInstance) {
		log.info("Processing workflow instance {} on API call...", workflowInstance.getId());
		WorkflowInstanceMessageDTO messageDTO = new WorkflowInstanceMessageDTO();
		messageDTO.setId(workflowInstance.getId());
		messageDTO.setWorkflowId(workflowInstance.getWorkflow().getId());
		messageDTO.setPriority(workflowInstance.getPriority());

		long workflowId = workflowInstance.getWorkflow().getId();
		if (workflowInstance.getWorkflow().isPaused() || this.isPaused) {
			log.info("Workflow paused with id {} ", workflowId);
			handlePausedWorkflowInstances(workflows);
		}

		long waitTime = calculateWaitTime(workflowInstance);
		if (waitTime > 0) {
			log.info("Added workflow instance id {} to time interval instances with wait time of {} ms",
					messageDTO.getId(), waitTime);
			addToTimeIntervalInstances(messageDTO);
		} else {
			handleWorkflowInstance(workflowId, messageDTO);
		}
	}

	private void handleWorkflowInstance(Long workflowId, WorkflowInstanceMessageDTO instanceMessageDTO) {
		if (shouldPickUpWorkflowInstance(workflowId, getThrottleLimit(workflowId))) {
			log.info("Pushing workflow instance id {} to work queue", instanceMessageDTO.getId());
			pushToWorkQueue(instanceMessageDTO);
		} else {
			log.info("Adding workflow instance id {} to queued instances", instanceMessageDTO.getId());
			List<WorkflowInstanceMessageDTO> queuedInstancesForWorkflow = queuedInstances.computeIfAbsent(workflowId,
					k -> new ArrayList<>());
			queuedInstancesForWorkflow.add(instanceMessageDTO);
			Collections.sort(queuedInstancesForWorkflow);
			queuedInstances.put(workflowId, queuedInstancesForWorkflow);
		}
	}

	private void addToTimeIntervalInstances(WorkflowInstanceMessageDTO messageDTO) {
		List<WorkflowInstanceMessageDTO> timeIntervalInstancesForWorkflow = timeIntervalInstances
				.computeIfAbsent(messageDTO.getWorkflowId(), k -> new ArrayList<>());
		timeIntervalInstancesForWorkflow.add(messageDTO);
		Collections.sort(timeIntervalInstancesForWorkflow);
		timeIntervalInstances.put(messageDTO.getWorkflowId(), timeIntervalInstancesForWorkflow);
	}

	private long calculateWaitTime(WorkflowInstance workflowInstance) {

		long waitTime = getWaitingTimeBasedOnLastInstance(workflowInstance);
		long currentTime = System.currentTimeMillis();
		if (waitTime == AppConstants.WAIT_UNTIL_LAST_INSTANCE_COMPLETE) {
			return AppConstants.WAIT_UNTIL_LAST_INSTANCE_COMPLETE;
		}
		return Math.max(0, waitTime - currentTime);
	}

	public void processOnWorkResult(WorkflowInstanceMessageDTO messageDTO) {
		log.info("Processing workflow instance {} on work result", messageDTO.getId());
		if (runningInstances.containsKey(messageDTO.getWorkflowId())) {
			List<Long> runningInstanceIds = runningInstances.get(messageDTO.getWorkflowId());
			runningInstanceIds.remove(messageDTO.getId());
			runningInstances.put(messageDTO.getWorkflowId(), runningInstanceIds);
		}
		WorkflowInstance workflowInstance = workflowInstanceRepository.findById(messageDTO.getId()).orElse(null);
		if (workflowInstance != null) {
			activeMqService.sendMessage(workflowInstance);

			// To update websockets
			log.info("Before Web socket call before COMPLETING status");

			TotalStatusCountDTO statusCounts = workflowInstanceService.retrieveTotalWorkflowsStatusCount();
			messagingTemplate.convertAndSend("/topic/workflow-status-counts", statusCounts);
			messagingTemplate.convertAndSend("/topic/workflow-updates",
					workflowInstanceService.retrieveStatusCountByWorkflow());
			log.info("Before Web socket call after COMPLETING status");

		}
		emailService.emailNotifier(messageDTO.getId(), messageDTO.getWorkflowId());
		processQueuedInstances(messageDTO.getWorkflowId());
	}

	public void processQueuedInstances(Long workflowId) {
		if (queuedInstances.containsKey(workflowId)) {
			WorkflowDTO workflowDTO = workflows.stream()
					.filter(workflow -> Objects.equals(workflow.getId(), workflowId)).findFirst().orElse(null);
			Workflow workflow = workflowRepository.findById(workflowId).orElse(null);
			if (workflow != null && (workflow.isPaused() || this.isPaused)) {
				log.info("Workflow id {} is paused", workflowId);
				handlePausedWorkflowInstances(workflows);
				return;
			}

			if (null != workflowDTO && workflowDTO.isEnabled()) {
				List<WorkflowInstanceMessageDTO> instanceMessageDTOS = queuedInstances.get(workflowId);
				instanceMessageDTOS.sort(
						Comparator.comparingInt((WorkflowInstanceMessageDTO a) -> a.getPriority().getPriorityValue())
								.thenComparingLong(WorkflowInstanceMessageDTO::getId));
				List<WorkflowInstanceMessageDTO> processedInstances = new ArrayList<>();
				for (WorkflowInstanceMessageDTO instanceMessageDTO : instanceMessageDTOS) {
					if (shouldPickUpWorkflowInstance(workflowId, workflowDTO.getThrottleLimit())) {
						pushToWorkQueue(instanceMessageDTO);
						processedInstances.add(instanceMessageDTO);
					} else {
						break;
					}
				}
				instanceMessageDTOS.removeAll(processedInstances);
				queuedInstances.put(workflowDTO.getId(), instanceMessageDTOS);
			}
		}
	}

	public boolean shouldPickUpWorkflowInstance(long workflowId, long workflowThrottleLimit) {
		WorkflowDTO workflowDTO = workflows.stream().filter(workflow -> Objects.equals(workflow.getId(), workflowId))
				.findFirst().orElse(null);
		log.info("workflows WorkflowDTO {} {}", workflows, workflowDTO);
		if (workflowDTO == null || !workflowDTO.isEnabled()) {
			return false;
		}

		List<Long> runningInstanceIds = runningInstances.get(workflowId);
		if (runningInstanceIds.size() >= workflowThrottleLimit) {
			log.info("Throttle limit reached for workflow id {} {}", workflowId, runningInstanceIds);
			return false;
		}
		return true;
	}

	@Transactional
	public void pushToWorkQueue(WorkflowInstanceMessageDTO messageDTO) {
		try {
			WorkflowInstance workflowInstance = workflowInstanceRepository.findById(messageDTO.getId()).orElse(null);
			// TODO added Running status to handle the case where the instance is running
			// and the system is restarted
			if (workflowInstance != null && (workflowInstance.getStatus() == WorkflowInstanceStatus.CREATED
					|| workflowInstance.getStatus() == WorkflowInstanceStatus.QUEUED
					|| workflowInstance.getStatus() == WorkflowInstanceStatus.RUNNING)) {
				runningInstances.computeIfAbsent(messageDTO.getWorkflowId(), instances -> new ArrayList<>())
						.add(messageDTO.getId());
				log.info("Running instances {}", runningInstances);
				log.info("Queued instance {}", queuedInstances);
				try {
					masterProducer.sendMessage(objectMapper.writeValueAsString(messageDTO));
					workflowInstance.setStatus(WorkflowInstanceStatus.QUEUED);
					workflowInstanceRepository.save(workflowInstance);
				} catch (Exception e) {
					log.error("Error sending message to work queue: {}", ExceptionUtils.getStackTrace(e));
				}

			}
		} catch (Exception e) {
			log.error("Exception in pushToWorkQueue {}", ExceptionUtils.getStackTrace(e));
		}
	}

	public void updateWorkflow(Workflow workflow) {
		log.info("Updating workflow in master for workflow Id {}", workflow.getId());
		WorkflowDTO workflowDTO = workflows.stream().filter(dto -> Objects.equals(dto.getId(), workflow.getId()))
				.findFirst().orElse(null);

		if (null != workflowDTO) {
			workflowDTO.setEnabled(workflow.isEnabled());
			workflowDTO.setPaused(workflow.isPaused());
			workflowDTO.setThrottleLimit(workflow.getThrottleLimit());
			processQueuedInstances(workflow.getId());
		}
	}

	public void updateWorkflowInstance(WorkflowInstance existingInstance) {
		log.info("Updating workflow instance in master when priority updated for instance Id {}",
				existingInstance.getId());
		List<WorkflowInstanceMessageDTO> queuedInstancesForWorkflow = queuedInstances
				.get(existingInstance.getWorkflow().getId());
		if (queuedInstancesForWorkflow != null) {
			WorkflowInstanceMessageDTO instanceMessageDTO = queuedInstancesForWorkflow.stream()
					.filter(instance -> instance.getId().equals(existingInstance.getId())).findFirst().orElse(null);
			if (instanceMessageDTO != null) {
				instanceMessageDTO.setPriority(existingInstance.getPriority());
				Collections.sort(queuedInstancesForWorkflow);
				queuedInstances.put(existingInstance.getWorkflow().getId(), queuedInstancesForWorkflow);
			}
		}

	}

	public void updatePausedProperty(boolean isPaused) {
		this.isPaused = isPaused;
		handlePausedWorkflowInstances(workflows);
	}

	public void addWorkflow(Workflow workflow) {
		log.info("Adding workflow in master for workflow Id {}", workflow.getId());
		WorkflowDTO workflowDTO = new WorkflowDTO();
		workflowDTO.setId(workflow.getId());
		workflowDTO.setThrottleLimit(workflow.getThrottleLimit());
		workflowDTO.setEnabled(workflow.isEnabled());

		if (!queuedInstances.containsKey(workflow.getId())) {
			queuedInstances.put(workflow.getId(), new ArrayList<>());
		}
		if (!runningInstances.containsKey(workflow.getId())) {
			runningInstances.put(workflow.getId(), new ArrayList<>());
		}
		this.workflows.add(workflowDTO);
	}

	public void handlePausedWorkflowInstances(Set<WorkflowDTO> workflows) {

		if (this.isPaused) {
			for (WorkflowDTO workflowDTO : workflows) {
				workflowDTO.setEnabled(false);
			}
			List<WorkflowInstanceStatus> statuses = Arrays.asList(WorkflowInstanceStatus.CREATED);
			List<WorkflowInstance> queuedInstances = workflowInstanceRepository.findByStatusIn(statuses);
			queuedInstances.forEach(instance -> {
				instance.setStatus(WorkflowInstanceStatus.PAUSED);
				workflowInstanceRepository.save(instance);
				log.info("The instance with id {} is paused", instance.getId());

			});
		} else {
			// Updating the status of instances after global pause disabled the workflow
			// based on the previous pause status
			for (WorkflowDTO workflowDTO : workflows) {
				workflowDTO.setEnabled(true);
				if (workflowDTO.paused) {
					workflowDTO.setEnabled(false);
					com.bmg.deliver.dto.WorkflowDTO workflowDTO1 = new com.bmg.deliver.dto.WorkflowDTO();
					workflowDTO1.setPaused(workflowDTO.paused);
					workflowInstanceService.updateWorkflowInstanceStatus(workflowDTO.getId(), workflowDTO1);
				} else {
					workflowDTO.setEnabled(true);
					com.bmg.deliver.dto.WorkflowDTO workflowDTO1 = new com.bmg.deliver.dto.WorkflowDTO();
					workflowDTO1.setPaused(workflowDTO.paused);
					workflowInstanceService.updateWorkflowInstanceStatus(workflowDTO.getId(), workflowDTO1);
					processQueuedInstances(workflowDTO.getId());

				}
			}
		}
	}

	@Scheduled(fixedRate = 10000)
	public void instancesFromTimeIntervalToPushToWorkQueue() {
		for (Map.Entry<Long, List<WorkflowInstanceMessageDTO>> entry : timeIntervalInstances.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				log.info("Processing time interval instances for workflow ID: {} ", entry);
			}
			processWorkflowInstances(entry);
		}
	}

	private void processWorkflowInstances(Map.Entry<Long, List<WorkflowInstanceMessageDTO>> entry) {
		List<WorkflowInstanceMessageDTO> timeIntervalInstancesForWorkflow = entry.getValue();
		List<WorkflowInstanceMessageDTO> processedInstances = new ArrayList<>();

		for (WorkflowInstanceMessageDTO instanceMessageDTO : timeIntervalInstancesForWorkflow) {
			processInstanceMessage(entry.getKey(), instanceMessageDTO, processedInstances);
		}
		timeIntervalInstancesForWorkflow.removeAll(processedInstances);
		timeIntervalInstances.put(entry.getKey(), timeIntervalInstancesForWorkflow);
	}

	private void processInstanceMessage(Long workflowId, WorkflowInstanceMessageDTO instanceMessageDTO,
			List<WorkflowInstanceMessageDTO> processedInstances) {
		WorkflowInstance workflowInstance = workflowInstanceRepository.findById(instanceMessageDTO.getId())
				.orElse(null);

		if (workflowInstance == null) {
			log.warn("Workflow instance not found for ID: {}", instanceMessageDTO.getId());
			processedInstances.add(instanceMessageDTO);
			return;
		}

		long waitTime = calculateWaitTime(workflowInstance);
		String waitTimeHMS = convertMillisToHMS(waitTime);
		log.info("Waiting time for instance ID {}: {}", workflowInstance.getId(), waitTimeHMS);

		if (waitTime <= 0) {
			handleWorkflowInstance(workflowId, instanceMessageDTO);
			processedInstances.add(instanceMessageDTO);
		}
	}
	private static String convertMillisToHMS(long millis) {
		long hours = millis / (1000 * 60 * 60);
		long minutes = millis / (1000 * 60) % 60;
		long seconds = millis / 1000 % 60;
		long milliseconds = millis % 1000;

		return String.format("%02dh %02dm %02ds %03dms", hours, minutes, seconds, milliseconds);
	}

	private long getThrottleLimit(Long workflowId) {
		Workflow workflow = workflowRepository.findById(workflowId).orElse(null);
		if (workflow == null) {
			log.warn("Workflow not found for ID: {}", workflowId);
			return 0;
		} else {
			return workflow.getThrottleLimit();
		}
	}

	@Transactional
	private Long getWaitingTimeBasedOnLastInstance(WorkflowInstance workflowInstance) {

		log.info("Calculating waiting time for WorkflowInstance ID: {}", workflowInstance.getId());

		Optional<WorkflowInstance> previousInstance = getLastInstanceByQuery(workflowInstance);
		if (previousInstance.isEmpty()) {
			log.warn("No previous instance found by instanceId for ID: {}", workflowInstance.getId());
			return 0L;
		}
		WorkflowInstance lastInstance = previousInstance.get();

		if (isSameDeliveryType(lastInstance, workflowInstance)) {
			String assetOrDataIngestionWaitTime = lastInstance.getDeliveryType().equals(DeliveryType.FULL_DELIVERY)
					? workflowInstance.getWorkflow().getAssetIngestionTime()
					: workflowInstance.getWorkflow().getDataIngestionTime();
			return (null == assetOrDataIngestionWaitTime || assetOrDataIngestionWaitTime.isEmpty()
					|| assetOrDataIngestionWaitTime.startsWith("0"))
							? 0L
							: addWaitTimeConfigToLastInstance(lastInstance, assetOrDataIngestionWaitTime);
		} else {
			log.info("Delivery type does not match. Last instance type: {}, Current instance type: {}",
					lastInstance.getDeliveryType(), workflowInstance.getDeliveryType());
		}
		return 0L;
	}

	protected Optional<WorkflowInstance> getLastInstanceByQuery(WorkflowInstance workflowInstance) {
		return workflowInstanceRepository.findLastInstanceByWorkflowIdAndStatusNative(
				workflowInstance.getWorkflow().getId(), workflowInstance.getIdentifier(), workflowInstance.getId());
	}

	private boolean isSameDeliveryType(WorkflowInstance lastInstance, WorkflowInstance currentInstance) {
		return lastInstance.getDeliveryType() == currentInstance.getDeliveryType()
				&& lastInstance.getIdentifier().equals(currentInstance.getIdentifier());
	}

	private Long addWaitTimeConfigToLastInstance(WorkflowInstance lastInstance, String assetOrDataIngestionWaitTime) {

		if (lastInstance.getCompleted() == null) {
			log.info("Completed date is null for instance ID: {}. Returning minimal waiting time.",
					lastInstance.getId());
			return 1L;
		}
		Date completedDate = lastInstance.getCompleted();
		LocalDateTime localDateTime = completedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		long completedTimeMillis = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		long waitTimeMillis = TimeUtils.convertTimeToMillis(assetOrDataIngestionWaitTime);
		return completedTimeMillis + waitTimeMillis;
	}
}