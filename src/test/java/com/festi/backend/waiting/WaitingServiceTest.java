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
import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.festival.Festival;
import com.festi.backend.notification.WaitingNotificationService;
import com.festi.backend.security.AuthenticatedUser;
import com.festi.backend.security.BoothAuthorizationService;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRepository;
import com.festi.backend.user.UserRole;
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

    @Mock
    private BoothAuthorizationService boothAuthorizationService;

    @Mock
    private WaitingNotificationService waitingNotificationService;

    private WaitingService waitingService;

    private Festival festival;
    private UUID festivalId;
    private String userId;
    private User user;

    @BeforeEach
    void setUp() {
        waitingService = new WaitingService(
                waitingRepository, boothRepository, userRepository, boothAuthorizationService, waitingNotificationService);
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
        when(waitingRepository.existsByBoothIdAndUserIdAndFestivalIdAndStatusIn(
                boothId, userId, festivalId, List.of(WaitingStatus.WAITING, WaitingStatus.CALLED))).thenReturn(false);
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
        Booth booth = nightBooth(boothId, false);

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
    void registerWaitingThrowsConflictWhenUserAlreadyHasActiveWaitingAtSameBooth() {
        UUID boothId = UUID.randomUUID();
        Booth booth = nightBooth(boothId, true);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(waitingRepository.countByUserIdAndFestivalIdAndStatusIn(
                userId, festivalId, List.of(WaitingStatus.WAITING, WaitingStatus.CALLED))).thenReturn(1L);
        when(waitingRepository.existsByBoothIdAndUserIdAndFestivalIdAndStatusIn(
                boothId, userId, festivalId, List.of(WaitingStatus.WAITING, WaitingStatus.CALLED))).thenReturn(true);

        assertThatThrownBy(() -> waitingService.registerWaiting(userId, festivalId, boothId, (short) 1))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void registerWaitingThrowsNotFoundWhenUserMissing() {
        UUID boothId = UUID.randomUUID();
        Booth booth = nightBooth(boothId, true);

        when(boothRepository.findById(boothId)).thenReturn(Optional.of(booth));
        when(waitingRepository.countByUserIdAndFestivalIdAndStatusIn(
                userId, festivalId, List.of(WaitingStatus.WAITING, WaitingStatus.CALLED))).thenReturn(0L);
        when(waitingRepository.existsByBoothIdAndUserIdAndFestivalIdAndStatusIn(
                boothId, userId, festivalId, List.of(WaitingStatus.WAITING, WaitingStatus.CALLED))).thenReturn(false);
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

    // ── getActiveWaitings ─────────────────────────────────────────────────────

    @Test
    void returnsActiveWaitingsForManagedBoothInRegistrationOrder() {
        Festival festival = festival();
        Booth booth = managedNightBooth(festival);
        Waiting first = waiting(festival, booth, "alice123");
        Waiting second = waiting(festival, booth, "bob123");
        second.call();
        AuthenticatedUser manager = manager(festival);

        when(boothRepository.findById(booth.getId())).thenReturn(Optional.of(booth));
        when(waitingRepository.findByBoothIdAndStatusInOrderByRegisteredAtAsc(
                booth.getId(), List.of(WaitingStatus.WAITING, WaitingStatus.CALLED)))
                .thenReturn(List.of(first, second));

        List<WaitingDTO.Response> response = waitingService.getActiveWaitings(manager, booth.getId());

        assertThat(response).extracting(WaitingDTO.Response::status)
                .containsExactly(WaitingStatus.WAITING, WaitingStatus.CALLED);
        verify(boothAuthorizationService).assertCanManageBooth(manager, booth);
    }

    // ── callWaiting ───────────────────────────────────────────────────────────

    @Test
    void callingWaitingMovesItToCalledAndIncrementsCallCount() {
        Festival festival = festival();
        Booth booth = managedNightBooth(festival);
        Waiting waiting = waiting(festival, booth, "alice123");
        AuthenticatedUser manager = manager(festival);

        when(waitingRepository.findById(waiting.getId())).thenReturn(Optional.of(waiting));

        WaitingDTO.Response response = waitingService.callWaiting(manager, waiting.getId());

        assertThat(response.status()).isEqualTo(WaitingStatus.CALLED);
        assertThat(response.callCount()).isEqualTo((short) 1);
        verify(boothAuthorizationService).assertCanManageBooth(manager, booth);
        verify(waitingNotificationService).enqueueCalled(waiting);
    }

    @Test
    void recallingCalledWaitingIncrementsCallCountAgain() {
        Festival festival = festival();
        Booth booth = managedNightBooth(festival);
        Waiting waiting = waiting(festival, booth, "alice123");
        waiting.call();

        when(waitingRepository.findById(waiting.getId())).thenReturn(Optional.of(waiting));

        WaitingDTO.Response response = waitingService.callWaiting(manager(festival), waiting.getId());

        assertThat(response.status()).isEqualTo(WaitingStatus.CALLED);
        assertThat(response.callCount()).isEqualTo((short) 2);
        verify(waitingNotificationService).enqueueCalled(waiting);
    }

    @Test
    void rejectsCallingCompletedWaiting() {
        Festival festival = festival();
        Booth booth = managedNightBooth(festival);
        Waiting waiting = waiting(festival, booth, "alice123");
        waiting.call();
        waiting.seat();

        when(waitingRepository.findById(waiting.getId())).thenReturn(Optional.of(waiting));

        assertThatThrownBy(() -> waitingService.callWaiting(manager(festival), waiting.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    // ── updateWaitingStatus ───────────────────────────────────────────────────

    @Test
    void seatsOnlyCalledWaiting() {
        Festival festival = festival();
        Booth booth = managedNightBooth(festival);
        Waiting waiting = waiting(festival, booth, "alice123");
        waiting.call();

        when(waitingRepository.findById(waiting.getId())).thenReturn(Optional.of(waiting));

        WaitingDTO.Response response = waitingService.updateWaitingStatus(
                manager(festival), waiting.getId(), new WaitingDTO.StatusRequest(WaitingStatus.SEATED));

        assertThat(response.status()).isEqualTo(WaitingStatus.SEATED);
    }

    @Test
    void rejectsSeatingWaitingBeforeItIsCalled() {
        Festival festival = festival();
        Booth booth = managedNightBooth(festival);
        Waiting waiting = waiting(festival, booth, "alice123");

        when(waitingRepository.findById(waiting.getId())).thenReturn(Optional.of(waiting));

        assertThatThrownBy(() -> waitingService.updateWaitingStatus(
                manager(festival), waiting.getId(), new WaitingDTO.StatusRequest(WaitingStatus.SEATED)))
                .isInstanceOf(BadRequestException.class);
    }

    // ── updateWaitingOpenStatus ───────────────────────────────────────────────

    @Test
    void opensAndClosesWaitingForManagedNightBooth() {
        Festival festival = festival();
        Booth booth = managedNightBooth(festival);
        booth.closeWaiting();
        AuthenticatedUser manager = manager(festival);

        when(boothRepository.findById(booth.getId())).thenReturn(Optional.of(booth));

        assertThat(waitingService.updateWaitingOpenStatus(
                manager, booth.getId(), new WaitingDTO.OpenStatusRequest(true)).isWaitingOpen()).isTrue();
        assertThat(waitingService.updateWaitingOpenStatus(
                manager, booth.getId(), new WaitingDTO.OpenStatusRequest(false)).isWaitingOpen()).isFalse();
    }

    @Test
    void rejectsWaitingOpenStatusChangeForDayBooth() {
        Festival festival = festival();
        Booth booth = new Booth("day booth", BoothCategory.INFO, BoothType.DAY);
        ReflectionTestUtils.setField(booth, "id", UUID.randomUUID());

        when(boothRepository.findById(booth.getId())).thenReturn(Optional.of(booth));

        assertThatThrownBy(() -> waitingService.updateWaitingOpenStatus(
                manager(festival), booth.getId(), new WaitingDTO.OpenStatusRequest(true)))
                .isInstanceOf(BadRequestException.class);
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

    private Festival festival() {
        Festival festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
        return festival;
    }

    private Booth openNightBooth() {
        Booth booth = new Booth("booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        ReflectionTestUtils.setField(booth, "id", UUID.randomUUID());
        booth.openWaiting();
        return booth;
    }

    private Booth managedNightBooth(Festival festival) {
        Booth booth = openNightBooth();
        User manager = new User(festival, "manager1", "hashed-password", "manager", "01011112222");
        manager.changeRole(UserRole.BOOTH_MANAGER);
        booth.assignManager(manager);
        return booth;
    }

    private Waiting waiting(Festival festival, Booth booth, String userId) {
        User user = new User(festival, userId, "hashed-password", "nickname", "01012345678");
        Waiting waiting = new Waiting(booth, user, (short) 2);
        ReflectionTestUtils.setField(waiting, "id", UUID.randomUUID());
        return waiting;
    }

    private AuthenticatedUser manager(Festival festival) {
        return new AuthenticatedUser("manager1", festival.getId(), UserRole.BOOTH_MANAGER);
    }
}
