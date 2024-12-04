package com.bmg.deliver.service;

import com.bmg.deliver.dto.WorkflowDTO;
import com.bmg.deliver.model.Bookmark;
import com.bmg.deliver.model.RemoteUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;

public interface BookmarkService {
	Bookmark createBookmark(Long id, RemoteUser user);

	boolean deleteBookmark(Long id, Long userId);

	List<Bookmark> getBookmarksByUsername(String username);

	Page<WorkflowDTO> getWorkflowsByUsername(String username, Pageable pageable, Boolean enabled, Date startDate,
			Date endDate);
}
