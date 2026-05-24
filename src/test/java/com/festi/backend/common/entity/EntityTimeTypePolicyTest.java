package com.festi.backend.common.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.booth.Booth;
import com.festi.backend.booth.BoothApplication;
import com.festi.backend.favorite.Favorite;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalDay;
import com.festi.backend.festival.Notice;
import com.festi.backend.festival.Timeline;
import com.festi.backend.location.BoothLocation;
import com.festi.backend.menu.MenuItem;
import com.festi.backend.user.User;
import com.festi.backend.waiting.Waiting;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

class EntityTimeTypePolicyTest {

    private static final List<Class<?>> ENTITY_TIME_OWNERS = List.of(
            BaseTimeEntity.class,
            User.class,
            Festival.class,
            FestivalDay.class,
            Notice.class,
            Timeline.class,
            Booth.class,
            BoothApplication.class,
            BoothLocation.class,
            MenuItem.class,
            Waiting.class,
            Favorite.class
    );

    @Test
    void everyAuditingEntityFieldUsesOffsetDateTime() {
        List<DeclaredField> auditingFields = ENTITY_TIME_OWNERS.stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredFields())
                        .map(field -> new DeclaredField(type, field)))
                .filter(declared -> isAuditingField(declared.field()))
                .toList();
        List<String> mismatches = auditingFields.stream()
                .filter(declared -> declared.field().getType() != OffsetDateTime.class)
                .map(declared -> declared.owner().getSimpleName() + "." + declared.field().getName()
                        + " uses " + declared.field().getType().getSimpleName())
                .toList();

        assertThat(auditingFields)
                .as("Auditing fields must be discovered from the current entity mappings")
                .isNotEmpty();
        assertThat(mismatches)
                .as("All auditing fields in persisted entity mappings must use OffsetDateTime")
                .isEmpty();
    }

    private boolean isAuditingField(Field field) {
        return field.isAnnotationPresent(CreatedDate.class)
                || field.isAnnotationPresent(LastModifiedDate.class);
    }

    private record DeclaredField(Class<?> owner, Field field) {
    }
}
