package com.festi.backend.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.booth.BoothType;
import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private FestivalRepository festivalRepository;

    @Mock
    private WaitingRepository waitingRepository;

    private FavoriteService favoriteService;

    private Festival festival;
    private UUID festivalId;
    private String userId;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteRepository, boothRepository, festivalRepository, waitingRepository);
        festivalId = UUID.randomUUID();
        userId = "testuser";
        festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", festivalId);
    }

    // ── addFavorite ──────────────────────────────────────────────────────────

    @Test
    void addFavoriteSucceedsWhenUnderLimit() {
        UUID boothId = UUID.randomUUID();
        Booth booth = booth(boothId, "booth A", BoothType.NIGHT);
        Favorite saved = new Favorite(festival, userId, booth);
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(favoriteRepository.existsByFestivalIdAndUserIdAndBoothId(festivalId, userId, boothId)).thenReturn(false);
        when(favoriteRepository.countByFestivalIdAndUserIdAndBoothType(festivalId, userId, BoothType.NIGHT)).thenReturn(3L);
        when(festivalRepository.findById(festivalId)).thenReturn(Optional.of(festival));
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(saved);

        FavoriteDTO.Response response = favoriteService.addFavorite(userId, festivalId, boothId);

        assertThat(response).isNotNull();
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void addFavoriteThrowsConflictWhenAlreadyExists() {
        UUID boothId = UUID.randomUUID();
        Booth booth = booth(boothId, "booth B", BoothType.DAY);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(favoriteRepository.existsByFestivalIdAndUserIdAndBoothId(festivalId, userId, boothId)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(userId, festivalId, boothId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already in favorites");
    }

    @Test
    void addFavoriteThrowsBadRequestWhenLimitReached() {
        UUID boothId = UUID.randomUUID();
        Booth booth = booth(boothId, "booth C", BoothType.NIGHT);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(favoriteRepository.existsByFestivalIdAndUserIdAndBoothId(festivalId, userId, boothId)).thenReturn(false);
        when(favoriteRepository.countByFestivalIdAndUserIdAndBoothType(festivalId, userId, BoothType.NIGHT)).thenReturn(5L);

        assertThatThrownBy(() -> favoriteService.addFavorite(userId, festivalId, boothId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Favorites limit reached");
    }

    @Test
    void addFavoriteEnforcesLimitPerTypeIndependently() {
        // DAY 타입 5개이더라도 NIGHT 타입은 독립적으로 제한됨
        UUID nightBoothId = UUID.randomUUID();
        Booth nightBooth = booth(nightBoothId, "night booth", BoothType.NIGHT);
        Favorite saved = new Favorite(festival, userId, nightBooth);
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());

        when(boothRepository.findById(nightBoothId)).thenReturn(Optional.of(nightBooth));
        when(favoriteRepository.existsByFestivalIdAndUserIdAndBoothId(festivalId, userId, nightBoothId)).thenReturn(false);
        // NIGHT 타입 count는 0 — DAY 타입이 5개여도 NIGHT는 별개
        when(favoriteRepository.countByFestivalIdAndUserIdAndBoothType(festivalId, userId, BoothType.NIGHT)).thenReturn(0L);
        when(festivalRepository.findById(festivalId)).thenReturn(Optional.of(festival));
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(saved);

        FavoriteDTO.Response response = favoriteService.addFavorite(userId, festivalId, nightBoothId);

        assertThat(response).isNotNull();
        // NIGHT 타입만 조회하고 DAY 타입은 조회하지 않았음을 검증
        verify(favoriteRepository).countByFestivalIdAndUserIdAndBoothType(festivalId, userId, BoothType.NIGHT);
        verify(favoriteRepository, never()).countByFestivalIdAndUserIdAndBoothType(festivalId, userId, BoothType.DAY);
    }

    @Test
    void addFavoriteThrowsNotFoundWhenBoothMissing() {
        UUID boothId = UUID.randomUUID();
        when(boothRepository.findById(boothId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.addFavorite(userId, festivalId, boothId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Booth not found");
    }

    // ── removeFavorite ───────────────────────────────────────────────────────

    @Test
    void removeFavoriteSucceedsForOwner() {
        UUID favoriteId = UUID.randomUUID();
        Booth booth = booth(UUID.randomUUID(), "booth D", BoothType.DAY);
        Favorite favorite = new Favorite(festival, userId, booth);
        ReflectionTestUtils.setField(favorite, "id", favoriteId);

        when(favoriteRepository.findById(favoriteId)).thenReturn(Optional.of(favorite));

        favoriteService.removeFavorite(userId, favoriteId);

        verify(favoriteRepository).delete(favorite);
    }

    @Test
    void removeFavoriteThrowsNotFoundForNonOwner() {
        UUID favoriteId = UUID.randomUUID();
        Booth booth = booth(UUID.randomUUID(), "booth E", BoothType.DAY);
        Favorite favorite = new Favorite(festival, "otheruser", booth);
        ReflectionTestUtils.setField(favorite, "id", favoriteId);

        when(favoriteRepository.findById(favoriteId)).thenReturn(Optional.of(favorite));

        assertThatThrownBy(() -> favoriteService.removeFavorite(userId, favoriteId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Favorite not found");
    }

    @Test
    void removeFavoriteThrowsNotFoundWhenFavoriteMissing() {
        UUID favoriteId = UUID.randomUUID();
        when(favoriteRepository.findById(favoriteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.removeFavorite(userId, favoriteId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Favorite not found");
    }

    // ── getFavorites ─────────────────────────────────────────────────────────

    @Test
    void getFavoritesReturnsListOrderedByCreatedAtDesc() {
        Booth booth1 = booth(UUID.randomUUID(), "booth F", BoothType.NIGHT);
        Booth booth2 = booth(UUID.randomUUID(), "booth G", BoothType.DAY);
        Favorite fav1 = new Favorite(festival, userId, booth1);
        Favorite fav2 = new Favorite(festival, userId, booth2);
        ReflectionTestUtils.setField(fav1, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(fav2, "id", UUID.randomUUID());

        when(favoriteRepository.findByFestivalIdAndUserIdOrderByCreatedAtDesc(festivalId, userId))
                .thenReturn(List.of(fav1, fav2));

        List<FavoriteDTO.Response> result = favoriteService.getFavorites(userId, festivalId);

        assertThat(result).hasSize(2);
    }

    @Test
    void getFavoritesReturnsEmptyListWhenNone() {
        when(favoriteRepository.findByFestivalIdAndUserIdOrderByCreatedAtDesc(festivalId, userId))
                .thenReturn(List.of());

        List<FavoriteDTO.Response> result = favoriteService.getFavorites(userId, festivalId);

        assertThat(result).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Booth booth(UUID id, String name, BoothType type) {
        Booth booth = new Booth(name, BoothCategory.ACTIVITY, type);
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }
}
