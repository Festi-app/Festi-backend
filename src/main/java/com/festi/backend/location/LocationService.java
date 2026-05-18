package com.festi.backend.location;

import com.festi.backend.booth.BoothType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LocationService {

    private final BoothLocationRepository boothLocationRepository;

    public LocationService(BoothLocationRepository boothLocationRepository) {
        this.boothLocationRepository = boothLocationRepository;
    }

    public List<LocationDTO.Response> getLocations(LocalDate day, BoothType type) {
        return boothLocationRepository.findByDayAndTypeOrderByIndex(day, type).stream()
                .map(LocationDTO.Response::from)
                .toList();
    }
}
