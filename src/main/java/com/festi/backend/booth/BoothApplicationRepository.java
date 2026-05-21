package com.festi.backend.booth;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothApplicationRepository extends JpaRepository<BoothApplication, UUID> {

    List<BoothApplication> findByFestivalId(UUID festivalId);

    List<BoothApplication> findByFestivalIdAndApplicantId(UUID festivalId, String applicantId);
}
