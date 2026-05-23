package com.festi.backend.location;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.booth.BoothType;
import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalDay;
import com.festi.backend.festival.FestivalDayRepository;
import com.festi.backend.festival.FestivalRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LocationService {

    private final BoothLocationRepository boothLocationRepository;
    private final FestivalRepository festivalRepository;
    private final FestivalDayRepository festivalDayRepository;
    private final BoothRepository boothRepository;

    public List<LocationDTO.Response> getLocations(LocalDate day, BoothType type) {
        Festival festival = findSingleFestival();
        FestivalDay festivalDay = festivalDayRepository.findByFestivalIdAndDay(festival.getId(), day)
                .orElseThrow(() -> new NotFoundException("Festival day not found."));
        return boothLocationRepository.findByDayAndTypeOrderByIndex(festivalDay, type).stream()
                .map(LocationDTO.Response::from)
                .toList();
    }

    @Transactional
    public List<LocationDTO.Response> createSlots(LocationDTO.SlotsRequest request) {
        Festival festival = findSingleFestival();
        FestivalDay festivalDay = festivalDayRepository.findByIdAndFestivalId(request.festivalDayId(), festival.getId())
                .orElseThrow(() -> new NotFoundException("Festival day not found."));

        List<BoothLocation> locations = new ArrayList<>();
        for (LocationDTO.ZoneSlotsRequest zone : request.zones()) {
            for (short index = 1; index <= zone.count(); index++) {
                if (boothLocationRepository.existsByFestivalIdAndDayAndZoneLabelAndIndex(
                        festival.getId(), festivalDay, zone.zoneLabel(), index)) {
                    throw new ConflictException("Location slot already exists.");
                }
                locations.add(new BoothLocation(festival, request.type(), festivalDay, zone.zoneLabel(), index));
            }
        }

        return boothLocationRepository.saveAll(locations).stream()
                .map(LocationDTO.Response::from)
                .toList();
    }

    @Transactional
    public LocationDTO.Response assignBooth(short locationId, LocationDTO.AssignmentRequest request) {
        BoothLocation location = findLocation(locationId);
        if (location.getBooth() != null) {
            throw new ConflictException("Location slot is already assigned.");
        }

        Booth booth = boothRepository.findById(request.boothId())
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        location.assignBooth(booth);
        return LocationDTO.Response.from(location);
    }

    @Transactional
    public void removeAssignment(short locationId) {
        findLocation(locationId).removeBooth();
    }

    private BoothLocation findLocation(short locationId) {
        return boothLocationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location slot not found."));
    }

    private Festival findSingleFestival() {
        return festivalRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Festival not found."));
    }
}
