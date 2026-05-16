package com.festi.backend.festival;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalRepository extends JpaRepository<Festival, UUID> {
}
