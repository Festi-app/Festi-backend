package com.festi.backend.waiting;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitingRepository extends JpaRepository<Waiting, UUID> {

    List<Waiting> findByUserId(UUID userId);

    @EntityGraph(attributePaths = "booth")
    List<Waiting> findByUserIdOrderByRegisteredAtDesc(UUID userId);

    List<Waiting> findByBoothIdAndStatusOrderByRegisteredAt(UUID boothId, WaitingStatus status);
}
