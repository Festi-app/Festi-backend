package com.festi.backend.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.booth.BoothType;
import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalDay;
import com.festi.backend.festival.FestivalDayDTO;
import com.festi.backend.festival.FestivalDayRepository;
import com.festi.backend.festival.FestivalRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private BoothLocationRepository boothLocationRepository;

    @Mock
    private FestivalRepository festivalRepository;

    @Mock
    private FestivalDayRepository festivalDayRepository;

    @Mock
    private BoothRepository boothRepository;

    private LocationService locationService;

    private Festival festival;

    @BeforeEach
    void setUp() {
        locationService = new LocationService(boothLocationRepository, festivalRepository, festivalDayRepository, boothRepository);
        festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
    }

    @Test
    void returnsPlacedAndUnplacedLocationsInIndexOrder() {
        LocalDate day = LocalDate.of(2026, 5, 20);
        FestivalDay festivalDay = new FestivalDay(festival, day);
        ReflectionTestUtils.setField(festivalDay, "id", UUID.randomUUID());

        Booth booth = new Booth("booth", BoothCategory.INFO, BoothType.DAY);
        BoothLocation first = new BoothLocation(festival, BoothType.DAY, festivalDay, "A");
        BoothLocation second = new BoothLocation(festival, BoothType.DAY, festivalDay, "B");
        first.assignBooth(booth, (short) 1);
        ReflectionTestUtils.setField(first, "id", (short) 1);
        ReflectionTestUtils.setField(second, "id", (short) 2);

        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(festivalDayRepository.findByFestivalIdAndDay(festival.getId(), day))
                .thenReturn(Optional.of(festivalDay));
        when(boothLocationRepository.findByDayAndTypeOrderByIndex(festivalDay, BoothType.DAY))
                .thenReturn(List.of(first, second));

        List<LocationDTO.Response> response = locationService.getLocations(day, BoothType.DAY);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).boothSummary()).isNotNull();
        assertThat(response.get(1).boothSummary()).isNull();
    }

    @Test
    void createsSlotsFromZoneCounts() {
        UUID dayId = UUID.randomUUID();
        FestivalDay festivalDay = festivalDay(dayId);
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(festivalDayRepository.findByIdAndFestivalId(dayId, festival.getId()))
                .thenReturn(Optional.of(festivalDay));
        when(boothLocationRepository.existsByFestivalIdAndDayAndZoneLabelAndIndex(
                festival.getId(), festivalDay, "A", (short) 1)).thenReturn(false);
        when(boothLocationRepository.existsByFestivalIdAndDayAndZoneLabelAndIndex(
                festival.getId(), festivalDay, "A", (short) 2)).thenReturn(false);
        when(boothLocationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LocationDTO.Response> response = locationService.createSlots(new LocationDTO.SlotsRequest(
                dayId,
                BoothType.NIGHT,
                List.of(new LocationDTO.ZoneSlotsRequest("A", (short) 2))
        ));

        assertThat(response).hasSize(2);
        assertThat(response).extracting(LocationDTO.Response::index).containsExactly((short) 1, (short) 2);
    }

    @Test
    void rejectsDuplicateSlotCreation() {
        UUID dayId = UUID.randomUUID();
        FestivalDay festivalDay = festivalDay(dayId);
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(festivalDayRepository.findByIdAndFestivalId(dayId, festival.getId()))
                .thenReturn(Optional.of(festivalDay));
        when(boothLocationRepository.existsByFestivalIdAndDayAndZoneLabelAndIndex(
                festival.getId(), festivalDay, "A", (short) 1)).thenReturn(true);

        assertThatThrownBy(() -> locationService.createSlots(new LocationDTO.SlotsRequest(
                dayId,
                BoothType.NIGHT,
                List.of(new LocationDTO.ZoneSlotsRequest("A", (short) 2))
        ))).isInstanceOf(ConflictException.class);
    }

    @Test
    void assignsAndRemovesBoothWithoutClearingSlotIndex() {
        short locationId = 1;
        UUID boothId = UUID.randomUUID();
        FestivalDay festivalDay = festivalDay(UUID.randomUUID());
        BoothLocation location = new BoothLocation(festival, BoothType.NIGHT, festivalDay, "A", (short) 1);
        ReflectionTestUtils.setField(location, "id", locationId);
        Booth booth = new Booth("booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        ReflectionTestUtils.setField(booth, "id", boothId);
        when(boothLocationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));

        LocationDTO.Response assigned = locationService.assignBooth(locationId, new LocationDTO.AssignmentRequest(boothId));
        locationService.removeAssignment(locationId);

        assertThat(assigned.boothSummary()).isNotNull();
        assertThat(location.getBooth()).isNull();
        assertThat(location.getIndex()).isEqualTo((short) 1);
    }

    @Test
    void rejectsAssigningAlreadyAssignedSlot() {
        short locationId = 1;
        UUID boothId = UUID.randomUUID();
        FestivalDay festivalDay = festivalDay(UUID.randomUUID());
        BoothLocation location = new BoothLocation(festival, BoothType.NIGHT, festivalDay, "A", (short) 1);
        location.assignBooth(new Booth("old", BoothCategory.INFO, BoothType.NIGHT));
        when(boothLocationRepository.findById(locationId)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> locationService.assignBooth(locationId, new LocationDTO.AssignmentRequest(boothId)))
                .isInstanceOf(ConflictException.class);
    }

    private FestivalDay festivalDay(UUID id) {
        FestivalDay festivalDay = new FestivalDay(festival, LocalDate.of(2026, 5, 20));
        ReflectionTestUtils.setField(festivalDay, "id", id);
        return festivalDay;
    }
}
