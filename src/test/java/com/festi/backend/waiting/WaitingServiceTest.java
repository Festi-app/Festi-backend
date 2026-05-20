package com.festi.backend.waiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothCategory;
import com.festi.backend.booth.BoothType;
import com.festi.backend.festival.Festival;
import com.festi.backend.user.User;
import java.time.LocalDate;
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
}
