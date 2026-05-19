package com.festi.backend.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UserPK> {

    @Query("SELECT u FROM User u WHERE u.pk.id = :id AND u.pk.festivalId = :festivalId")
    Optional<User> findByIdAndFestivalId(@Param("id") String id, @Param("festivalId") UUID festivalId);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.pk.id = :id AND u.pk.festivalId = :festivalId")
    boolean existsByIdAndFestivalId(@Param("id") String id, @Param("festivalId") UUID festivalId);
}
