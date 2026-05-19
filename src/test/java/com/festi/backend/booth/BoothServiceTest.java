package com.festi.backend.booth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.festival.Festival;
import com.festi.backend.location.BoothLocation;
import com.festi.backend.location.BoothLocationRepository;
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
class BoothServiceTest {

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private BoothLocationRepository boothLocationRepository;

    private BoothService boothService;

    private Festival festival;

    @BeforeEach
    void setUp() {
        boothService = new BoothService(boothRepository, boothLocationRepository);
        festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
    }

    @Test
    void findBoothsByTypeAndCategoryWhenDayIsAbsent() {
        Booth booth = booth(UUID.randomUUID(), "night booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        when(boothRepository.findByTypeAndCategory(BoothType.NIGHT, BoothCategory.ALCOHOL))
                .thenReturn(List.of(booth));

        List<BoothDTO.Summary> response = boothService.getBooths(null, BoothType.NIGHT, BoothCategory.ALCOHOL);

        assertThat(response).extracting(BoothDTO.Summary::name).containsExactly("night booth");
    }

    @Test
    void filtersPlacedBoothsByDayAndCategory() {
        LocalDate day = LocalDate.of(2026, 5, 20);
        Booth matching = booth(UUID.randomUUID(), "matching", BoothCategory.ALCOHOL, BoothType.NIGHT);
        Booth wrongCategory = booth(UUID.randomUUID(), "wrong", BoothCategory.INFO, BoothType.NIGHT);

        when(boothLocationRepository.findByDayOrderByIndex(day))
                .thenReturn(List.of(
                        location(day, BoothType.NIGHT, matching, (short) 1),
                        location(day, BoothType.NIGHT, wrongCategory, (short) 2)
                ));

        List<BoothDTO.Summary> response = boothService.getBooths(day, null, BoothCategory.ALCOHOL);

        assertThat(response).extracting(BoothDTO.Summary::name).containsExactly("matching");
    }

    @Test
    void returnsBoothDetailAndRejectsMissingBooth() {
        UUID boothId = UUID.randomUUID();
        Booth booth = booth(boothId, "detail booth", BoothCategory.EXPERIENCE, BoothType.DAY);
        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(boothRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000000")))
                .thenReturn(Optional.empty());

        BoothDTO.Detail response = boothService.getBooth(boothId);

        assertThat(response.name()).isEqualTo("detail booth");
        assertThatThrownBy(() -> boothService.getBooth(UUID.fromString("00000000-0000-0000-0000-000000000000")))
                .isInstanceOf(NotFoundException.class);
    }

    private Booth booth(UUID id, String name, BoothCategory category, BoothType type) {
        Booth booth = new Booth(name, category, type, "creator");
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }

    private BoothLocation location(LocalDate day, BoothType type, Booth booth, short index) {
        BoothLocation location = new BoothLocation(festival, type, day, "zone");
        location.assignBooth(booth, index);
        return location;
    }
}
