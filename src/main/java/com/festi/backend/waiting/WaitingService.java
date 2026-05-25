package com.festi.backend.waiting;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothDTO;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.booth.BoothType;
import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.notification.WaitingNotificationService;
import com.festi.backend.security.AuthenticatedUser;
import com.festi.backend.security.BoothAuthorizationService;
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
    private static final List<WaitingStatus> ACTIVE_STATUSES =
            List.of(WaitingStatus.WAITING, WaitingStatus.CALLED);

    private final WaitingRepository waitingRepository;
    private final BoothRepository boothRepository;
    private final UserRepository userRepository;
    private final BoothAuthorizationService boothAuthorizationService;
    private final WaitingNotificationService waitingNotificationService;

    public List<WaitingDTO.Response> getMyWaitings(String userId, UUID festivalId) {
        return waitingRepository.findByUserIdAndFestivalIdOrderByRegisteredAtDesc(userId, festivalId).stream()
                .map(w -> WaitingDTO.Response.from(w, resolvePosition(w), resolveCurrentCallPosition(w.getBooth().getId()), resolveWaitingTeamCount(w.getBooth().getId())))
                .toList();
    }

    public List<WaitingDTO.Response> getActiveWaitings(AuthenticatedUser currentUser, UUID boothId) {
        Booth booth = getBooth(boothId);
        boothAuthorizationService.assertCanManageBooth(currentUser, booth);
        List<Waiting> waitings = waitingRepository.findByBoothIdAndStatusInOrderByRegisteredAtAsc(boothId, ACTIVE_STATUSES);
        Integer currentCallPosition = resolveCurrentCallPosition(boothId);
        Integer waitingTeamCount = resolveWaitingTeamCount(boothId);
        int[] position = {1};
        return waitings.stream()
                .map(w -> WaitingDTO.Response.from(w, w.getStatus() == WaitingStatus.WAITING ? position[0]++ : null, currentCallPosition, waitingTeamCount))
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
                userId, festivalId, ACTIVE_STATUSES);
        if (activeCount >= MAX_ACTIVE_WAITINGS) {
            throw new BadRequestException("Maximum " + MAX_ACTIVE_WAITINGS + " active waitings allowed per user.");
        }
        if (waitingRepository.existsByBoothIdAndUserIdAndFestivalIdAndStatusIn(
                boothId, userId, festivalId, ACTIVE_STATUSES)) {
            throw new ConflictException("Active waiting already exists for this booth.");
        }

        User user = userRepository.findByIdAndFestivalId(userId, festivalId)
                .orElseThrow(() -> new NotFoundException("User not found."));

        Waiting waiting = waitingRepository.save(new Waiting(booth, user, partySize));
        return WaitingDTO.Response.from(waiting, resolvePosition(waiting), resolveCurrentCallPosition(boothId), resolveWaitingTeamCount(boothId));
    }

    @Transactional
    public WaitingDTO.Response callWaiting(AuthenticatedUser currentUser, UUID waitingId) {
        Waiting waiting = getWaiting(waitingId);
        boothAuthorizationService.assertCanManageBooth(currentUser, waiting.getBooth());
        if (waiting.getStatus() != WaitingStatus.WAITING && waiting.getStatus() != WaitingStatus.CALLED) {
            throw new BadRequestException("Only active waitings can be called.");
        }
        waiting.call();
        waitingNotificationService.enqueueCalled(waiting);
        return WaitingDTO.Response.from(waiting);
    }

    @Transactional
    public WaitingDTO.Response updateWaitingStatus(AuthenticatedUser currentUser, UUID waitingId,
                                                    WaitingDTO.StatusRequest request) {
        Waiting waiting = getWaiting(waitingId);
        boothAuthorizationService.assertCanManageBooth(currentUser, waiting.getBooth());
        if (request.status() != WaitingStatus.SEATED || waiting.getStatus() != WaitingStatus.CALLED) {
            throw new BadRequestException("Only called waitings can be marked as seated.");
        }
        waiting.seat();
        return WaitingDTO.Response.from(waiting);
    }

    @Transactional
    public BoothDTO.Detail updateWaitingOpenStatus(AuthenticatedUser currentUser, UUID boothId,
                                                   WaitingDTO.OpenStatusRequest request) {
        Booth booth = getBooth(boothId);
        boothAuthorizationService.assertCanManageBooth(currentUser, booth);
        if (booth.getType() != BoothType.NIGHT) {
            throw new BadRequestException("Waiting is only available for NIGHT booths.");
        }
        if (request.open()) {
            booth.openWaiting();
        } else {
            booth.closeWaiting();
        }
        return BoothDTO.Detail.from(booth);
    }

    @Transactional
    public void cancelWaiting(String userId, UUID festivalId, UUID waitingId) {
        Waiting waiting = getWaiting(waitingId);

        if (!waiting.getUser().getId().equals(userId)
                || !waiting.getUser().getFestivalId().equals(festivalId)) {
            throw new NotFoundException("Waiting not found.");
        }

        if (waiting.getStatus() != WaitingStatus.WAITING && waiting.getStatus() != WaitingStatus.CALLED) {
            throw new BadRequestException("Cannot cancel a waiting with status " + waiting.getStatus() + ".");
        }

        waiting.cancel();
    }

    private Integer resolveWaitingTeamCount(UUID boothId) {
        return (int) waitingRepository.countByBoothIdAndStatus(boothId, WaitingStatus.WAITING);
    }

    private Integer resolveCurrentCallPosition(UUID boothId) {
        return waitingRepository.countActiveBeforeFirstCalled(boothId, ACTIVE_STATUSES, WaitingStatus.CALLED)
                .map(count -> (int) (count + 1))
                .orElse(null);
    }

    private Integer resolvePosition(Waiting waiting) {
        if (waiting.getStatus() != WaitingStatus.WAITING) {
            return null;
        }
        long ahead = waitingRepository.countByBoothIdAndStatusWaitingBeforeRegisteredAt(
                waiting.getBooth().getId(), waiting.getRegisteredAt());
        return (int) ahead + 1;
    }

    private Booth getBooth(UUID boothId) {
        return boothRepository.findById(boothId)
                .orElseThrow(() -> new NotFoundException("Booth not found."));
    }

    private Waiting getWaiting(UUID waitingId) {
        return waitingRepository.findById(waitingId)
                .orElseThrow(() -> new NotFoundException("Waiting not found."));
    }
}
