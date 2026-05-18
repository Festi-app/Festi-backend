package com.festi.backend.festival;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    List<Notice> findByFestivalIdOrderByCreatedAtDesc(UUID festivalId);
}
