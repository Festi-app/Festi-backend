package com.festi.backend.waiting;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.booth.BoothType;
import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WaitingService {

    private static final int MAX_ACTIVE_WAITINGS = 3;

    private final WaitingRepository waitingRepository;
    private final BoothRepository boothRepository;
    private final UserRepository userRepository;

    public List<WaitingDTO.Response> getMyWaitings(String userId, UUID festivalId) {
        return waitingRepository.findByUserIdAndFestivalIdOrderByRegisteredAtDesc(userId, festivalId).stream()
                .map(WaitingDTO.Response::from)
                .toList();
    }

    @Transactional
    public WaitingDTO.Response registerWaiting(String userId, UUID festivalId, UUID boothId, short partySize) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));

        if (booth.getType() != BoothType.NIGHT) {
            throw new BadRequestException("Waiting is only available for NIGHT booths.");
        }

        if (!booth.isWaitingOpen()) {
            throw new BadRequestException("Waiting is not open for this booth.");
        }

        long activeCount = waitingRepository.countByUserIdAndFestivalIdAndStatusIn(
                userId, festivalId, List.of(WaitingStatus.WAITING, WaitingStatus.CALLED));
        if (activeCount >= MAX_ACTIVE_WAITINGS) {
            throw new BadRequestException("Maximum " + MAX_ACTIVE_WAITINGS + " active waitings allowed per user.");
        }

        User user = userRepository.findByIdAndFestivalId(userId, festivalId)
                .orElseThrow(() -> new NotFoundException("User not found."));

        Waiting waiting = waitingRepository.save(new Waiting(booth, user, partySize));
        return WaitingDTO.Response.from(waiting);
    }

    @Transactional
    public void cancelWaiting(String userId, UUID festivalId, UUID waitingId) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new NotFoundException("Waiting not found."));

        if (!waiting.getUser().getId().equals(userId)
                || !waiting.getUser().getFestivalId().equals(festivalId)) {
            throw new NotFoundException("Waiting not found.");
        }

        if (waiting.getStatus() != WaitingStatus.WAITING && waiting.getStatus() != WaitingStatus.CALLED) {
            throw new BadRequestException("Cannot cancel a waiting with status " + waiting.getStatus() + ".");
        }

        waiting.cancel();
    }
}
