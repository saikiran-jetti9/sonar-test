package com.bmg.deliver.serviceimpl.email;

import com.bmg.deliver.dto.WorkflowEmailDTO;
import com.bmg.deliver.enums.EmailStatus;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.exceptions.EmailNotFoundException;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowEmail;
import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.repository.WorkflowEmailRepository;
import com.bmg.deliver.repository.WorkflowInstanceRepository;
import com.bmg.deliver.repository.WorkflowRepository;
import com.bmg.deliver.service.EmailService;
import com.bmg.deliver.utils.AppConstants;
import com.bmg.deliver.utils.EmailContentBuilder;
import com.sendgrid.*;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

	private final SendGrid sendGrid;

	@Value("${sendgrid.api.fromMail}")
	private String fromMail;

	private final WorkflowEmailRepository workflowEmailRepository;

	private final WorkflowRepository workflowRepository;

	private final WorkflowInstanceRepository workflowInstanceRepository;

	private final EmailContentBuilder emailContentBuilder;

	@Autowired
	public EmailServiceImpl(SendGrid sendGrid, WorkflowEmailRepository workflowEmailRepository,
			WorkflowRepository workflowRepository, WorkflowInstanceRepository workflowInstanceRepository,
			EmailContentBuilder emailContentBuilder) {
		this.sendGrid = sendGrid;
		this.workflowEmailRepository = workflowEmailRepository;
		this.workflowRepository = workflowRepository;
		this.workflowInstanceRepository = workflowInstanceRepository;
		this.emailContentBuilder = emailContentBuilder;
	}

	@Override
	public void sendEmail(String to, String subject, String body) {
		Email from = new Email(fromMail);
		Email toEmail = new Email(to);
		Content content = new Content("text/html", body);
		Mail mail = new Mail();
		Personalization personalization = new Personalization();
		mail.setFrom(from);
		mail.setSubject(subject);
		mail.addPersonalization(personalization);
		mail.addContent(content);
		personalization.addTo(toEmail);
		Request request = new Request();
		try {
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			sendGrid.api(request);
			log.info("Instance Status Mail Sent Successfully!");
		} catch (Exception e) {
			log.error(AppConstants.ERROR_SENDING_EMAIL, e);
		}
	}

	@Override
	public WorkflowEmail addEmail(Long id, WorkflowEmailDTO emailDTO) {
		Optional<Workflow> workflow = workflowRepository.findById(id);
		WorkflowEmail savedEmail = new WorkflowEmail();
		workflow.ifPresent(savedEmail::setWorkflow);
		savedEmail.setEmail(emailDTO.getEmail());
		savedEmail.setName(emailDTO.getName());
		savedEmail.setStatus(EmailStatus.valueOf(emailDTO.getStatus()));
		return workflowEmailRepository.save(savedEmail);
	}

	@Override
	public WorkflowEmail updateEmail(Long id, WorkflowEmailDTO emailDTO) {
		Optional<WorkflowEmail> existingEmail = workflowEmailRepository.findById(id);
		if (existingEmail.isPresent()) {
			WorkflowEmail email = existingEmail.get();
			email.setEmail(emailDTO.getEmail());
			email.setStatus(EmailStatus.valueOf(emailDTO.getStatus()));
			email.setName(emailDTO.getName());
			workflowEmailRepository.save(email);
			return email;
		}
		return null;
	}

	@Override
	public WorkflowEmail getEmail(Long id) {
		return workflowEmailRepository.findById(id).orElse(null);
	}

	@Override
	public List<WorkflowEmailDTO> getEmailByWorkflowId(Long id) {
		List<WorkflowEmail> emails = workflowEmailRepository.findByWorkflowId(id);

		if (emails.isEmpty()) {
			return new ArrayList<>();
		}

		List<WorkflowEmailDTO> emailDTOs = new ArrayList<>();
		for (WorkflowEmail email : emails) {
			WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
			emailDTO.setId(email.getId());
			emailDTO.setEmail(email.getEmail());
			emailDTO.setName(email.getName());
			emailDTO.setStatus(email.getStatus().toString());
			emailDTOs.add(emailDTO);
		}
		return emailDTOs;
	}

	@Override
	public boolean deleteEmail(Long id) {
		try {
			WorkflowEmail email = getEmail(id);
			if (email != null) {
				workflowEmailRepository.deleteById(id);
				return true;
			} else {
				return false;
			}
		} catch (EmailNotFoundException e) {
			throw new EmailNotFoundException(AppConstants.ERROR_DELETING_EMAIL + id);
		}
	}

	@Override
	public void emailNotifier(Long instanceId, Long workflowId) {
		Optional<WorkflowInstance> optionalInstance = workflowInstanceRepository.findById(instanceId);
		if (optionalInstance.isPresent()) {
			WorkflowInstance instance = optionalInstance.get();
			List<WorkflowEmail> workflowEmails = workflowEmailRepository.findByWorkflowId(workflowId);
			if (workflowEmails.isEmpty()) {
				log.info("No emails found for workflow with id: {}", workflowId);
			}
			sendMails(workflowEmails, instance);
		}
	}

	public void sendMails(List<WorkflowEmail> workflowEmails, WorkflowInstance instance) {
		for (WorkflowEmail workflowEmail : workflowEmails) {
			try {
				String recipientName = workflowEmail.getName();
				String workflowName = instance.getWorkflow().getName();
				String workflowCapitalizedName = Character.toUpperCase(workflowName.charAt(0))
						+ workflowName.substring(1);
				String newStatus = null;
				String subject = null;

				WorkflowInstanceStatus instanceStatus = instance.getStatus();
				EmailStatus emailStatus = workflowEmail.getStatus();
				if (instanceStatus.equals(WorkflowInstanceStatus.COMPLETED)
						&& (emailStatus == EmailStatus.SUCCESS || emailStatus == EmailStatus.BOTH)) {
					newStatus = String.valueOf(EmailStatus.SUCCESS);
					subject = String.format(" [%s] BMG Product Delivery For %s: Completed Successfully", newStatus,
							workflowCapitalizedName);
				} else if (instanceStatus.equals(WorkflowInstanceStatus.FAILED)
						&& (emailStatus == EmailStatus.ERROR || emailStatus == EmailStatus.BOTH)) {
					newStatus = String.valueOf(EmailStatus.ERROR);
					subject = String.format(" [%s] BMG Product Delivery For %s: Failed to Complete", newStatus,
							workflowCapitalizedName);
				}
				if (null != newStatus && null != subject) {
					String emailContent = emailContentBuilder.buildEmailContent(recipientName, instance, newStatus);
					sendEmail(workflowEmail.getEmail(), subject, emailContent);
				}
			} catch (Exception e) {
				log.error(AppConstants.ERROR_SENDING_EMAIL, e);
			}
		}
	}
}
