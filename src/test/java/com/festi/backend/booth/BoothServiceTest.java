package com.festi.backend.booth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.festi.backend.menu.MenuItem;
import com.festi.backend.menu.MenuItemRepository;
import com.festi.backend.security.AuthenticatedUser;
import com.festi.backend.security.BoothAuthorizationService;
import com.festi.backend.user.UserRole;
import com.festi.backend.waiting.WaitingRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BoothServiceTest {

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private BoothLocationRepository boothLocationRepository;

    @Mock
    private FestivalRepository festivalRepository;

    @Mock
    private FestivalDayRepository festivalDayRepository;

    @Mock
    private BoothAuthorizationService boothAuthorizationService;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private ImageStorage imageStorage;

    @Mock
    private ImageFileTransactionManager imageFileTransactionManager;
    
    @Mock
    private WaitingRepository waitingRepository;

    private BoothService boothService;

    private Festival festival;
    private AuthenticatedUser manager;
    private AuthenticatedUser festivalAdmin;

    @BeforeEach
    void setUp() {
        boothService = new BoothService(boothRepository, boothLocationRepository, festivalRepository,
                festivalDayRepository, boothAuthorizationService, menuItemRepository, imageStorage,
                imageFileTransactionManager);
        festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
        manager = new AuthenticatedUser("manageruser", festival.getId(), UserRole.BOOTH_MANAGER);
        festivalAdmin = new AuthenticatedUser("adminuser", festival.getId(), UserRole.FESTIVAL_ADMIN);
    }

    // ── getBooths (no day) ───────────────────────────────────────────────────

    @Test
    void findBoothsByTypeAndCategoryWhenDayIsAbsent() {
        Booth booth = booth(UUID.randomUUID(), "night booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        when(boothRepository.findByTypeAndCategory(BoothType.NIGHT, BoothCategory.ALCOHOL))
                .thenReturn(List.of(booth));

        List<BoothDTO.Summary> response = boothService.getBooths(null, BoothType.NIGHT, BoothCategory.ALCOHOL);

        assertThat(response).extracting(BoothDTO.Summary::name).containsExactly("night booth");
    }

    @Test
    void findBoothsByTypeOnlyWhenNoCategoryFilter() {
        Booth b1 = booth(UUID.randomUUID(), "booth A", BoothCategory.ALCOHOL, BoothType.NIGHT);
        Booth b2 = booth(UUID.randomUUID(), "booth B", BoothCategory.MARKET, BoothType.NIGHT);
        when(boothRepository.findByType(BoothType.NIGHT)).thenReturn(List.of(b1, b2));

        List<BoothDTO.Summary> response = boothService.getBooths(null, BoothType.NIGHT, null);

        assertThat(response).hasSize(2);
    }

    @Test
    void findBoothsByCategoryOnlyWhenNoTypeFilter() {
        Booth b1 = booth(UUID.randomUUID(), "booth C", BoothCategory.MARKET, BoothType.DAY);
        when(boothRepository.findByCategory(BoothCategory.MARKET)).thenReturn(List.of(b1));

        List<BoothDTO.Summary> response = boothService.getBooths(null, null, BoothCategory.MARKET);

        assertThat(response).extracting(BoothDTO.Summary::name).containsExactly("booth C");
    }

    @Test
    void findAllBoothsWhenNoFilters() {
        Booth b1 = booth(UUID.randomUUID(), "booth D", BoothCategory.ACTIVITY, BoothType.DAY);
        Booth b2 = booth(UUID.randomUUID(), "booth E", BoothCategory.ALCOHOL, BoothType.NIGHT);
        when(boothRepository.findAll()).thenReturn(List.of(b1, b2));

        List<BoothDTO.Summary> response = boothService.getBooths(null, null, null);

        assertThat(response).hasSize(2);
    }

    // ── getBooths (with day) ─────────────────────────────────────────────────

    @Test
    void filtersPlacedBoothsByDayAndCategory() {
        LocalDate day = LocalDate.of(2026, 5, 20);
        FestivalDay festivalDay = new FestivalDay(festival, day);
        ReflectionTestUtils.setField(festivalDay, "id", UUID.randomUUID());

        Booth matching = booth(UUID.randomUUID(), "matching", BoothCategory.ALCOHOL, BoothType.NIGHT);
        Booth wrongCategory = booth(UUID.randomUUID(), "wrong", BoothCategory.INFO, BoothType.NIGHT);

        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(festivalDayRepository.findByFestivalIdAndDay(festival.getId(), day))
                .thenReturn(Optional.of(festivalDay));
        when(boothLocationRepository.findByDayOrderByIndex(festivalDay))
                .thenReturn(List.of(
                        location(festivalDay, BoothType.NIGHT, matching, (short) 1),
                        location(festivalDay, BoothType.NIGHT, wrongCategory, (short) 2)
                ));

        List<BoothDTO.Summary> response = boothService.getBooths(day, null, BoothCategory.ALCOHOL);

        assertThat(response).extracting(BoothDTO.Summary::name).containsExactly("matching");
    }

    @Test
    void throwsNotFoundWhenFestivalMissingOnDayQuery() {
        LocalDate day = LocalDate.of(2026, 5, 20);
        when(festivalRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> boothService.getBooths(day, null, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Festival not found");
    }

    @Test
    void throwsNotFoundWhenFestivalDayMissingOnDayQuery() {
        LocalDate day = LocalDate.of(2026, 5, 20);
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(festivalDayRepository.findByFestivalIdAndDay(festival.getId(), day))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> boothService.getBooths(day, null, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Festival day not found");
    }

    @Test
    void excludesUnassignedLocationsFromDayQuery() {
        LocalDate day = LocalDate.of(2026, 5, 18);
        FestivalDay festivalDay = new FestivalDay(festival, day);
        ReflectionTestUtils.setField(festivalDay, "id", UUID.randomUUID());

        // booth 없이 빈 location
        BoothLocation emptyLocation = new BoothLocation(festival, BoothType.NIGHT, festivalDay, "zone");

        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(festivalDayRepository.findByFestivalIdAndDay(festival.getId(), day))
                .thenReturn(Optional.of(festivalDay));
        when(boothLocationRepository.findByDayOrderByIndex(festivalDay))
                .thenReturn(List.of(emptyLocation));

        List<BoothDTO.Summary> response = boothService.getBooths(day, null, null);

        assertThat(response).isEmpty();
    }

    // ── getBooth ─────────────────────────────────────────────────────────────

    @Test
    void returnsBoothDetailAndRejectsMissingBooth() {
        UUID boothId = UUID.randomUUID();
        Booth booth = booth(boothId, "detail booth", BoothCategory.EXPERIENCE, BoothType.DAY);
        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(boothRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000000")))
                .thenReturn(Optional.empty());

        BoothDTO.Detail response = boothService.getBooth(boothId);

        assertThat(response.name()).isEqualTo("detail booth");
        assertThatThrownBy(() -> boothService.getBooth(UUID.fromString("00000000-0000-0000-0000-000000000000")))
                .isInstanceOf(NotFoundException.class);
    }

    // ── updateBooth ──────────────────────────────────────────────────────────

    @Test
    void updateBoothSucceeds() {
        UUID boothId = UUID.randomUUID();
        Booth booth = booth(boothId, "old name", BoothCategory.ALCOHOL, BoothType.NIGHT);
        BoothDTO.UpdateRequest request = new BoothDTO.UpdateRequest(
                "new name", BoothCategory.MARKET, "설명", "10:00~22:00");

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));

        BoothDTO.Detail response = boothService.updateBooth(manager, boothId, request);

        assertThat(response.name()).isEqualTo("new name");
        assertThat(response.category()).isEqualTo(BoothCategory.MARKET);
    }

    @Test
    void updateBoothThrowsNotFoundForMissingBooth() {
        UUID boothId = UUID.randomUUID();
        BoothDTO.UpdateRequest request = new BoothDTO.UpdateRequest(
                "name", BoothCategory.ACTIVITY, null, null);

        when(boothRepository.findById(boothId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boothService.updateBooth(manager, boothId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Booth not found");
    }

    @Test
    void updateBoothThrowsAccessDeniedWhenNotAuthorized() {
        UUID boothId = UUID.randomUUID();
        Booth booth = booth(boothId, "booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        BoothDTO.UpdateRequest request = new BoothDTO.UpdateRequest(
                "name", BoothCategory.ALCOHOL, null, null);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        doThrow(new AccessDeniedException("Access is denied."))
                .when(boothAuthorizationService).assertCanManageBooth(manager, booth);

        assertThatThrownBy(() -> boothService.updateBooth(manager, boothId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── createFoodTruck ──────────────────────────────────────────────────────

    @Test
    void createFoodTruckSucceeds() {
        BoothDTO.CreateFoodTruckRequest request = new BoothDTO.CreateFoodTruckRequest(
                "푸드트럭A", BoothCategory.MARKET, "설명", "11:00~20:00");

        Booth saved = booth(UUID.randomUUID(), "푸드트럭A", BoothCategory.MARKET, BoothType.FOOD_TRUCK);
        when(boothRepository.save(any(Booth.class))).thenReturn(saved);

        BoothDTO.Detail response = boothService.createFoodTruck(request);

        assertThat(response.name()).isEqualTo("푸드트럭A");
        assertThat(response.type()).isEqualTo(BoothType.FOOD_TRUCK);
    }

    @Test
    void createFoodTruckDefaultsToActivityCategoryWhenNull() {
        BoothDTO.CreateFoodTruckRequest request = new BoothDTO.CreateFoodTruckRequest(
                "푸드트럭B", null, null, null);

        Booth saved = booth(UUID.randomUUID(), "푸드트럭B", BoothCategory.ACTIVITY, BoothType.FOOD_TRUCK);
        when(boothRepository.save(any(Booth.class))).thenReturn(saved);

        BoothDTO.Detail response = boothService.createFoodTruck(request);

        assertThat(response.type()).isEqualTo(BoothType.FOOD_TRUCK);
        assertThat(response.category()).isEqualTo(BoothCategory.ACTIVITY);
    }

    // ── image management ─────────────────────────────────────────────────────

    @Test
    void replacesBoothImageThroughDedicatedOperation() {
        UUID boothId = UUID.randomUUID();
        Booth booth = booth(boothId, "booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        booth.updateImage("/media/images/booths/old.png");
        MockMultipartFile image = new MockMultipartFile("image", "new.png", "image/png", new byte[]{1});
        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(imageStorage.store(image, ImageDirectory.BOOTHS))
                .thenReturn(new StoredImage("/media/images/booths/new.png"));

        BoothDTO.Detail response = boothService.updateImage(manager, boothId, image);

        assertThat(response.imageUrl()).isEqualTo("/media/images/booths/new.png");
        verify(boothAuthorizationService).assertCanManageBooth(manager, booth);
        verify(imageStorage).store(image, ImageDirectory.BOOTHS);
        verify(imageFileTransactionManager).replaceAfterTransaction(
                "/media/images/booths/old.png", "/media/images/booths/new.png");
    }

    @Test
    void removesBoothImageIdempotently() {
        UUID boothId = UUID.randomUUID();
        Booth booth = booth(boothId, "booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));

        boothService.removeImage(manager, boothId);

        assertThat(booth.getImageUrl()).isNull();
        verify(imageFileTransactionManager).deleteAfterCommit(null);
    }

    @Test
    void deletingFoodTruckSchedulesBoothAndMenuImagesForDeletion() {
        UUID boothId = UUID.randomUUID();
        Booth foodTruck = booth(boothId, "truck", BoothCategory.ACTIVITY, BoothType.FOOD_TRUCK);
        foodTruck.updateImage("/media/images/booths/truck.png");
        MenuItem menuItem = new MenuItem(foodTruck, "menu", 5000, "desc", (short) 1);
        menuItem.updateImage("/media/images/menus/menu.png");
        when(boothRepository.findById(boothId)).thenReturn(Optional.of(foodTruck));
        when(menuItemRepository.findByBoothIdOrderBySortOrder(boothId)).thenReturn(List.of(menuItem));

        boothService.deleteFoodTruck(boothId);

        verify(boothRepository).delete(foodTruck);
        verify(imageFileTransactionManager).deleteAfterCommit("/media/images/booths/truck.png");
        verify(imageFileTransactionManager).deleteAfterCommit("/media/images/menus/menu.png");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Booth booth(UUID id, String name, BoothCategory category, BoothType type) {
        Booth booth = new Booth(name, category, type);
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }

    private BoothLocation location(FestivalDay festivalDay, BoothType type, Booth booth, short index) {
        BoothLocation location = new BoothLocation(festival, type, festivalDay, "zone");
        location.assignBooth(booth, index);
        return location;
    }
}
