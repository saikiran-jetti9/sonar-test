package com.bmg.deliver.repository;

import com.bmg.deliver.model.Template;
import com.bmg.deliver.model.TemplateVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, Long> {

	@Query(value = "SELECT * FROM template_version tv WHERE tv.template_id = :templateId ORDER BY tv.created DESC LIMIT 20", nativeQuery = true)
	List<TemplateVersion> findLatestByTemplate(@Param("templateId") Long templateId);

	Page<TemplateVersion> findByTemplate(Template template, Pageable pageable);

	void deleteByTemplateId(Long id);
}
