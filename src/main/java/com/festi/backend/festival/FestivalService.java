package com.festi.backend.festival;

import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.common.exception.NotFoundException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FestivalService {

    private final FestivalRepository festivalRepository;
    private final NoticeRepository noticeRepository;
    private final TimelineRepository timelineRepository;
    private final FestivalDayRepository festivalDayRepository;

    public FestivalDTO.Response getFestival() {
        return FestivalDTO.Response.from(findSingleFestival());
    }

    public List<FestivalDayDTO.Summary> getFestivalDays() {
        Festival festival = findSingleFestival();
        return festivalDayRepository.findByFestivalIdOrderByDay(festival.getId()).stream()
                .map(FestivalDayDTO.Summary::from)
                .toList();
    }

    public List<NoticeDTO.Response> getNotices() {
        Festival festival = findSingleFestival();
        return noticeRepository.findByFestivalIdOrderByPinnedDescCreatedAtDesc(festival.getId()).stream()
                .map(NoticeDTO.Response::from)
                .toList();
    }

    public List<TimelineDTO.Response> getTimelines() {
        Festival festival = findSingleFestival();
        Map<LocalDate, FestivalDay> festivalDayByDate = festivalDayRepository
                .findByFestivalIdOrderByDay(festival.getId()).stream()
                .collect(Collectors.toMap(FestivalDay::getDay, fd -> fd));
        return timelineRepository.findByFestivalIdOrderByDayAscStartTimeAsc(festival.getId()).stream()
                .map(timeline -> TimelineDTO.Response.from(timeline, festivalDayByDate.get(timeline.getDay())))
                .toList();
    }

    @Transactional
    public FestivalDTO.Response updateFestival(FestivalDTO.UpdateRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        Festival festival = findSingleFestival();
        festival.update(request.name(), request.startDate(), request.endDate(), request.description());
        return FestivalDTO.Response.from(festival);
    }

    @Transactional
    public FestivalDayDTO.Response createFestivalDay(FestivalDayDTO.Request request) {
        validateHours(request);
        Festival festival = findSingleFestival();
        if (festivalDayRepository.existsByFestivalIdAndDay(festival.getId(), request.day())) {
            throw new ConflictException("Festival day already exists.");
        }

        FestivalDay festivalDay = new FestivalDay(festival, request.day());
        festivalDay.updateHours(request.dayStart(), request.dayEnd(), request.nightStart(), request.nightEnd());
        return FestivalDayDTO.Response.from(festivalDayRepository.save(festivalDay));
    }

    @Transactional
    public FestivalDayDTO.Response updateFestivalDay(UUID festivalDayId, FestivalDayDTO.Request request) {
        validateHours(request);
        Festival festival = findSingleFestival();
        FestivalDay festivalDay = findFestivalDay(festivalDayId, festival);
        if (festivalDayRepository.existsByFestivalIdAndDayAndIdNot(festival.getId(), request.day(), festivalDayId)) {
            throw new ConflictException("Festival day already exists.");
        }

        festivalDay.update(request.day(), request.dayStart(), request.dayEnd(), request.nightStart(), request.nightEnd());
        return FestivalDayDTO.Response.from(festivalDay);
    }

    @Transactional
    public void deleteFestivalDay(UUID festivalDayId) {
        Festival festival = findSingleFestival();
        festivalDayRepository.delete(findFestivalDay(festivalDayId, festival));
    }

    @Transactional
    public NoticeDTO.Response createNotice(NoticeDTO.Request request) {
        Festival festival = findSingleFestival();
        Notice notice = new Notice(festival, request.title(), request.content(), request.pinned());
        return NoticeDTO.Response.from(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeDTO.Response updateNotice(UUID noticeId, NoticeDTO.Request request) {
        Festival festival = findSingleFestival();
        Notice notice = noticeRepository.findByIdAndFestivalId(noticeId, festival.getId())
                .orElseThrow(() -> new NotFoundException("Notice not found."));
        notice.update(request.title(), request.content(), request.pinned());
        return NoticeDTO.Response.from(notice);
    }

    @Transactional
    public void deleteNotice(UUID noticeId) {
        Festival festival = findSingleFestival();
        Notice notice = noticeRepository.findByIdAndFestivalId(noticeId, festival.getId())
                .orElseThrow(() -> new NotFoundException("Notice not found."));
        noticeRepository.delete(notice);
    }

    @Transactional
    public TimelineDTO.Response createTimeline(TimelineDTO.Request request) {
        Festival festival = findSingleFestival();
        FestivalDay festivalDay = findFestivalDay(request.festivalDayId(), festival);
        validateTimeRange(request.startTime(), request.endTime());
        Timeline timeline = new Timeline(
                festival,
                festivalDay.getDay(),
                request.title(),
                request.artist(),
                request.startTime(),
                request.endTime()
        );
        return TimelineDTO.Response.from(timelineRepository.save(timeline), festivalDay);
    }

    @Transactional
    public TimelineDTO.Response updateTimeline(UUID timelineId, TimelineDTO.Request request) {
        Festival festival = findSingleFestival();
        FestivalDay festivalDay = findFestivalDay(request.festivalDayId(), festival);
        validateTimeRange(request.startTime(), request.endTime());
        Timeline timeline = timelineRepository.findByIdAndFestivalId(timelineId, festival.getId())
                .orElseThrow(() -> new NotFoundException("Timeline not found."));
        timeline.update(festivalDay.getDay(), request.title(), request.artist(), request.startTime(), request.endTime());
        return TimelineDTO.Response.from(timeline, festivalDay);
    }

    @Transactional
    public void deleteTimeline(UUID timelineId) {
        Festival festival = findSingleFestival();
        Timeline timeline = timelineRepository.findByIdAndFestivalId(timelineId, festival.getId())
                .orElseThrow(() -> new NotFoundException("Timeline not found."));
        timelineRepository.delete(timeline);
    }

    private FestivalDay findFestivalDay(UUID festivalDayId, Festival festival) {
        return festivalDayRepository.findByIdAndFestivalId(festivalDayId, festival.getId())
                .orElseThrow(() -> new NotFoundException("Festival day not found."));
    }

    private Festival findSingleFestival() {
        List<Festival> festivals = festivalRepository.findAll();
        if (festivals.isEmpty()) {
            throw new NotFoundException("Festival not found.");
        }
        return festivals.getFirst();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Festival start date must be on or before end date.");
        }
    }

    private void validateHours(FestivalDayDTO.Request request) {
        validateTimeRange(request.dayStart(), request.dayEnd());
        validateTimeRange(request.nightStart(), request.nightEnd());
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("Start time must be before end time.");
        }
    }
}
