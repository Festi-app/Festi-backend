package com.festi.backend.festival;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.festi.backend.common.exception.NotFoundException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalServiceTest {

    @Mock
    private FestivalRepository festivalRepository;

    @Mock
    private NoticeRepository noticeRepository;

    private FestivalService festivalService;

    @BeforeEach
    void setUp() {
        festivalService = new FestivalService(festivalRepository, noticeRepository);
    }

    @Test
    void readsFirstFestivalAndPinnedThenLatestNotices() {
        Festival festival = festival(UUID.randomUUID());
        Festival ignoredFestival = festival(UUID.randomUUID());
        Notice pinned = notice(festival, "pinned", true, OffsetDateTime.of(2026, 5, 17, 10, 0, 0, 0, ZoneOffset.UTC));
        Notice latest = notice(festival, "latest", false, OffsetDateTime.of(2026, 5, 18, 10, 0, 0, 0, ZoneOffset.UTC));
        Notice older = notice(festival, "older", false, OffsetDateTime.of(2026, 5, 17, 10, 0, 0, 0, ZoneOffset.UTC));
        when(festivalRepository.findAll()).thenReturn(List.of(festival, ignoredFestival));
        when(noticeRepository.findByFestivalIdOrderByPinnedDescCreatedAtDesc(festival.getId()))
                .thenReturn(List.of(pinned, latest, older));

        FestivalDTO.Response festivalResponse = festivalService.getFestival();
        List<NoticeDTO.Response> noticeResponses = festivalService.getNotices();

        assertThat(festivalResponse.name()).isEqualTo("Festi");
        assertThat(noticeResponses).extracting(NoticeDTO.Response::title)
                .containsExactly("pinned", "latest", "older");
        assertThat(noticeResponses.get(0).pinned()).isTrue();
        assertThat(noticeResponses.get(1).pinned()).isFalse();
    }

    @Test
    void rejectsFestivalReadsWhenSingleFestivalIsMissing() {
        when(festivalRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> festivalService.getFestival())
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> festivalService.getNotices())
                .isInstanceOf(NotFoundException.class);
    }

    private Festival festival(UUID id) {
        Festival festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", id);
        return festival;
    }

    private Notice notice(Festival festival, String title, boolean pinned, OffsetDateTime createdAt) {
        Notice notice = new Notice(festival, title, "content", pinned, null);
        ReflectionTestUtils.setField(notice, "createdAt", createdAt);
        return notice;
    }
}
