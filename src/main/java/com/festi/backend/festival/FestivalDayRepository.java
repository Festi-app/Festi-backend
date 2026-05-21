package com.festi.backend.festival;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalDayRepository extends JpaRepository<FestivalDay, UUID> {

    List<FestivalDay> findByFestivalIdOrderByDay(UUID festivalId);

    Optional<FestivalDay> findByFestivalIdAndDay(UUID festivalId, LocalDate day);
}
