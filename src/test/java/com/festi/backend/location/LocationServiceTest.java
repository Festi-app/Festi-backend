package com.festi.backend.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothType;
import com.festi.backend.user.User;
import java.time.LocalDate;
import java.util.List;
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

    @BeforeEach
    void setUp() {
        locationService = new LocationService(boothLocationRepository);
    }

    @Test
    void returnsPlacedAndUnplacedLocationsInIndexOrder() {
        LocalDate day = LocalDate.of(2026, 5, 20);
        Booth booth = new Booth("booth", BoothCategory.INFO, BoothType.DAY,
                new User("creator@example.com", "hash", "creator", "01012345678"));
        BoothLocation first = new BoothLocation(BoothType.DAY, day, "A");
        BoothLocation second = new BoothLocation(BoothType.DAY, day, "B");
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
