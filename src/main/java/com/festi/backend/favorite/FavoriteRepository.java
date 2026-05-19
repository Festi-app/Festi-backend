package com.festi.backend.favorite;

import com.festi.backend.booth.BoothType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    List<Favorite> findByFestivalIdAndUserIdOrderByCreatedAtAsc(UUID festivalId, String userId);

    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.festival.id = :festivalId AND f.userId = :userId AND f.booth.type = :boothType")
    long countByFestivalIdAndUserIdAndBoothType(@Param("festivalId") UUID festivalId,
                                                @Param("userId") String userId,
                                                @Param("boothType") BoothType boothType);

    boolean existsByFestivalIdAndUserIdAndBoothId(UUID festivalId, String userId, UUID boothId);
}
