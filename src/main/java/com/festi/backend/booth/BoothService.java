package com.festi.backend.booth;

import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.location.BoothLocation;
import com.festi.backend.location.BoothLocationRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BoothService {

    private final BoothRepository boothRepository;
    private final BoothLocationRepository boothLocationRepository;

    public BoothService(BoothRepository boothRepository, BoothLocationRepository boothLocationRepository) {
        this.boothRepository = boothRepository;
        this.boothLocationRepository = boothLocationRepository;
    }

    public List<BoothDTO.Summary> getBooths(LocalDate day, BoothType type, BoothCategory category) {
        if (day != null) {
            return getPlacedBooths(day, type, category);
        }
        return getActiveBooths(type, category).stream()
                .map(BoothDTO.Summary::from)
                .toList();
    }

    public BoothDTO.Detail getBooth(UUID boothId) {
        Booth booth = boothRepository.findByIdAndIsActiveTrue(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        return BoothDTO.Detail.from(booth);
    }

    private List<BoothDTO.Summary> getPlacedBooths(LocalDate day, BoothType type, BoothCategory category) {
        List<BoothLocation> locations = type == null
                ? boothLocationRepository.findByDayOrderByIndex(day)
                : boothLocationRepository.findByDayAndTypeOrderByIndex(day, type);

        Map<UUID, Booth> uniqueBooths = new LinkedHashMap<>();
        for (BoothLocation location : locations) {
            Booth booth = location.getBooth();
            if (booth == null || !booth.isActive()) {
                continue;
            }
            if (type != null && booth.getType() != type) {
                continue;
            }
            if (category != null && booth.getCategory() != category) {
                continue;
            }
            uniqueBooths.putIfAbsent(booth.getId(), booth);
        }

        return uniqueBooths.values().stream()
                .map(BoothDTO.Summary::from)
                .toList();
    }

    private List<Booth> getActiveBooths(BoothType type, BoothCategory category) {
        if (type != null && category != null) {
            return boothRepository.findByTypeAndCategoryAndIsActiveTrue(type, category);
        }
        if (type != null) {
            return boothRepository.findByTypeAndIsActiveTrue(type);
        }
        if (category != null) {
            return boothRepository.findByCategoryAndIsActiveTrue(category);
        }
        return boothRepository.findByIsActiveTrue();
    }
}
