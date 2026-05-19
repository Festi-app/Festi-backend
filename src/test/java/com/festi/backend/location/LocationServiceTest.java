package com.festi.backend.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothType;
import com.festi.backend.festival.Festival;
import java.time.LocalDate;
import java.util.List;
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

    private LocationService locationService;

    private Festival festival;

    @BeforeEach
    void setUp() {
        locationService = new LocationService(boothLocationRepository);
        festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
    }

    @Test
    void returnsPlacedAndUnplacedLocationsInIndexOrder() {
        LocalDate day = LocalDate.of(2026, 5, 20);
        Booth booth = new Booth("booth", BoothCategory.INFO, BoothType.DAY, "creator");
        BoothLocation first = new BoothLocation(festival, BoothType.DAY, day, "A");
        BoothLocation second = new BoothLocation(festival, BoothType.DAY, day, "B");
        first.assignBooth(booth, (short) 1);
        ReflectionTestUtils.setField(first, "id", (short) 1);
        ReflectionTestUtils.setField(second, "id", (short) 2);

        when(boothLocationRepository.findByDayAndTypeOrderByIndex(day, BoothType.DAY))
                .thenReturn(List.of(first, second));

        List<LocationDTO.Response> response = locationService.getLocations(day, BoothType.DAY);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).boothSummary()).isNotNull();
        assertThat(response.get(1).boothSummary()).isNull();
    }
}
