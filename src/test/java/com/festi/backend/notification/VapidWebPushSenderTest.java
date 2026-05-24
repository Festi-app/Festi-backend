package com.festi.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.festi.backend.festival.Festival;
import com.festi.backend.user.User;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.time.LocalDate;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class VapidWebPushSenderTest {

    private static final String P256DH =
            "BOtBVgsHVWXzwhDAoFE8P2IgQvabz_tuJjIlNacmS3XZ3fRDuVWiBp8bPR3vHCA78edquclcXXYb-olcj3QtIZ4=";
    private static final String AUTH = "IOScBh9LW5mJ_K2JwXyNqQ==";
    private static final String VAPID_PUBLIC_KEY =
            "BGgL7I82SAQM78oyGwaJdrQFhVfZqL9h4Y18BLtgJQ-9pSGXwxqAWQudqmcv41RcWgk1ssUeItv4-8khxbhYveM=";
    private static final String VAPID_PRIVATE_KEY = "ANlfcVVFB4JiMYcI74_h9h04QZ1Ks96AyEa1yrMgDwn3";

    @BeforeAll
    static void addBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    void mapsAcceptedPushResponseToSentResult() throws Exception {
        VapidWebPushSender sender = senderReturning(201);

        WebPushSender.SendResult result = sender.send(subscription(), payload());

        assertThat(result.successful()).isTrue();
        assertThat(result.responseStatus()).isEqualTo(201);
        assertThat(result.retryable()).isFalse();
    }

    @Test
    void treatsRateLimitAsRetryableFailure() throws Exception {
        VapidWebPushSender sender = senderReturning(429);

        WebPushSender.SendResult result = sender.send(subscription(), payload());

        assertThat(result.successful()).isFalse();
        assertThat(result.responseStatus()).isEqualTo(429);
        assertThat(result.retryable()).isTrue();
    }

    @Test
    void treatsGoneSubscriptionAsPermanentFailure() throws Exception {
        VapidWebPushSender sender = senderReturning(410);

        WebPushSender.SendResult result = sender.send(subscription(), payload());

        assertThat(result.successful()).isFalse();
        assertThat(result.responseStatus()).isEqualTo(410);
        assertThat(result.retryable()).isFalse();
    }

    @Test
    void selectedCryptoDependenciesCanPrepareEncryptedVapidRequest() throws Exception {
        PushService pushService = new PushService(
                VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, "mailto:admin@example.com");
        Notification notification = new Notification(
                "https://push.example.com/subscription/1",
                P256DH,
                AUTH,
                "{}".getBytes(StandardCharsets.UTF_8),
                300);

        HttpPost request = pushService.preparePost(notification, Encoding.AES128GCM);

        assertThat(request.getEntity().getContentLength()).isPositive();
        assertThat(request.getFirstHeader("Authorization")).isNotNull();
    }

    private VapidWebPushSender senderReturning(int statusCode) throws Exception {
        PushService pushService = mock(PushService.class);
        HttpResponse response = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(pushService.send(any(Notification.class))).thenReturn(response);
        return new VapidWebPushSender(pushService, new ObjectMapper(), 300);
    }

    private PushSubscription subscription() {
        Festival festival = new Festival("Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        User user = new User(festival, "alice123", "hashed", "Alice", "01012345678");
        return new PushSubscription(user, "https://push.example.com/subscription/1", P256DH, AUTH);
    }

    private PushNotificationPayload payload() {
        return new PushNotificationPayload("입장 안내", "부스로 방문해 주세요.", "/icons/called.png", "/waitings");
    }
}
