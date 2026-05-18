package com.festi.backend.booth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothRepository extends JpaRepository<Booth, UUID> {

    List<Booth> findByTypeAndCategoryAndIsActiveTrue(BoothType type, BoothCategory category);

    List<Booth> findByTypeAndIsActiveTrue(BoothType type);

    List<Booth> findByCategoryAndIsActiveTrue(BoothCategory category);

    List<Booth> findByIsActiveTrue();

    Optional<Booth> findByIdAndIsActiveTrue(UUID id);
}
