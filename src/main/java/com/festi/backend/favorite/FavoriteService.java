package com.festi.backend.favorite;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalRepository;
import com.festi.backend.waiting.WaitingRepository;
import com.festi.backend.waiting.WaitingStatus;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private static final int MAX_FAVORITES_PER_TYPE = 5;

    private final FavoriteRepository favoriteRepository;
    private final BoothRepository boothRepository;
    private final FestivalRepository festivalRepository;
    private final WaitingRepository waitingRepository;

    @Transactional(readOnly = true)
    public List<FavoriteDTO.Response> getFavorites(String userId, UUID festivalId) {
        return favoriteRepository.findByFestivalIdAndUserIdOrderByCreatedAtDesc(festivalId, userId).stream()
                .map(f -> FavoriteDTO.Response.from(f, (int) waitingRepository.countByBoothIdAndStatus(f.getBooth().getId(), WaitingStatus.WAITING)))
                .toList();
    }

    @Transactional
    public FavoriteDTO.Response addFavorite(String userId, UUID festivalId, UUID boothId) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));

        if (favoriteRepository.existsByFestivalIdAndUserIdAndBoothId(festivalId, userId, boothId)) {
            throw new ConflictException("Booth is already in favorites.");
        }

        long count = favoriteRepository.countByFestivalIdAndUserIdAndBoothType(festivalId, userId, booth.getType());
        if (count >= MAX_FAVORITES_PER_TYPE) {
            throw new BadRequestException(
                    "Favorites limit reached for type " + booth.getType() + ". Maximum is " + MAX_FAVORITES_PER_TYPE + ".");
        }

        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NotFoundException("Festival not found."));

        Favorite favorite = favoriteRepository.save(new Favorite(festival, userId, booth));
        return FavoriteDTO.Response.from(favorite, (int) waitingRepository.countByBoothIdAndStatus(booth.getId(), WaitingStatus.WAITING));
    }

    @Transactional
    public void removeFavorite(String userId, UUID favoriteId) {
        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new NotFoundException("Favorite not found."));

        if (!favorite.getUserId().equals(userId)) {
            throw new NotFoundException("Favorite not found.");
        }

        favoriteRepository.delete(favorite);
    }
}
