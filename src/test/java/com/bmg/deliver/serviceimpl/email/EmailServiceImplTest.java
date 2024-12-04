package com.bmg.deliver.serviceimpl.email;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.bmg.deliver.dto.WorkflowEmailDTO;
import com.bmg.deliver.enums.EmailStatus;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowEmail;
import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.repository.WorkflowEmailRepository;
import com.bmg.deliver.repository.WorkflowInstanceRepository;
import com.bmg.deliver.repository.WorkflowRepository;
import com.bmg.deliver.utils.EmailContentBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sendgrid.SendGrid;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

	@InjectMocks
	private EmailServiceImpl emailService;

	@Mock
	private WorkflowEmailRepository workflowEmailRepository;

	@Mock
	private WorkflowInstanceRepository workflowInstanceRepository;

	@Mock
	private SendGrid sendGrid;

	@Mock
	private WorkflowRepository workflowRepository;

	@Mock
	private EmailContentBuilder emailContentBuilder;

	private Gson gson = new Gson();

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		ReflectionTestUtils.setField(emailService, "workflowEmailRepository", workflowEmailRepository);
		ReflectionTestUtils.setField(emailService, "workflowInstanceRepository", workflowInstanceRepository);

	}

	@Test
	void testSendEmailSuccess() {
		String to = "recipient@example.com";
		String subject = "Test Subject";
		String body = "<html><body><h1>Hello!</h1><p>This is a test email.</p></body></html>";
		emailService.sendEmail(to, subject, body);
	}

	@Test
	void addEmail() {
		WorkflowEmail workflowEmail = new WorkflowEmail();
		workflowEmail.setEmail("test@gmail.com");
		workflowEmail.setStatus(EmailStatus.SUCCESS);
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));
		Workflow workflow = new Workflow();
		workflow.setId(1L);
		when(workflowEmailRepository.save(any(WorkflowEmail.class))).thenReturn(workflowEmail);
		WorkflowEmail result = emailService.addEmail(1L, emailDTO);

		assertNotNull(result);
		assertEquals("test@gmail.com", result.getEmail());
		assertEquals(EmailStatus.SUCCESS, result.getStatus());
	}

	@Test
	void testSendStatusMailForMatchedEmailsWhenStatusIsSuccess() {
		WorkflowEmail workflowEmail = new WorkflowEmail();
		workflowEmail.setEmail("test@example.com");
		List<WorkflowEmail> emails = new ArrayList<>();
		emails.add(workflowEmail);
		WorkflowInstance instance = new WorkflowInstance();
		instance.setId(1L);
		instance.setStatus(WorkflowInstanceStatus.COMPLETED);
		JsonObject jsonData = new JsonObject();
		instance.setTriggerData(jsonData.toString());
		emailService.sendMails(emails, instance);
	}

	@Test
	void testSendStatusMailForMatchedEmailsWhenStatusIsError() {
		WorkflowEmail workflowEmail = new WorkflowEmail();
		workflowEmail.setEmail("test@example.com");
		List<WorkflowEmail> emails = new ArrayList<>();
		emails.add(workflowEmail);
		WorkflowInstance instance = new WorkflowInstance();
		instance.setId(1L);
		instance.setStatus(WorkflowInstanceStatus.FAILED);
		JsonObject jsonData = new JsonObject();
		instance.setTriggerData(jsonData.toString());
		emailService.sendMails(emails, instance);
	}

	@Test
	void testGetEmailByWorkflowIdWithEmails() {
		WorkflowEmail email1 = new WorkflowEmail();
		email1.setId(1L);
		email1.setEmail("test@bmg.com");
		email1.setWorkflow(new Workflow());
		email1.setStatus(EmailStatus.SUCCESS);

		WorkflowEmail email2 = new WorkflowEmail();
		email2.setId(2L);
		email2.setEmail("test-fail@bmg.com");
		email2.setWorkflow(new Workflow());
		email2.setStatus(EmailStatus.FAILURE);

		when(workflowEmailRepository.findByWorkflowId(anyLong())).thenReturn(List.of(email1, email2));
		List<WorkflowEmailDTO> result = emailService.getEmailByWorkflowId(1L);
		assertEquals(2, result.size());
	}

	@Test
	void testUpdateEmailReturnNull() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));
		WorkflowEmail email = emailService.updateEmail(1L, emailDTO);
		assertNull(email);
	}

	@Test
	void testUpdateEmailReturnEmail() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));
		WorkflowEmail email = new WorkflowEmail();
		email.setId(1L);
		email.setEmail("test@gmail.com");
		when(workflowEmailRepository.findById(anyLong())).thenReturn(Optional.of(email));
		WorkflowEmail result = emailService.updateEmail(1L, emailDTO);
		assertEquals(email, result);
	}

	@Test
	void testGetEmailByWorkflowIdEmpty() {
		List<WorkflowEmail> emails = new ArrayList<>();
		when(workflowEmailRepository.findByWorkflowId(anyLong())).thenReturn(emails);
		List<WorkflowEmailDTO> result = emailService.getEmailByWorkflowId(1L);
		assertNotNull(result);
	}
	@Test
	void testDeleteEmailFalse() {
		boolean isDeleted = emailService.deleteEmail(1L);
		assertEquals(false, isDeleted);
	}

	@Test
	void testDeleteEmailTrue() {
		WorkflowEmail email = new WorkflowEmail();
		email.setId(1L);
		email.setEmail("test@gmail.com");
		when(workflowEmailRepository.findById(anyLong())).thenReturn(Optional.of(email));
		boolean isDeleted = emailService.deleteEmail(1L);
		assertEquals(true, isDeleted);
	}

	@Test
	void testEmailNotifier() throws IOException {
		Workflow workflow = new Workflow();
		workflow.setId(1L);
		workflow.setName("test");
		WorkflowInstance instance = new WorkflowInstance();
		instance.setId(1L);
		instance.setStatus(WorkflowInstanceStatus.valueOf("COMPLETED"));
		JsonObject jsonObject;
		try (FileReader reader = new FileReader("src/test/resources/ddexstep/product.json")) {
			jsonObject = gson.fromJson(reader, JsonObject.class);
		}
		instance.setTriggerData(jsonObject.toString());
		instance.setWorkflow(workflow);
		when(workflowInstanceRepository.findById(anyLong())).thenReturn(Optional.of(instance));
		emailService.emailNotifier(1L, 1L);
		assertEquals(WorkflowInstanceStatus.COMPLETED, instance.getStatus());
	}

	@Test
	void testSendEmailsForSuccess() {
		WorkflowEmail email1 = new WorkflowEmail();
		email1.setId(1L);
		email1.setEmail("test@bmg.com");
		email1.setWorkflow(new Workflow());
		email1.setStatus(EmailStatus.SUCCESS);

		List<WorkflowEmail> emails = new ArrayList<>();
		emails.add(email1);

		WorkflowInstance instance = new WorkflowInstance();
		instance.setId(1L);
		instance.setStatus(WorkflowInstanceStatus.COMPLETED);
		JsonObject jsonData = new JsonObject();
		instance.setTriggerData(jsonData.toString());

		Workflow workflow = new Workflow();
		workflow.setId(1L);
		workflow.setName("test");

		instance.setWorkflow(workflow);

		emailService.sendMails(emails, instance);
	}

	@Test
	void testSendEmailsForError() throws IOException {
		WorkflowEmail email1 = new WorkflowEmail();
		email1.setId(1L);
		email1.setEmail("test@bmg.com");
		email1.setWorkflow(new Workflow());
		email1.setStatus(EmailStatus.FAILURE);

		List<WorkflowEmail> emails = new ArrayList<>();
		emails.add(email1);

		WorkflowInstance instance = new WorkflowInstance();
		instance.setId(1L);
		instance.setStatus(WorkflowInstanceStatus.FAILED);
		JsonObject jsonData = new JsonObject();
		instance.setTriggerData(jsonData.toString());

		Workflow workflow = new Workflow();
		workflow.setId(1L);
		workflow.setName("test");

		instance.setWorkflow(workflow);
		emailService.sendMails(emails, instance);
	}
}
