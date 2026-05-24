package com.festi.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.festival.Festival;
import com.festi.backend.user.User;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@ActiveProfiles("test")
class JpaAuditingConfigTest {

    @Autowired
    private AuditingHandler auditingHandler;

    @Test
    void marksNewSignupUserWithOffsetDateTimes() {
        Festival festival = new Festival(
                "Festi", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20), "desc");
        ReflectionTestUtils.setField(festival, "id", UUID.randomUUID());
        User user = new User(festival, "alice123", "hashed-password", "nickname", "01012345678");

        auditingHandler.markCreated(user);

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(user.getUpdatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }
}
