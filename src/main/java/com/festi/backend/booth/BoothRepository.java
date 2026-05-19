package com.festi.backend.booth;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothRepository extends JpaRepository<Booth, UUID> {

    List<Booth> findByTypeAndCategory(BoothType type, BoothCategory category);

    List<Booth> findByType(BoothType type);

    List<Booth> findByCategory(BoothCategory category);
}
