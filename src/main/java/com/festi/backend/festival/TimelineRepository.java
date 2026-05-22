package com.festi.backend.festival;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimelineRepository extends JpaRepository<Timeline, UUID> {

    List<Timeline> findByFestivalIdOrderByDayAscStartTimeAsc(UUID festivalId);

    Optional<Timeline> findByIdAndFestivalId(UUID id, UUID festivalId);
}
