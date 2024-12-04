package com.bmg.deliver.controller;

import static junit.framework.TestCase.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.bmg.deliver.dto.WorkflowEmailDTO;
import com.bmg.deliver.enums.EmailStatus;
import com.bmg.deliver.model.WorkflowEmail;
import com.bmg.deliver.service.EmailService;
import com.bmg.deliver.utils.AppConstants;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

	@InjectMocks
	private EmailController emailController;

	@Mock
	private EmailService emailService;

	private List<WorkflowEmailDTO> emails;

	@BeforeEach
	void setup() {
		ReflectionTestUtils.setField(emailController, "emailService", emailService);

		emails = new ArrayList<>();
		WorkflowEmailDTO email = new WorkflowEmailDTO();
		emails.add(email);
	}

	@Test
	void testAddEmailSuccess() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));
		WorkflowEmail email = new WorkflowEmail();
		email.setId(1L);
		email.setEmail("test@gmail.com");
		email.setStatus(EmailStatus.SUCCESS);
		when(emailService.addEmail(anyLong(), any(WorkflowEmailDTO.class))).thenReturn(email);
		ResponseEntity<WorkflowEmail> response = emailController.addEmail(1L, emailDTO);
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals(email, response.getBody());
	}

	@Test
	void testAddEmailException() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));

		when(emailService.addEmail(anyLong(), any(WorkflowEmailDTO.class)))
				.thenThrow(new RuntimeException("Error adding email"));

		ResponseEntity<WorkflowEmail> response = emailController.addEmail(1L, emailDTO);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals(null, response.getBody());
	}

	@Test
	void testSendEmailSuccess() {
		String to = "recipient@example.com";
		String subject = "Subject";
		String body = "Content";
		doNothing().when(emailService).sendEmail(to, subject, body);
		String result = emailController.sendEmail(to, subject, body);
		assertEquals("Email sent successfully", result);
	}

	@Test
	void testSendEmailException() {
		String to = "recipient@example.com";
		String subject = "Subject";
		String body = "Content";
		RuntimeException exception = new RuntimeException("Error sending email");
		doThrow(exception).when(emailService).sendEmail(to, subject, body);
		String result = emailController.sendEmail(to, subject, body);
		String expectedMessage = AppConstants.ERROR_SENDING_EMAIL + ExceptionUtils.getStackTrace(exception);
		assertEquals(expectedMessage, result);
	}

	@Test
	void testUpdateEmailSuccess() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));
		WorkflowEmail email = new WorkflowEmail();
		email.setId(1L);
		email.setEmail("test@gmail.com");
		email.setStatus(EmailStatus.SUCCESS);
		when(emailService.updateEmail(anyLong(), any(WorkflowEmailDTO.class))).thenReturn(email);
		ResponseEntity<WorkflowEmail> response = emailController.updateEmail(1L, emailDTO);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(email, response.getBody());
	}

	@Test
	void testUpdateEmailException() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));

		when(emailService.updateEmail(anyLong(), any(WorkflowEmailDTO.class)))
				.thenThrow(new RuntimeException("Error updating email"));

		ResponseEntity<WorkflowEmail> response = emailController.updateEmail(1L, emailDTO);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals(null, response.getBody());
	}

	@Test
	void testUpdateEmailNotFound() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));
		WorkflowEmail email = new WorkflowEmail();
		email.setId(1L);
		email.setEmail("test@gmail.com");
		email.setStatus(EmailStatus.SUCCESS);
		when(emailService.updateEmail(anyLong(), any(WorkflowEmailDTO.class))).thenReturn(null);
		ResponseEntity<WorkflowEmail> response = emailController.updateEmail(1L, emailDTO);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals(null, response.getBody());
	}

	@Test
	void testGetEmailSuccess() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));
		WorkflowEmail email = new WorkflowEmail();
		email.setId(1L);
		email.setEmail("test@gmail.com");
		email.setStatus(EmailStatus.SUCCESS);
		when(emailService.getEmail(anyLong())).thenReturn(email);
		ResponseEntity<WorkflowEmail> response = emailController.getEmail(1L);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(email, response.getBody());
	}

	@Test
	void testGetEmailNotFound() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));
		WorkflowEmail email = new WorkflowEmail();
		email.setId(1L);
		email.setEmail("test@gmail.com");
		email.setStatus(EmailStatus.SUCCESS);
		when(emailService.getEmail(anyLong())).thenReturn(null);
		ResponseEntity<WorkflowEmail> response = emailController.getEmail(1L);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals(null, response.getBody());
	}

	@Test
	void testGetEmailException() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));

		when(emailService.getEmail(anyLong())).thenThrow(new RuntimeException("Error getting email"));

		ResponseEntity<WorkflowEmail> response = emailController.getEmail(1L);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals(null, response.getBody());
	}

	@Test
	void testGetEmailsByWorkflowIdSuccess() {
		Long id = 1L;
		when(emailService.getEmailByWorkflowId(id)).thenReturn(emails);
		ResponseEntity<List<WorkflowEmailDTO>> response = emailController.getEmailsByWorkflowId(id);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(emails, response.getBody());
		verify(emailService, times(1)).getEmailByWorkflowId(id);
	}

	@Test
	void testGetEmailsByWorkflowIdInternalError() {
		Long id = 1L;
		when(emailService.getEmailByWorkflowId(id)).thenThrow(new RuntimeException("Unexpected error"));
		ResponseEntity<List<WorkflowEmailDTO>> response = emailController.getEmailsByWorkflowId(id);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNull(response.getBody());
		verify(emailService, times(1)).getEmailByWorkflowId(id);
	}

	@Test
	void testDeleteEmailSuccess() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));
		WorkflowEmail email = new WorkflowEmail();
		email.setId(1L);
		email.setEmail("test@gmail.com");
		email.setStatus(EmailStatus.SUCCESS);
		when(emailService.deleteEmail(anyLong())).thenReturn(true);
		ResponseEntity<String> response = emailController.deleteEmail(1L);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(AppConstants.EMAIL_DELETED_SUCCESSFULLY, response.getBody());
	}

	@Test
	void testDeleteEmailException() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));

		when(emailService.deleteEmail(anyLong())).thenThrow(new RuntimeException("Error deleting email"));

		ResponseEntity<String> response = emailController.deleteEmail(1L);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals(AppConstants.ERROR_DELETING_EMAIL, response.getBody());
	}

	@Test
	void testDeleteEmailNotFound() {
		WorkflowEmailDTO emailDTO = new WorkflowEmailDTO();
		emailDTO.setEmail("test@gmail.com");
		emailDTO.setStatus(String.valueOf(EmailStatus.SUCCESS));
		WorkflowEmail email = new WorkflowEmail();
		email.setId(1L);
		email.setEmail("test@gmail.com");
		email.setStatus(EmailStatus.SUCCESS);
		when(emailService.deleteEmail(anyLong())).thenReturn(false);
		ResponseEntity<String> response = emailController.deleteEmail(1L);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals(AppConstants.EMAIL_NOT_FOUND, response.getBody());
	}

}
