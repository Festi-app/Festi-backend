package com.festi.backend.booth;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/booths")
public class BoothController {

    private final BoothService boothService;

    public BoothController(BoothService boothService) {
        this.boothService = boothService;
    }

    @GetMapping
    public ResponseEntity<List<BoothDTO.Summary>> getBooths(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
            @RequestParam(required = false) BoothType type,
            @RequestParam(required = false) BoothCategory category
    ) {
        return ResponseEntity.ok(boothService.getBooths(day, type, category));
    }

    @GetMapping("/{boothId}")
    public ResponseEntity<BoothDTO.Detail> getBooth(@PathVariable UUID boothId) {
        return ResponseEntity.ok(boothService.getBooth(boothId));
    }
}
