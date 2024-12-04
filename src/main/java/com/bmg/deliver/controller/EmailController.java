package com.bmg.deliver.controller;

import com.bmg.deliver.dto.WorkflowEmailDTO;
import com.bmg.deliver.model.WorkflowEmail;
import com.bmg.deliver.service.EmailService;
import com.bmg.deliver.utils.AppConstants;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email")
@Slf4j
public class EmailController {

	private final EmailService emailService;

	@Autowired
	public EmailController(EmailService emailService) {
		this.emailService = emailService;
	}

	@PostMapping("/{id}")
	public ResponseEntity<WorkflowEmail> addEmail(@PathVariable Long id,
			@Valid @RequestBody WorkflowEmailDTO emailDTO) {
		try {
			WorkflowEmail email = emailService.addEmail(id, emailDTO);
			return new ResponseEntity<>(email, HttpStatus.CREATED);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<WorkflowEmail> updateEmail(@PathVariable Long id,
			@Valid @RequestBody WorkflowEmailDTO emailDTO) {
		try {
			WorkflowEmail updatedEmail = emailService.updateEmail(id, emailDTO);
			if (updatedEmail != null) {
				return new ResponseEntity<>(updatedEmail, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/{id}")
	public ResponseEntity<WorkflowEmail> getEmail(@PathVariable Long id) {
		try {
			WorkflowEmail email = emailService.getEmail(id);
			if (email != null) {
				return new ResponseEntity<>(email, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/workflow/{id}")
	public ResponseEntity<List<WorkflowEmailDTO>> getEmailsByWorkflowId(@PathVariable Long id) {
		try {
			List<WorkflowEmailDTO> emails = emailService.getEmailByWorkflowId(id);
			return new ResponseEntity<>(emails, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteEmail(@PathVariable Long id) {
		try {
			boolean isDeleted = emailService.deleteEmail(id);
			if (isDeleted) {
				return ResponseEntity.ok(String.format(AppConstants.EMAIL_DELETED_SUCCESSFULLY, id));
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(String.format(AppConstants.EMAIL_NOT_FOUND, id));
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AppConstants.ERROR_DELETING_EMAIL);
		}
	}

	@GetMapping("/send-email")
	public String sendEmail(@RequestParam String to, @RequestParam String subject, @RequestParam String body) {
		try {
			emailService.sendEmail(to, subject, body);
			return AppConstants.EMAIL_SENT_SUCCESSFULLY;
		} catch (Exception e) {
			return AppConstants.ERROR_SENDING_EMAIL + ExceptionUtils.getStackTrace(e);
		}
	}
}
