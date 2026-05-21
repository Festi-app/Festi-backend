package com.festi.backend.favorite;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRepository;
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
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FavoriteDTO.Response> getFavorites(UUID userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(FavoriteDTO.Response::from)
                .toList();
    }

    @Transactional
    public FavoriteDTO.Response addFavorite(UUID userId, UUID boothId) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));

        if (favoriteRepository.existsByUserIdAndBoothId(userId, boothId)) {
            throw new ConflictException("Booth is already in favorites.");
        }

        long count = favoriteRepository.countByUserIdAndBoothType(userId, booth.getType());
        if (count >= MAX_FAVORITES_PER_TYPE) {
            throw new BadRequestException(
                    "Favorites limit reached for type " + booth.getType() + ". Maximum is " + MAX_FAVORITES_PER_TYPE + ".");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));

        Favorite favorite = favoriteRepository.save(new Favorite(user, booth));
        return FavoriteDTO.Response.from(favorite);
    }

    @Transactional
    public void removeFavorite(UUID userId, UUID favoriteId) {
        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new NotFoundException("Favorite not found."));

        if (!favorite.getUser().getId().equals(userId)) {
            throw new NotFoundException("Favorite not found.");
        }

        favoriteRepository.delete(favorite);
    }
}
