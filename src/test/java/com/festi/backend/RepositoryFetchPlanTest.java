package com.festi.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.booth.BoothType;
import com.festi.backend.location.BoothLocationRepository;
import com.festi.backend.waiting.WaitingRepository;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

class RepositoryFetchPlanTest {

    @Test
    void boothLocationReadQueriesFetchBoothWithTheRootQuery() throws NoSuchMethodException {
        Method byDayAndType = BoothLocationRepository.class.getMethod(
                "findByDayAndTypeOrderByIndex",
                LocalDate.class,
                BoothType.class
        );
        Method byDay = BoothLocationRepository.class.getMethod(
                "findByDayOrderByIndex",
                LocalDate.class
        );

        assertFetchesBooth(byDayAndType);
        assertFetchesBooth(byDay);
    }

    @Test
    void waitingReadQueryFetchesBoothWithTheRootQuery() throws NoSuchMethodException {
        Method method = WaitingRepository.class.getMethod(
                "findByUserIdAndFestivalIdOrderByRegisteredAtDesc",
                String.class,
                UUID.class
        );

        assertFetchesBooth(method);
    }

    private void assertFetchesBooth(Method method) {
        EntityGraph entityGraph = method.getAnnotation(EntityGraph.class);

        assertThat(entityGraph).isNotNull();
        assertThat(entityGraph.attributePaths()).containsExactly("booth");
    }
}
