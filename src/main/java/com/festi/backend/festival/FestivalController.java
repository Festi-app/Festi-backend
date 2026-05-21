package com.festi.backend.festival;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/festival")
@RequiredArgsConstructor
public class FestivalController {

    private final FestivalService festivalService;

    @GetMapping
    public ResponseEntity<FestivalDTO.Response> getFestival() {
        return ResponseEntity.ok(festivalService.getFestival());
    }

    @GetMapping("/notices")
    public ResponseEntity<List<NoticeDTO.Response>> getNotices() {
        return ResponseEntity.ok(festivalService.getNotices());
    }

    @GetMapping("/timelines")
    public ResponseEntity<List<TimelineDTO.Response>> getTimelines() {
        return ResponseEntity.ok(festivalService.getTimelines());
    }
}
