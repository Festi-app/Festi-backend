package com.festi.backend.booth;

import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalDay;
import com.festi.backend.festival.FestivalDayRepository;
import com.festi.backend.festival.FestivalRepository;
import com.festi.backend.image.ImageFileTransactionManager;
import com.festi.backend.image.ImageStorage;
import com.festi.backend.image.ImageStorage.ImageDirectory;
import com.festi.backend.image.ImageStorage.StoredImage;
import com.festi.backend.location.BoothLocation;
import com.festi.backend.location.BoothLocationRepository;
import com.festi.backend.menu.MenuItemRepository;
import com.festi.backend.security.AuthenticatedUser;
import com.festi.backend.security.BoothAuthorizationService;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoothService {

    private final BoothRepository boothRepository;
    private final BoothLocationRepository boothLocationRepository;
    private final FestivalRepository festivalRepository;
    private final FestivalDayRepository festivalDayRepository;
    private final BoothAuthorizationService boothAuthorizationService;
    private final MenuItemRepository menuItemRepository;
    private final ImageStorage imageStorage;
    private final ImageFileTransactionManager imageFileTransactionManager;

    public List<BoothDTO.Summary> getBooths(LocalDate day, BoothType type, BoothCategory category) {
        if (day != null) {
            return getPlacedBooths(day, type, category);
        }
        return getBooths(type, category).stream()
                .map(BoothDTO.Summary::from)
                .toList();
    }

    public BoothDTO.Detail getBooth(UUID boothId) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        return BoothDTO.Detail.from(booth);
    }

    @Transactional
    public BoothDTO.Detail createFoodTruck(BoothDTO.CreateFoodTruckRequest request) {
        BoothCategory category = request.category() != null ? request.category() : BoothCategory.ACTIVITY;
        Booth booth = new Booth(request.name(), category, BoothType.FOOD_TRUCK);
        booth.update(request.name(), category, request.description(), request.operatingHours());
        return BoothDTO.Detail.from(boothRepository.save(booth));
    }

    @Transactional
    public BoothDTO.Detail updateFoodTruck(UUID boothId, BoothDTO.UpdateFoodTruckRequest request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        if (booth.getType() != BoothType.FOOD_TRUCK) {
            throw new BadRequestException("Booth is not a food truck.");
        }
        booth.updatePartial(request.name(), request.category(), request.description(), request.operatingHours());
        return BoothDTO.Detail.from(booth);
    }

    @Transactional
    public void deleteFoodTruck(UUID boothId) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        if (booth.getType() != BoothType.FOOD_TRUCK) {
            throw new BadRequestException("Booth is not a food truck.");
        }
        menuItemRepository.findByBoothIdOrderBySortOrder(boothId).forEach(
                menuItem -> imageFileTransactionManager.deleteAfterCommit(menuItem.getImageUrl()));
        imageFileTransactionManager.deleteAfterCommit(booth.getImageUrl());
        boothRepository.delete(booth);
    }

    @Transactional
    public BoothDTO.Detail updateBooth(AuthenticatedUser currentUser, UUID boothId,
                                       BoothDTO.UpdateRequest request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        boothAuthorizationService.assertCanManageBooth(currentUser, booth);
        booth.update(request.name(), request.category(), request.description(), request.operatingHours());
        return BoothDTO.Detail.from(booth);
    }

    @Transactional
    public BoothDTO.Detail updateImage(AuthenticatedUser currentUser, UUID boothId, MultipartFile image) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        boothAuthorizationService.assertCanManageBooth(currentUser, booth);
        String previousUrl = booth.getImageUrl();
        StoredImage storedImage = imageStorage.store(image, ImageDirectory.BOOTHS);
        imageFileTransactionManager.replaceAfterTransaction(previousUrl, storedImage.publicUrl());
        booth.updateImage(storedImage.publicUrl());
        return BoothDTO.Detail.from(booth);
    }

    @Transactional
    public void removeImage(AuthenticatedUser currentUser, UUID boothId) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
        boothAuthorizationService.assertCanManageBooth(currentUser, booth);
        String previousUrl = booth.getImageUrl();
        booth.removeImage();
        imageFileTransactionManager.deleteAfterCommit(previousUrl);
    }

    private List<BoothDTO.Summary> getPlacedBooths(LocalDate day, BoothType type, BoothCategory category) {
        Festival festival = festivalRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Festival not found."));
        FestivalDay festivalDay = festivalDayRepository.findByFestivalIdAndDay(festival.getId(), day)
                .orElseThrow(() -> new NotFoundException("Festival day not found."));
        List<BoothLocation> locations = type == null
                ? boothLocationRepository.findByDayOrderByIndex(festivalDay)
                : boothLocationRepository.findByDayAndTypeOrderByIndex(festivalDay, type);

        Map<UUID, Booth> uniqueBooths = new LinkedHashMap<>();
        for (BoothLocation location : locations) {
            Booth booth = location.getBooth();
            if (booth == null) {
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

    private List<Booth> getBooths(BoothType type, BoothCategory category) {
        if (type != null && category != null) {
            return boothRepository.findByTypeAndCategory(type, category);
        }
        if (type != null) {
            return boothRepository.findByType(type);
        }
        if (category != null) {
            return boothRepository.findByCategory(category);
        }
        return boothRepository.findAll();
    }
}
