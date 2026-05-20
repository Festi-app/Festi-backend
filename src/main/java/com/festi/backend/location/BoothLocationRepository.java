package com.festi.backend.location;

import com.festi.backend.booth.BoothType;
import com.festi.backend.festival.FestivalDay;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothLocationRepository extends JpaRepository<BoothLocation, Short> {

    @EntityGraph(attributePaths = "booth")
    List<BoothLocation> findByDayAndTypeOrderByIndex(FestivalDay day, BoothType type);

    @EntityGraph(attributePaths = "booth")
    List<BoothLocation> findByDayOrderByIndex(FestivalDay day);
}
