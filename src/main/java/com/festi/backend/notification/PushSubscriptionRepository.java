package com.festi.backend.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    @Query("SELECT s FROM PushSubscription s WHERE s.id = :id AND s.user.pk.id = :userId AND s.user.pk.festivalId = :festivalId")
    Optional<PushSubscription> findByIdAndUserIdAndFestivalId(@Param("id") UUID id,
                                                               @Param("userId") String userId,
                                                               @Param("festivalId") UUID festivalId);

    @Query("SELECT s FROM PushSubscription s WHERE s.user.pk.id = :userId AND s.user.pk.festivalId = :festivalId")
    List<PushSubscription> findByUserIdAndFestivalId(@Param("userId") String userId,
                                                      @Param("festivalId") UUID festivalId);
}
