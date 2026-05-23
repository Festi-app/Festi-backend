package com.festi.backend.festival;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.common.exception.NotFoundException;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
class FestivalServiceTest {

    @Mock
    private FestivalRepository festivalRepository;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private TimelineRepository timelineRepository;

    @Mock
    private FestivalDayRepository festivalDayRepository;

    private FestivalService festivalService;

    @BeforeEach
    void setUp() {
        festivalService = new FestivalService(festivalRepository, noticeRepository, timelineRepository, festivalDayRepository);
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

    @Test
    void updatesFestivalAndManagesFestivalDays() {
        Festival festival = festival(UUID.randomUUID());
        UUID dayId = UUID.randomUUID();
        FestivalDay day = festivalDay(festival, dayId, LocalDate.of(2026, 5, 18));
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(festivalDayRepository.existsByFestivalIdAndDay(festival.getId(), LocalDate.of(2026, 5, 18)))
                .thenReturn(false);
        when(festivalDayRepository.save(any(FestivalDay.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(festivalDayRepository.findByIdAndFestivalId(dayId, festival.getId())).thenReturn(Optional.of(day));
        when(festivalDayRepository.existsByFestivalIdAndDayAndIdNot(
                festival.getId(), LocalDate.of(2026, 5, 19), dayId)).thenReturn(false);

        FestivalDTO.Response updatedFestival = festivalService.updateFestival(new FestivalDTO.UpdateRequest(
                "Updated", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "updated desc"));
        FestivalDayDTO.Response createdDay = festivalService.createFestivalDay(dayRequest(LocalDate.of(2026, 5, 18)));
        FestivalDayDTO.Response updatedDay = festivalService.updateFestivalDay(dayId, dayRequest(LocalDate.of(2026, 5, 19)));
        festivalService.deleteFestivalDay(dayId);

        assertThat(updatedFestival.name()).isEqualTo("Updated");
        assertThat(createdDay.day()).isEqualTo(LocalDate.of(2026, 5, 18));
        assertThat(updatedDay.day()).isEqualTo(LocalDate.of(2026, 5, 19));
        verify(festivalDayRepository).delete(day);
    }

    @Test
    void rejectsDuplicateFestivalDayCreateOrUpdate() {
        Festival festival = festival(UUID.randomUUID());
        UUID dayId = UUID.randomUUID();
        FestivalDay day = festivalDay(festival, dayId, LocalDate.of(2026, 5, 18));
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(festivalDayRepository.existsByFestivalIdAndDay(festival.getId(), LocalDate.of(2026, 5, 18)))
                .thenReturn(true);
        when(festivalDayRepository.findByIdAndFestivalId(dayId, festival.getId())).thenReturn(Optional.of(day));
        when(festivalDayRepository.existsByFestivalIdAndDayAndIdNot(
                festival.getId(), LocalDate.of(2026, 5, 19), dayId)).thenReturn(true);

        assertThatThrownBy(() -> festivalService.createFestivalDay(dayRequest(LocalDate.of(2026, 5, 18))))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> festivalService.updateFestivalDay(dayId, dayRequest(LocalDate.of(2026, 5, 19))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createsUpdatesAndDeletesNotices() {
        Festival festival = festival(UUID.randomUUID());
        UUID noticeId = UUID.randomUUID();
        Notice notice = notice(festival, "Notice", false, OffsetDateTime.now());
        ReflectionTestUtils.setField(notice, "id", noticeId);
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noticeRepository.findByIdAndFestivalId(noticeId, festival.getId())).thenReturn(Optional.of(notice));

        NoticeDTO.Response created = festivalService.createNotice(new NoticeDTO.Request("Notice", "Content", true));
        NoticeDTO.Response updated = festivalService.updateNotice(noticeId, new NoticeDTO.Request("Updated", "New content", false));
        festivalService.deleteNotice(noticeId);

        assertThat(created.title()).isEqualTo("Notice");
        assertThat(updated.title()).isEqualTo("Updated");
        assertThat(updated.pinned()).isFalse();
        verify(noticeRepository).delete(notice);
    }

    @Test
    void createsUpdatesAndDeletesTimelinesUsingFestivalDay() {
        Festival festival = festival(UUID.randomUUID());
        UUID dayId = UUID.randomUUID();
        UUID timelineId = UUID.randomUUID();
        FestivalDay day = festivalDay(festival, dayId, LocalDate.of(2026, 5, 18));
        Timeline timeline = timeline(festival, timelineId, LocalDate.of(2026, 5, 18), "Stage");
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(festivalDayRepository.findByIdAndFestivalId(dayId, festival.getId())).thenReturn(Optional.of(day));
        when(timelineRepository.save(any(Timeline.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(timelineRepository.findByIdAndFestivalId(timelineId, festival.getId())).thenReturn(Optional.of(timeline));

        TimelineDTO.Response created = festivalService.createTimeline(timelineRequest(dayId, "Stage"));
        TimelineDTO.Response updated = festivalService.updateTimeline(timelineId, timelineRequest(dayId, "Updated Stage"));
        festivalService.deleteTimeline(timelineId);

        assertThat(created.festivalDay().id()).isEqualTo(dayId);
        assertThat(updated.title()).isEqualTo("Updated Stage");
        verify(timelineRepository).delete(timeline);
    }

    private Festival festival(UUID id) {
        Festival festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", id);
        return festival;
    }

    private FestivalDay festivalDay(Festival festival, UUID id, LocalDate day) {
        FestivalDay festivalDay = new FestivalDay(festival, day);
        festivalDay.updateHours(LocalTime.of(10, 0), LocalTime.of(17, 0), LocalTime.of(18, 0), LocalTime.of(23, 0));
        ReflectionTestUtils.setField(festivalDay, "id", id);
        return festivalDay;
    }

    private Notice notice(Festival festival, String title, boolean pinned, OffsetDateTime createdAt) {
        Notice notice = new Notice(festival, title, "content", pinned);
        ReflectionTestUtils.setField(notice, "createdAt", createdAt);
        return notice;
    }

    private Timeline timeline(Festival festival, UUID id, LocalDate day, String title) {
        Timeline timeline = new Timeline(festival, day, title, "Artist", LocalTime.of(18, 0), LocalTime.of(19, 0));
        ReflectionTestUtils.setField(timeline, "id", id);
        return timeline;
    }

    private FestivalDayDTO.Request dayRequest(LocalDate day) {
        return new FestivalDayDTO.Request(
                day,
                LocalTime.of(10, 0),
                LocalTime.of(17, 0),
                LocalTime.of(18, 0),
                LocalTime.of(23, 0)
        );
    }

    private TimelineDTO.Request timelineRequest(UUID festivalDayId, String title) {
        return new TimelineDTO.Request(
                festivalDayId,
                title,
                "Artist",
                LocalTime.of(18, 0),
                LocalTime.of(19, 0)
        );
    }
}
