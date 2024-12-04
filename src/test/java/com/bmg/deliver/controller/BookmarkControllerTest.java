package com.bmg.deliver.controller;

import com.bmg.deliver.dto.BookmarkDTO;
import com.bmg.deliver.dto.WorkflowDTO;
import com.bmg.deliver.model.Bookmark;
import com.bmg.deliver.model.RemoteUser;
import com.bmg.deliver.service.BookmarkService;
import com.bmg.deliver.service.RemoteUserService;
import com.bmg.deliver.utils.AppConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.*;
@ExtendWith(MockitoExtension.class)
class BookmarkControllerTest {

	@InjectMocks
	private BookmarkController bookmarkController;

	@Mock
	private BookmarkService bookmarkService;

	@Mock
	private RemoteUserService remoteUserService;

	private BookmarkDTO bookmarkDTO;
	private RemoteUser remoteUser;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
		bookmarkDTO = new BookmarkDTO();
		bookmarkDTO.setUserName("testUser");
		bookmarkDTO.setWorkflowId(123L);

		remoteUser = new RemoteUser();
		remoteUser.setId(1L);
		remoteUser.setUsername("testUser");
	}

	@Test
	void testCreateBookmarkSuccess() {
		Mockito.when(remoteUserService.getUserByUsername(anyString())).thenReturn(Optional.of(remoteUser));
		Mockito.when(bookmarkService.createBookmark(anyLong(), any(RemoteUser.class))).thenReturn(new Bookmark());

		ResponseEntity<?> response = bookmarkController.createBookmark(bookmarkDTO);

		Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
	}

	@Test
	void testCreateBookmarkUserNotFound() {
		Mockito.when(remoteUserService.getUserByUsername(anyString())).thenReturn(Optional.empty());

		ResponseEntity<?> response = bookmarkController.createBookmark(bookmarkDTO);

		Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		Assertions.assertEquals(AppConstants.ERROR_FETCHING_REMOTE_USER_BY_USERNAME, response.getBody());
	}

	@Test
	void testCreateBookmarkException() {
		Mockito.when(remoteUserService.getUserByUsername(anyString()))
				.thenThrow(new RuntimeException("Test Exception"));

		ResponseEntity<?> response = bookmarkController.createBookmark(bookmarkDTO);

		Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		Assertions.assertEquals(AppConstants.ERROR_CREATING_BOOKMARK + "Test Exception", response.getBody());
	}

	@Test
	void testDeleteBookmarkSuccess() {
		Mockito.when(remoteUserService.getUserByUsername(anyString())).thenReturn(Optional.of(remoteUser));
		Mockito.when(bookmarkService.deleteBookmark(anyLong(), anyLong())).thenReturn(true);

		ResponseEntity<?> response = bookmarkController.deleteBookmark(123L, "testUser");

		Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
		Assertions.assertEquals(AppConstants.BOOKMARK_DELETED_SUCESSFULLY, response.getBody());
	}

	@Test
	void testDeleteBookmarkUserNotFound() {
		Mockito.when(remoteUserService.getUserByUsername(anyString())).thenReturn(Optional.empty());

		ResponseEntity<?> response = bookmarkController.deleteBookmark(123L, "testUser");

		Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		Assertions.assertEquals(AppConstants.ERROR_FETCHING_REMOTE_USER_BY_USERNAME, response.getBody());
	}

	@Test
	void testDeleteBookmarkNotFound() {
		Mockito.when(remoteUserService.getUserByUsername(anyString())).thenReturn(Optional.of(remoteUser));
		Mockito.when(bookmarkService.deleteBookmark(anyLong(), anyLong())).thenReturn(false);

		ResponseEntity<?> response = bookmarkController.deleteBookmark(123L, "testUser");

		Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		Assertions.assertEquals(AppConstants.BOOKMARK_NOT_FOUND, response.getBody());
	}

	@Test
	void testDeleteBookmarkException() {
		Mockito.when(remoteUserService.getUserByUsername(anyString()))
				.thenThrow(new RuntimeException("Test Exception"));

		ResponseEntity<?> response = bookmarkController.deleteBookmark(123L, "testUser");

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals(AppConstants.ERROR_DELETING_BOOKMARK + "Test Exception", response.getBody());
	}

	@Test
	void testGetBookmarksByUserSuccess() {
		List<Bookmark> bookmarkList = new ArrayList<>();
		bookmarkList.add(new Bookmark());

		Mockito.when(bookmarkService.getBookmarksByUsername(anyString())).thenReturn(bookmarkList);

		List<Bookmark> response = bookmarkController.getBookmarksByUser("testUser");

		Assertions.assertEquals(1, response.size());
	}

	@Test
	void testGetWorkflowsByUsernameSuccess() {
		Pageable pageable = PageRequest.of(0, 20, Sort.by("created").descending());

		List<WorkflowDTO> workflowList = new ArrayList<>();
		workflowList.add(new WorkflowDTO());
		Page<WorkflowDTO> workflowPage = new PageImpl<>(workflowList, pageable, 1);

		Mockito.when(bookmarkService.getWorkflowsByUsername(anyString(), any(Pageable.class), any(), any(), any()))
				.thenReturn(workflowPage);

		ResponseEntity<?> response = bookmarkController.getWorkflowsByUsername("testUser", 0, 20, "created", "asc",
				null, null, null);

		Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
		Assertions.assertTrue(response.getBody() instanceof Page);
		Assertions.assertEquals(1, ((Page<?>) response.getBody()).getTotalElements());
	}

	@Test
	void testGetWorkflowsByUsernameException() {
		Mockito.when(bookmarkService.getWorkflowsByUsername(anyString(), any(Pageable.class), any(), any(), any()))
				.thenThrow(new RuntimeException("Test Exception"));

		ResponseEntity<?> response = bookmarkController.getWorkflowsByUsername("testUser", 0, 20, "created", "asc",
				null, null, null);

		Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
	}

}
