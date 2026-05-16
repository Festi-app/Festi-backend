package com.festi.backend;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void appliesFlywayMigrationsAndValidatesSchema() {
        Integer appliedMigrationCount = jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where success = true",
            Integer.class
        );
        Boolean boothAdminAssignmentsExists = jdbcTemplate.queryForObject(
            "select to_regclass('public.booth_admin_assignments') is not null",
            Boolean.class
        );

        assertThat(appliedMigrationCount).isGreaterThan(0);
        assertThat(boothAdminAssignmentsExists).isTrue();
    }
}
