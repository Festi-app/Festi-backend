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
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

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
    void everyTemporalEntityFieldUsesOffsetDateTime() {
        List<String> mismatches = ENTITY_TIME_OWNERS.stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredFields())
                        .map(field -> new DeclaredField(type, field)))
                .filter(declared -> TemporalAccessor.class.isAssignableFrom(declared.field().getType()))
                .filter(declared -> declared.field().getType() != OffsetDateTime.class)
                .map(declared -> declared.owner().getSimpleName() + "." + declared.field().getName()
                        + " uses " + declared.field().getType().getSimpleName())
                .toList();

        assertThat(mismatches)
                .as("All temporal fields in persisted entity mappings must use OffsetDateTime")
                .isEmpty();
    }

    private record DeclaredField(Class<?> owner, Field field) {
    }
}
