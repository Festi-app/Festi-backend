package com.festi.backend.waiting;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaitingRepository extends JpaRepository<Waiting, UUID> {

    @Query("SELECT w FROM Waiting w WHERE w.userId = :userId AND w.userFestivalId = :festivalId")
    List<Waiting> findByUserIdAndFestivalId(@Param("userId") String userId, @Param("festivalId") UUID festivalId);

    @EntityGraph(attributePaths = "booth")
    @Query("SELECT w FROM Waiting w WHERE w.userId = :userId AND w.userFestivalId = :festivalId ORDER BY w.registeredAt DESC")
    List<Waiting> findByUserIdAndFestivalIdOrderByRegisteredAtDesc(@Param("userId") String userId, @Param("festivalId") UUID festivalId);

    List<Waiting> findByBoothIdAndStatusOrderByRegisteredAt(UUID boothId, WaitingStatus status);
}
