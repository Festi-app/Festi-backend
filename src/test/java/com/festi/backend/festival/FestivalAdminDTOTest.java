package com.festi.backend.festival;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.booth.BoothType;
import com.festi.backend.location.LocationDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FestivalAdminDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidFestivalAdminRequests() {
        assertThat(validator.validate(new FestivalDTO.UpdateRequest(
                "Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc"))).isEmpty();
        assertThat(validator.validate(new FestivalDayDTO.Request(
                LocalDate.of(2026, 5, 18), LocalTime.of(10, 0), LocalTime.of(17, 0),
                LocalTime.of(18, 0), LocalTime.of(23, 0)))).isEmpty();
        assertThat(validator.validate(new NoticeDTO.Request("Notice", "Content", true))).isEmpty();
        assertThat(validator.validate(new TimelineDTO.Request(
                UUID.randomUUID(), "Stage", "Artist", LocalTime.of(18, 0), LocalTime.of(19, 0)))).isEmpty();
        assertThat(validator.validate(new LocationDTO.SlotsRequest(
                UUID.randomUUID(), BoothType.NIGHT, List.of(new LocationDTO.ZoneSlotsRequest("A", (short) 3))))).isEmpty();
        assertThat(validator.validate(new LocationDTO.AssignmentRequest(UUID.randomUUID()))).isEmpty();
    }

    @Test
    void rejectsBlankAndMissingRequiredFields() {
        assertThat(validator.validate(new FestivalDTO.UpdateRequest(" ", null, null, null))).isNotEmpty();
        assertThat(validator.validate(new FestivalDayDTO.Request(null, null, null, null, null))).isNotEmpty();
        assertThat(validator.validate(new NoticeDTO.Request(" ", " ", false))).isNotEmpty();
        assertThat(validator.validate(new TimelineDTO.Request(null, " ", " ", null, null))).isNotEmpty();
        assertThat(validator.validate(new LocationDTO.SlotsRequest(
                null, null, List.of(new LocationDTO.ZoneSlotsRequest(" ", (short) 0))))).isNotEmpty();
    }
}
