package com.festi.backend.favorite;

import com.festi.backend.booth.BoothType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    @EntityGraph(attributePaths = "booth")
    List<Favorite> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndBoothId(UUID userId, UUID boothId);

    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.user.id = :userId AND f.booth.type = :boothType")
    long countByUserIdAndBoothType(@Param("userId") UUID userId, @Param("boothType") BoothType boothType);
}
