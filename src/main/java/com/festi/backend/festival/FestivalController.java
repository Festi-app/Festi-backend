package com.festi.backend.festival;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/festival")
public class FestivalController {

    private final FestivalService festivalService;

    public FestivalController(FestivalService festivalService) {
        this.festivalService = festivalService;
    }

    @GetMapping
    public ResponseEntity<FestivalDTO.Response> getFestival() {
        return ResponseEntity.ok(festivalService.getFestival());
    }

    @GetMapping("/notices")
    public ResponseEntity<List<NoticeDTO.Response>> getNotices() {
        return ResponseEntity.ok(festivalService.getNotices());
    }
}
