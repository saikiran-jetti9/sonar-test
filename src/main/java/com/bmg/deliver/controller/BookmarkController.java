package com.bmg.deliver.controller;

import com.bmg.deliver.config.MasterCondition;
import com.bmg.deliver.dto.BookmarkDTO;
import com.bmg.deliver.dto.WorkflowDTO;
import com.bmg.deliver.model.Bookmark;
import com.bmg.deliver.model.RemoteUser;
import com.bmg.deliver.service.BookmarkService;
import com.bmg.deliver.service.RemoteUserService;
import com.bmg.deliver.utils.AppConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bookmark")
@Conditional(MasterCondition.class)
public class BookmarkController {
	@Autowired
	private BookmarkService bookmarkService;

	@Autowired
	private RemoteUserService remoteUserService;

	@PostMapping
	public ResponseEntity<?> createBookmark(@RequestBody BookmarkDTO bookmarkDTO) {
		try {
			Optional<RemoteUser> user = remoteUserService.getUserByUsername(bookmarkDTO.getUserName());
			if (user.isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(AppConstants.ERROR_FETCHING_REMOTE_USER_BY_USERNAME);
			}
			Bookmark createdBookmark = bookmarkService.createBookmark(bookmarkDTO.getWorkflowId(), user.get());
			return ResponseEntity.ok(createdBookmark);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(AppConstants.ERROR_CREATING_BOOKMARK + e.getMessage());
		}
	}

	@DeleteMapping
	public ResponseEntity<?> deleteBookmark(@RequestParam Long workflowId, @RequestParam String username) {
		try {
			Optional<RemoteUser> user = remoteUserService.getUserByUsername(username);
			if (user.isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(AppConstants.ERROR_FETCHING_REMOTE_USER_BY_USERNAME);
			}
			boolean deleted = bookmarkService.deleteBookmark(workflowId, user.get().getId());
			if (deleted) {
				return ResponseEntity.ok(AppConstants.BOOKMARK_DELETED_SUCESSFULLY);
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AppConstants.BOOKMARK_NOT_FOUND);
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(AppConstants.ERROR_DELETING_BOOKMARK + e.getMessage());
		}
	}

	@GetMapping("/user/{username}")
	public List<Bookmark> getBookmarksByUser(@PathVariable String username) {
		return bookmarkService.getBookmarksByUsername(username);
	}

	@GetMapping("/user/{username}/workflows")
	public ResponseEntity<?> getWorkflowsByUsername(@PathVariable String username,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false, defaultValue = "created") String sortBy,
			@RequestParam(required = false, defaultValue = "asc") String order,
			@RequestParam(required = false) Boolean enabled,
			@RequestParam(required = false) @DateTimeFormat(pattern = AppConstants.DATE_FORMAT_FULL_TIMESTAMP) Date startDate,
			@RequestParam(required = false) @DateTimeFormat(pattern = AppConstants.DATE_FORMAT_FULL_TIMESTAMP) Date endDate) {
		try {
			Sort sort = switch (sortBy) {
				case AppConstants.NAME -> AppConstants.ASC.equalsIgnoreCase(order)
						? Sort.by(Sort.Order.asc(AppConstants.NAME).ignoreCase())
						: Sort.by(Sort.Order.desc(AppConstants.NAME).ignoreCase());
				case AppConstants.ENABLED -> AppConstants.ASC.equalsIgnoreCase(order)
						? Sort.by(AppConstants.ENABLED).ascending()
						: Sort.by(AppConstants.ENABLED).descending();
				case AppConstants.CREATED -> AppConstants.ASC.equalsIgnoreCase(order)
						? Sort.by(AppConstants.CREATED).ascending()
						: Sort.by(AppConstants.CREATED).descending();
				default -> Sort.by("created").descending();
			};
			Pageable pageable = PageRequest.of(page, size, sort);
			Page<WorkflowDTO> workflowsPage = bookmarkService.getWorkflowsByUsername(username, pageable, enabled,
					startDate, endDate);
			return ResponseEntity.ok(workflowsPage);
		} catch (Exception e) {
			Page<WorkflowDTO> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
			return ResponseEntity.ok(emptyPage);
		}
	}
}
