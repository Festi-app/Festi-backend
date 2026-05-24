package com.festi.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PushMessagePropertiesTest {

    @Autowired
    private PushMessageProperties properties;

    @Test
    void loadsCalledPushDisplayTemplateFromImportedConfiguration() {
        assertThat(properties.called().title()).isEqualTo("입장 안내");
        assertThat(properties.called().body()).isEqualTo("부스로 방문해 주세요.");
        assertThat(properties.called().icon()).isEqualTo("/icons/called.png");
    }

    @Test
    void pushTemplateExposesDisplayFieldsOnly() {
        assertThat(PushMessageProperties.PushMessageTemplate.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly("title", "body", "icon");
    }
}
