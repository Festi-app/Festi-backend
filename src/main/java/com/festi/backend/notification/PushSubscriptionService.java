package com.festi.backend.notification;

import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.security.AuthenticatedUser;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    @Transactional
    public PushSubscriptionDTO.Response register(AuthenticatedUser currentUser, PushSubscriptionDTO.Request request) {
        User user = userRepository.findByIdAndFestivalId(currentUser.id(), currentUser.festivalId())
                .orElseThrow(() -> new NotFoundException("User not found."));
        PushSubscription subscription = pushSubscriptionRepository.findByEndpoint(request.endpoint())
                .map(existing -> {
                    existing.rebind(user, request.keys().p256dh(), request.keys().auth());
                    return existing;
                })
                .orElseGet(() -> pushSubscriptionRepository.save(new PushSubscription(
                        user, request.endpoint(), request.keys().p256dh(), request.keys().auth())));
        return PushSubscriptionDTO.Response.from(subscription);
    }

    @Transactional
    public void remove(AuthenticatedUser currentUser, UUID subscriptionId) {
        PushSubscription subscription = pushSubscriptionRepository.findByIdAndUserIdAndFestivalId(
                        subscriptionId, currentUser.id(), currentUser.festivalId())
                .orElseThrow(() -> new NotFoundException("Push subscription not found."));
        pushSubscriptionRepository.delete(subscription);
    }
}
