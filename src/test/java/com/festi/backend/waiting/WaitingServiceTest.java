package com.festi.backend.waiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothRepository;
import com.festi.backend.booth.BoothType;
import com.festi.backend.common.exception.BadRequestException;
import com.festi.backend.common.exception.ConflictException;
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

    @BeforeEach
    void setUp() {
        waitingService = new WaitingService(
                waitingRepository, boothRepository, userRepository, boothAuthorizationService, waitingNotificationService);
    }

    @Test
    void readsCurrentUsersWaitingsInRepositoryOrder() {
        Festival festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
        User user = new User(festival, "alice123", "hashed-password", "nickname", "01012345678");

        Booth booth = new Booth("booth", BoothCategory.ALCOHOL, BoothType.NIGHT);
        ReflectionTestUtils.setField(booth, "id", UUID.randomUUID());
        Waiting waiting = new Waiting(booth, user, (short) 2);

        when(waitingRepository.findByUserIdAndFestivalIdOrderByRegisteredAtDesc("alice123", festival.getId()))
                .thenReturn(List.of(waiting));

        List<WaitingDTO.Response> response = waitingService.getMyWaitings("alice123", festival.getId());

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().boothSummary().name()).isEqualTo("booth");
    }

    @Test
    void rejectsRegistrationWhenUserAlreadyHasThreeActiveWaitings() {
        Festival festival = festival();
        Booth booth = openNightBooth();
        String userId = "alice123";

        when(boothRepository.findById(booth.getId())).thenReturn(Optional.of(booth));
        when(waitingRepository.countByUserIdAndFestivalIdAndStatusIn(
                userId, festival.getId(), List.of(WaitingStatus.WAITING, WaitingStatus.CALLED)))
                .thenReturn(3L);

        assertThatThrownBy(() -> waitingService.registerWaiting(
                userId, festival.getId(), booth.getId(), (short) 2))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsRegistrationWhenUserAlreadyHasActiveWaitingAtSameBooth() {
        Festival festival = festival();
        Booth booth = openNightBooth();
        String userId = "alice123";

        when(boothRepository.findById(booth.getId())).thenReturn(Optional.of(booth));
        when(waitingRepository.countByUserIdAndFestivalIdAndStatusIn(
                userId, festival.getId(), List.of(WaitingStatus.WAITING, WaitingStatus.CALLED)))
                .thenReturn(1L);
        when(waitingRepository.existsByBoothIdAndUserIdAndFestivalIdAndStatusIn(
                booth.getId(), userId, festival.getId(), List.of(WaitingStatus.WAITING, WaitingStatus.CALLED)))
                .thenReturn(true);

        assertThatThrownBy(() -> waitingService.registerWaiting(
                userId, festival.getId(), booth.getId(), (short) 2))
                .isInstanceOf(ConflictException.class);
    }

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
