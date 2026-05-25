package com.festi.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.festival.Festival;
import com.festi.backend.security.AuthenticatedUser;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRepository;
import com.festi.backend.user.UserRole;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionServiceTest {

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;

    @Mock
    private UserRepository userRepository;

    private PushSubscriptionService pushSubscriptionService;

    private Festival festival;

    private User user;

    private AuthenticatedUser currentUser;

    @BeforeEach
    void setUp() {
        pushSubscriptionService = new PushSubscriptionService(pushSubscriptionRepository, userRepository);
        festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
        user = new User(festival, "alice123", "hashed", "Alice", "01012345678");
        currentUser = new AuthenticatedUser("alice123", festival.getId(), UserRole.USER);
    }

    @Test
    void registersNewSubscriptionForAuthenticatedUser() {
        PushSubscriptionDTO.Request request = request("endpoint-1", "p256dh-1", "auth-1");
        when(userRepository.findByIdAndFestivalId(currentUser.id(), currentUser.festivalId()))
                .thenReturn(Optional.of(user));
        when(pushSubscriptionRepository.findByEndpoint("endpoint-1")).thenReturn(Optional.empty());
        when(pushSubscriptionRepository.save(any(PushSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PushSubscriptionDTO.Response response = pushSubscriptionService.register(currentUser, request);

        ArgumentCaptor<PushSubscription> captor = ArgumentCaptor.forClass(PushSubscription.class);
        verify(pushSubscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getP256dhKey()).isEqualTo("p256dh-1");
        assertThat(response.endpoint()).isEqualTo("endpoint-1");
    }

    @Test
    void rebindsExistingEndpointToCurrentUserInsteadOfDuplicatingDeliveryTarget() {
        User previousUser = new User(festival, "bob123", "hashed", "Bob", "01000000000");
        PushSubscription existing = new PushSubscription(previousUser, "endpoint-1", "old-key", "old-auth");
        PushSubscriptionDTO.Request request = request("endpoint-1", "new-key", "new-auth");
        when(userRepository.findByIdAndFestivalId(currentUser.id(), currentUser.festivalId()))
                .thenReturn(Optional.of(user));
        when(pushSubscriptionRepository.findByEndpoint("endpoint-1")).thenReturn(Optional.of(existing));

        pushSubscriptionService.register(currentUser, request);

        assertThat(existing.getUser()).isEqualTo(user);
        assertThat(existing.getP256dhKey()).isEqualTo("new-key");
        assertThat(existing.getAuthKey()).isEqualTo("new-auth");
        verify(pushSubscriptionRepository, never()).save(any(PushSubscription.class));
    }

    @Test
    void removesOnlyCurrentUsersSubscription() {
        UUID subscriptionId = UUID.randomUUID();
        PushSubscription subscription = new PushSubscription(user, "endpoint-1", "key", "auth");
        when(pushSubscriptionRepository.findByIdAndUserIdAndFestivalId(
                subscriptionId, currentUser.id(), currentUser.festivalId()))
                .thenReturn(Optional.of(subscription));

        pushSubscriptionService.remove(currentUser, subscriptionId);

        verify(pushSubscriptionRepository).delete(subscription);
    }

    @Test
    void hidesSubscriptionOwnedByAnotherUserOnRemoval() {
        UUID subscriptionId = UUID.randomUUID();
        when(pushSubscriptionRepository.findByIdAndUserIdAndFestivalId(
                subscriptionId, currentUser.id(), currentUser.festivalId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pushSubscriptionService.remove(currentUser, subscriptionId))
                .isInstanceOf(NotFoundException.class);
    }

    private PushSubscriptionDTO.Request request(String endpoint, String p256dh, String auth) {
        return new PushSubscriptionDTO.Request(endpoint, new PushSubscriptionDTO.Keys(p256dh, auth));
    }
}
