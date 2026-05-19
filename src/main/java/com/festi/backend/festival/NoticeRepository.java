package com.festi.backend.festival;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    @Query("SELECT n FROM Notice n WHERE n.festival.id = :festivalId ORDER BY n.pinned DESC, n.createdAt DESC")
    List<Notice> findByFestivalIdOrderByPinnedDescCreatedAtDesc(@Param("festivalId") UUID festivalId);
}
