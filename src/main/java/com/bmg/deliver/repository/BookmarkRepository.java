package com.bmg.deliver.repository;

import com.bmg.deliver.model.Bookmark;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

	Optional<Bookmark> findByWorkflowIdAndRemoteUser_Id(Long workflowId, Long userId);

	@Query("SELECT b FROM Bookmark b WHERE b.remoteUser.username = :username")
	List<Bookmark> findBookmarksByUsername(@Param("username") String username);

}
