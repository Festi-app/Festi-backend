package com.festi.backend.booth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothApplicationRepository extends JpaRepository<BoothApplication, UUID> {

    List<BoothApplication> findByFestivalId(UUID festivalId);

    List<BoothApplication> findByFestivalIdAndApplicantId(UUID festivalId, String applicantId);

    List<BoothApplication> findByFestivalIdOrderByCreatedAtDesc(UUID festivalId);

    Optional<BoothApplication> findByIdAndFestivalId(UUID id, UUID festivalId);

    Optional<BoothApplication> findFirstByFestivalIdAndApplicantIdOrderByCreatedAtDesc(UUID festivalId, String applicantId);
}
