package com.festi.backend.location;

import com.festi.backend.booth.BoothType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothLocationRepository extends JpaRepository<BoothLocation, Short> {

    @EntityGraph(attributePaths = "booth")
    List<BoothLocation> findByDayAndTypeOrderByIndex(LocalDate day, BoothType type);

    @EntityGraph(attributePaths = "booth")
    List<BoothLocation> findByDayOrderByIndex(LocalDate day);
}
