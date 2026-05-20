package com.festi.backend.location;

import com.festi.backend.booth.BoothType;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalDay;
import com.festi.backend.festival.FestivalDayRepository;
import com.festi.backend.festival.FestivalRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LocationService {

    private final BoothLocationRepository boothLocationRepository;
    private final FestivalRepository festivalRepository;
    private final FestivalDayRepository festivalDayRepository;

    public LocationService(BoothLocationRepository boothLocationRepository,
                           FestivalRepository festivalRepository,
                           FestivalDayRepository festivalDayRepository) {
        this.boothLocationRepository = boothLocationRepository;
        this.festivalRepository = festivalRepository;
        this.festivalDayRepository = festivalDayRepository;
    }

    public List<LocationDTO.Response> getLocations(LocalDate day, BoothType type) {
        Festival festival = festivalRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Festival not found."));
        FestivalDay festivalDay = festivalDayRepository.findByFestivalIdAndDay(festival.getId(), day)
                .orElseThrow(() -> new NotFoundException("Festival day not found."));
        return boothLocationRepository.findByDayAndTypeOrderByIndex(festivalDay, type).stream()
                .map(LocationDTO.Response::from)
                .toList();
    }
}
