package com.festi.backend.waiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.booth.BoothType;
import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.festival.Festival;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRepository;
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
class WaitingServiceTest {

    @Mock
    private WaitingRepository waitingRepository;

    @Mock
    private BoothRepository boothRepository;

    @Mock
    private UserRepository userRepository;

    private WaitingService waitingService;

    private Festival festival;
    private UUID festivalId;
    private String userId;
    private User user;

    @BeforeEach
    void setUp() {
        waitingService = new WaitingService(waitingRepository, boothRepository, userRepository);
        festivalId = UUID.randomUUID();
        userId = "alice123";
        festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", festivalId);
        user = new User(festival, userId, "hashed", "nickname", "01012345678");
    }

    // ── getMyWaitings ─────────────────────────────────────────────────────────

    @Test
    void readsCurrentUsersWaitingsInRepositoryOrder() {
        Booth booth = nightBooth(UUID.randomUUID(), true);
        Waiting waiting = new Waiting(booth, user, (short) 2);

        when(waitingRepository.findByUserIdAndFestivalIdOrderByRegisteredAtDesc(userId, festivalId))
                .thenReturn(List.of(waiting));

        List<WaitingDTO.Response> response = waitingService.getMyWaitings(userId, festivalId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().boothSummary().name()).isEqualTo("night booth");
    }

    @Test
    void getMyWaitingsReturnsEmptyListWhenNone() {
        when(waitingRepository.findByUserIdAndFestivalIdOrderByRegisteredAtDesc(userId, festivalId))
                .thenReturn(List.of());

        List<WaitingDTO.Response> response = waitingService.getMyWaitings(userId, festivalId);

        assertThat(response).isEmpty();
    }

    // ── registerWaiting ───────────────────────────────────────────────────────

    @Test
    void registerWaitingSucceeds() {
        UUID boothId = UUID.randomUUID();
        Booth booth = nightBooth(boothId, true);
        Waiting saved = new Waiting(booth, user, (short) 2);
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(waitingRepository.countByUserIdAndFestivalIdAndStatusIn(
                userId, festivalId, List.of(WaitingStatus.WAITING, WaitingStatus.CALLED))).thenReturn(1L);
        when(userRepository.findByIdAndFestivalId(userId, festivalId)).thenReturn(Optional.of(user));
        when(waitingRepository.save(any(Waiting.class))).thenReturn(saved);

        WaitingDTO.Response response = waitingService.registerWaiting(userId, festivalId, boothId, (short) 2);

        assertThat(response.partySize()).isEqualTo((short) 2);
        assertThat(response.status()).isEqualTo(WaitingStatus.WAITING);
        verify(waitingRepository).save(any(Waiting.class));
    }

    @Test
    void registerWaitingThrowsNotFoundForMissingBooth() {
        UUID boothId = UUID.randomUUID();
        when(boothRepository.findById(boothId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waitingService.registerWaiting(userId, festivalId, boothId, (short) 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Booth not found");
    }

    @Test
    void registerWaitingThrowsBadRequestForDayBooth() {
        UUID boothId = UUID.randomUUID();
        Booth dayBooth = new Booth("day booth", BoothCategory.ACTIVITY, BoothType.DAY);
        ReflectionTestUtils.setField(dayBooth, "id", boothId);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(dayBooth));

        assertThatThrownBy(() -> waitingService.registerWaiting(userId, festivalId, boothId, (short) 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("NIGHT booths");
    }

    @Test
    void registerWaitingThrowsBadRequestWhenWaitingNotOpen() {
        UUID boothId = UUID.randomUUID();
        Booth booth = nightBooth(boothId, false); // isWaitingOpen = false

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));

        assertThatThrownBy(() -> waitingService.registerWaiting(userId, festivalId, boothId, (short) 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not open");
    }

    @Test
    void registerWaitingThrowsBadRequestWhenActiveWaitingsAtLimit() {
        UUID boothId = UUID.randomUUID();
        Booth booth = nightBooth(boothId, true);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(waitingRepository.countByUserIdAndFestivalIdAndStatusIn(
                userId, festivalId, List.of(WaitingStatus.WAITING, WaitingStatus.CALLED))).thenReturn(3L);

        assertThatThrownBy(() -> waitingService.registerWaiting(userId, festivalId, boothId, (short) 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Maximum");
    }

    @Test
    void registerWaitingThrowsNotFoundWhenUserMissing() {
        UUID boothId = UUID.randomUUID();
        Booth booth = nightBooth(boothId, true);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(waitingRepository.countByUserIdAndFestivalIdAndStatusIn(
                userId, festivalId, List.of(WaitingStatus.WAITING, WaitingStatus.CALLED))).thenReturn(0L);
        when(userRepository.findByIdAndFestivalId(userId, festivalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waitingService.registerWaiting(userId, festivalId, boothId, (short) 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ── cancelWaiting ─────────────────────────────────────────────────────────

    @Test
    void cancelWaitingSucceedsWithWaitingStatus() {
        UUID waitingId = UUID.randomUUID();
        Booth booth = nightBooth(UUID.randomUUID(), true);
        Waiting waiting = waiting(waitingId, booth, user, WaitingStatus.WAITING);

        when(waitingRepository.findById(waitingId)).thenReturn(Optional.of(waiting));

        waitingService.cancelWaiting(userId, festivalId, waitingId);

        assertThat(waiting.getStatus()).isEqualTo(WaitingStatus.CANCELLED);
    }

    @Test
    void cancelWaitingSucceedsWithCalledStatus() {
        UUID waitingId = UUID.randomUUID();
        Booth booth = nightBooth(UUID.randomUUID(), true);
        Waiting waiting = waiting(waitingId, booth, user, WaitingStatus.CALLED);

        when(waitingRepository.findById(waitingId)).thenReturn(Optional.of(waiting));

        waitingService.cancelWaiting(userId, festivalId, waitingId);

        assertThat(waiting.getStatus()).isEqualTo(WaitingStatus.CANCELLED);
    }

    @Test
    void cancelWaitingThrowsNotFoundForMissingWaiting() {
        UUID waitingId = UUID.randomUUID();
        when(waitingRepository.findById(waitingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waitingService.cancelWaiting(userId, festivalId, waitingId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Waiting not found");
    }

    @Test
    void cancelWaitingThrowsNotFoundForWrongOwner() {
        UUID waitingId = UUID.randomUUID();
        Booth booth = nightBooth(UUID.randomUUID(), true);
        User otherUser = new User(festival, "otheruser", "hashed", "other", "01000000000");
        Waiting waiting = waiting(waitingId, booth, otherUser, WaitingStatus.WAITING);

        when(waitingRepository.findById(waitingId)).thenReturn(Optional.of(waiting));

        assertThatThrownBy(() -> waitingService.cancelWaiting(userId, festivalId, waitingId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Waiting not found");
    }

    @Test
    void cancelWaitingThrowsBadRequestWhenAlreadySeated() {
        UUID waitingId = UUID.randomUUID();
        Booth booth = nightBooth(UUID.randomUUID(), true);
        Waiting waiting = waiting(waitingId, booth, user, WaitingStatus.SEATED);

        when(waitingRepository.findById(waitingId)).thenReturn(Optional.of(waiting));

        assertThatThrownBy(() -> waitingService.cancelWaiting(userId, festivalId, waitingId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot cancel");
    }

    @Test
    void cancelWaitingThrowsBadRequestWhenAlreadyCancelled() {
        UUID waitingId = UUID.randomUUID();
        Booth booth = nightBooth(UUID.randomUUID(), true);
        Waiting waiting = waiting(waitingId, booth, user, WaitingStatus.CANCELLED);

        when(waitingRepository.findById(waitingId)).thenReturn(Optional.of(waiting));

        assertThatThrownBy(() -> waitingService.cancelWaiting(userId, festivalId, waitingId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot cancel");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Booth nightBooth(UUID id, boolean waitingOpen) {
        Booth booth = new Booth("night booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        ReflectionTestUtils.setField(booth, "id", id);
        if (waitingOpen) {
            booth.openWaiting();
        }
        return booth;
    }

    private Waiting waiting(UUID id, Booth booth, User user, WaitingStatus status) {
        Waiting waiting = new Waiting(booth, user, (short) 2);
        ReflectionTestUtils.setField(waiting, "id", id);
        ReflectionTestUtils.setField(waiting, "status", status);
        return waiting;
    }
}
