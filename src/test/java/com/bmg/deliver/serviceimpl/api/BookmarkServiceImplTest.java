package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.dto.BookmarkDTO;
import com.bmg.deliver.dto.WorkflowDTO;
import com.bmg.deliver.model.Bookmark;
import com.bmg.deliver.model.RemoteUser;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.repository.BookmarkRepository;
import com.bmg.deliver.repository.WorkflowInstanceRepository;
import com.bmg.deliver.repository.WorkflowRepository;
import com.bmg.deliver.service.RemoteUserService;
import com.bmg.deliver.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceImplTest {

	@InjectMocks
	private BookmarkServiceImpl bookmarkService;

	@Mock
	private BookmarkRepository bookmarkRepository;

	@Mock
	private RemoteUserService remoteUserService;

	@Mock
	private WorkflowInstanceRepository workflowInstanceRepository;

	@Mock
	private WorkflowRepository workflowRepository;

	@Mock
	private WorkflowService workflowService;

	private BookmarkDTO bookmarkDTO;
	private Bookmark bookmark;
	private RemoteUser remoteUser;

	private Workflow workflow;

	@BeforeEach
	void setUp() {
		bookmarkDTO = new BookmarkDTO();
		bookmarkDTO.setUserName("test");
		bookmarkDTO.setWorkflowId(100L);

		workflow = new Workflow();
		workflow.setId(1L);

		remoteUser = new RemoteUser();
		remoteUser.setId(1L);

		bookmark = new Bookmark();
		bookmark.setWorkflowId(bookmarkDTO.getWorkflowId());
		bookmark.setRemoteUser(remoteUser);
	}

	@Test
	void testCreateBookmarkSuccess() {
		when(bookmarkRepository.save(any(Bookmark.class))).thenReturn(bookmark);
		Bookmark result = bookmarkService.createBookmark(100L, remoteUser);
		assertNotNull(result);
		assertEquals(100L, result.getWorkflowId());
		verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
	}
	@Test
	void testDeleteBookmarkSuccess() {
		when(bookmarkRepository.findByWorkflowIdAndRemoteUser_Id(anyLong(), anyLong())).thenReturn(Optional.of(bookmark));
		boolean deleted = bookmarkService.deleteBookmark(100L, 1L);
		assertTrue(deleted);
		verify(bookmarkRepository, times(1)).delete(bookmark);
	}
	@Test
	void testDeleteBookmarkNotFound() {
		when(bookmarkRepository.findByWorkflowIdAndRemoteUser_Id(anyLong(), anyLong())).thenReturn(Optional.empty());
		boolean deleted = bookmarkService.deleteBookmark(100L, 1L);
		assertFalse(deleted);
		verify(bookmarkRepository, never()).delete(any(Bookmark.class));
	}

	@Test
	void testDeleteBookmarkException() {
		when(bookmarkRepository.findByWorkflowIdAndRemoteUser_Id(anyLong(), anyLong())).thenThrow(new RuntimeException("Error deleting bookmark"));
		RuntimeException exception = assertThrows(RuntimeException.class, () -> bookmarkService.deleteBookmark(100L, 1L));
		assertTrue(exception.getMessage().contains("Error deleting bookmark"));
	}

	@Test
	void testGetBookmarksByUsernameSuccess() {
		when(bookmarkRepository.findBookmarksByUsername(anyString())).thenReturn(Arrays.asList(bookmark));
		List<Bookmark> bookmarks = bookmarkService.getBookmarksByUsername("test");
		assertNotNull(bookmarks);
		assertEquals(1, bookmarks.size());
		verify(bookmarkRepository, times(1)).findBookmarksByUsername(anyString());
	}

	@Test
	void testGetWorkflowsByUsernameSuccess() {
		Pageable pageable = PageRequest.of(0, 20);

		Page<Workflow> bookmarkPage = new PageImpl<>(Collections.singletonList(workflow), pageable, 1);

		when(remoteUserService.getUserByUsername(anyString())).thenReturn(Optional.of(remoteUser));
		when(workflowRepository.findWorkflowsByUsername(anyString(), eq(pageable))).thenReturn(bookmarkPage);

		Page<WorkflowDTO> workflows = bookmarkService.getWorkflowsByUsername("test", pageable, null, null, null);

		assertNotNull(workflows);
		assertEquals(1, workflows.getTotalElements());
	}

	@Test
	void testGetWorkflowsByUsernameUserNotFound() {
		Pageable pageable = PageRequest.of(0, 20);

		when(remoteUserService.getUserByUsername(anyString())).thenReturn(Optional.empty());

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> bookmarkService.getWorkflowsByUsername("test", pageable, null, null, null));

		assertEquals("Remote user not found by username", exception.getMessage());
	}

	@Test
	void testGetWorkflowsByUsernameNoBookmarksFound() {
		Pageable pageable = PageRequest.of(0, 20);

		Page<Workflow> emptyBookmarkPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

		when(remoteUserService.getUserByUsername(anyString())).thenReturn(Optional.of(remoteUser));
		when(workflowRepository.findWorkflowsByUsername(anyString(), eq(pageable))).thenReturn(emptyBookmarkPage);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> bookmarkService.getWorkflowsByUsername("test", pageable, null, null, null));

		assertEquals("No workflows found for user", exception.getMessage());
	}
}
