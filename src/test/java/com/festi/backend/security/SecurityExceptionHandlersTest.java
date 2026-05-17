package com.festi.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.festi.backend.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

class SecurityExceptionHandlersTest {

    @Test
    void accessDeniedHandlerUsesCommonErrorResponseShape() throws Exception {
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler(JsonMapper.builder().build());
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(ErrorCode.ACCESS_DENIED.getStatus().value());
        assertThat(response.getContentAsString()).contains("\"code\":\"ACCESS_DENIED\"");
    }
}
