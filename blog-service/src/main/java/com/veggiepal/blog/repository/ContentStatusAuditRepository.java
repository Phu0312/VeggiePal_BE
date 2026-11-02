package com.veggiepal.blog.repository;

import com.veggiepal.blog.entity.ContentStatusAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentStatusAuditRepository extends JpaRepository<ContentStatusAudit, Long> {
}
