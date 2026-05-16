package com.festi.backend.booth;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothAdminAssignmentRepository extends JpaRepository<BoothAdminAssignment, UUID> {

    boolean existsByBoothIdAndUserId(UUID boothId, UUID userId);
}
