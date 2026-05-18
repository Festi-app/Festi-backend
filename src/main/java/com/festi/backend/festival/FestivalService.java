package com.festi.backend.festival;

import com.festi.backend.common.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FestivalService {

    private final FestivalRepository festivalRepository;
    private final NoticeRepository noticeRepository;

    public FestivalService(FestivalRepository festivalRepository, NoticeRepository noticeRepository) {
        this.festivalRepository = festivalRepository;
        this.noticeRepository = noticeRepository;
    }

    public FestivalDTO.Response getFestival() {
        return FestivalDTO.Response.from(findSingleFestival());
    }

    public List<NoticeDTO.Response> getNotices() {
        Festival festival = findSingleFestival();
        return noticeRepository.findByFestivalIdOrderByCreatedAtDesc(festival.getId()).stream()
                .map(NoticeDTO.Response::from)
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
