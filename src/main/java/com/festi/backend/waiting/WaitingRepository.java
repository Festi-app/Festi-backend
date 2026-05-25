package com.festi.backend.waiting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaitingRepository extends JpaRepository<Waiting, UUID> {

    @Query("SELECT w FROM Waiting w WHERE w.user.pk.id = :userId AND w.user.pk.festivalId = :festivalId")
    List<Waiting> findByUserIdAndFestivalId(@Param("userId") String userId, @Param("festivalId") UUID festivalId);

    @EntityGraph(attributePaths = "booth")
    @Query("SELECT w FROM Waiting w WHERE w.user.pk.id = :userId AND w.user.pk.festivalId = :festivalId ORDER BY w.registeredAt DESC")
    List<Waiting> findByUserIdAndFestivalIdOrderByRegisteredAtDesc(@Param("userId") String userId, @Param("festivalId") UUID festivalId);

    List<Waiting> findByBoothIdAndStatusOrderByRegisteredAt(UUID boothId, WaitingStatus status);

    @EntityGraph(attributePaths = "booth")
    List<Waiting> findByBoothIdAndStatusInOrderByRegisteredAtAsc(UUID boothId, List<WaitingStatus> statuses);

    @Query("SELECT COUNT(w) FROM Waiting w WHERE w.user.pk.id = :userId AND w.user.pk.festivalId = :festivalId AND w.status IN :statuses")
    long countByUserIdAndFestivalIdAndStatusIn(@Param("userId") String userId, @Param("festivalId") UUID festivalId, @Param("statuses") List<WaitingStatus> statuses);

    @Query("SELECT COUNT(w) > 0 FROM Waiting w WHERE w.booth.id = :boothId AND w.user.pk.id = :userId AND w.user.pk.festivalId = :festivalId AND w.status IN :statuses")
    boolean existsByBoothIdAndUserIdAndFestivalIdAndStatusIn(@Param("boothId") UUID boothId, @Param("userId") String userId,
                                                               @Param("festivalId") UUID festivalId, @Param("statuses") List<WaitingStatus> statuses);

    @Query("SELECT COUNT(w) FROM Waiting w WHERE w.booth.id = :boothId AND w.status = 'WAITING' AND w.registeredAt < :registeredAt")
    long countByBoothIdAndStatusWaitingBeforeRegisteredAt(@Param("boothId") UUID boothId,
                                                          @Param("registeredAt") java.time.OffsetDateTime registeredAt);

    @Query("SELECT COUNT(w) FROM Waiting w WHERE w.booth.id = :boothId AND w.status IN :statuses AND w.registeredAt < (SELECT MIN(w2.registeredAt) FROM Waiting w2 WHERE w2.booth.id = :boothId AND w2.status = :calledStatus)")
    Optional<Long> countActiveBeforeFirstCalled(@Param("boothId") UUID boothId,
                                                @Param("statuses") List<WaitingStatus> statuses,
                                                @Param("calledStatus") WaitingStatus calledStatus);
}
