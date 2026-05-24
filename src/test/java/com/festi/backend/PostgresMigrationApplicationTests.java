package com.festi.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.notification.WaitingNotificationEventRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@Tag("postgres")
@SpringBootTest
@ActiveProfiles("postgres-test")
class PostgresMigrationApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WaitingNotificationEventRepository notificationEventRepository;

    @Test
    void appliesFlywayMigrationsAndValidatesSchema() {
        Integer appliedMigrationCount = jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where success = true",
            Integer.class
        );
        String phoneNullable = jdbcTemplate.queryForObject(
            """
            select is_nullable
            from information_schema.columns
            where table_schema = 'public'
              and table_name = 'users'
              and column_name = 'phone'
            """,
            String.class
        );
        Integer notificationTableCount = jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where table_schema = 'public'
              and table_name in ('push_subscriptions', 'waiting_notification_events', 'push_notification_deliveries')
            """,
            Integer.class
        );
        Integer outboxColumnCount = jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.columns
            where table_schema = 'public'
              and table_name = 'waiting_notification_events'
              and column_name in ('status', 'attempt_count', 'available_at', 'processing_started_at', 'processed_at', 'failure_reason')
            """,
            Integer.class
        );

        assertThat(appliedMigrationCount).isGreaterThan(0);
        assertThat(phoneNullable).isEqualTo("NO");
        assertThat(notificationTableCount).isEqualTo(3);
        assertThat(outboxColumnCount).isEqualTo(6);
    }

    @Test
    void outboxClaimQueryCanRunAgainstPostgresEnumStatus() {
        assertThat(notificationEventRepository.findNextAvailableForUpdate(OffsetDateTime.now())).isEmpty();
    }
}
