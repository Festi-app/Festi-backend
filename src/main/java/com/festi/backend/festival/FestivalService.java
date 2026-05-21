package com.festi.backend.festival;

import com.festi.backend.common.exception.NotFoundException;
import java.util.List;
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

    public FestivalDTO.Response getFestival() {
        return FestivalDTO.Response.from(findSingleFestival());
    }

    public List<NoticeDTO.Response> getNotices() {
        Festival festival = findSingleFestival();
        return noticeRepository.findByFestivalIdOrderByCreatedAtDesc(festival.getId()).stream()
                .map(NoticeDTO.Response::from)
                .toList();
    }

    public List<TimelineDTO.Response> getTimelines() {
        Festival festival = findSingleFestival();
        return timelineRepository.findByFestivalIdOrderByDayAscStartTimeAsc(festival.getId()).stream()
                .map(TimelineDTO.Response::from)
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
