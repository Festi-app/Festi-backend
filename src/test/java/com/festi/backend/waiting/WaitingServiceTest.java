package com.festi.backend.waiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothType;
import com.festi.backend.user.User;
import java.util.List;
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

    private WaitingService waitingService;

    @BeforeEach
    void setUp() {
        waitingService = new WaitingService(waitingRepository);
    }

    @Test
    void readsCurrentUsersWaitingsInRepositoryOrder() {
        UUID userId = UUID.randomUUID();
        User user = new User("user@example.com", "hash", "user", "01012345678");
        Booth booth = new Booth("booth", BoothCategory.ALCOHOL, BoothType.NIGHT, user);
        Waiting waiting = new Waiting(booth, user, (short) 2);
        when(waitingRepository.findByUserIdOrderByRegisteredAtDesc(userId)).thenReturn(List.of(waiting));

        List<WaitingDTO.Response> response = waitingService.getMyWaitings(userId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().boothSummary().name()).isEqualTo("booth");
    }
}
