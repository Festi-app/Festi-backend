package com.festi.backend.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothType;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalDay;
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

    private LocationService locationService;

    private Festival festival;

    @BeforeEach
    void setUp() {
        locationService = new LocationService(boothLocationRepository, festivalRepository, festivalDayRepository);
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
}
