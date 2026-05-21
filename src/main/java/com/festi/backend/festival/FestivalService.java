package com.festi.backend.festival;

import com.festi.backend.common.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    private Festival findSingleFestival() {
        List<Festival> festivals = festivalRepository.findAll();
        if (festivals.isEmpty()) {
            throw new NotFoundException("Festival not found.");
        }
        return festivals.getFirst();
    }
}
