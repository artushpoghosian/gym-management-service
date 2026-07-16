package com.gym.client;

import com.gym.security.JwtService;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignAuthInterceptorTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private FeignAuthInterceptor interceptor;

    private RequestTemplate template;

    @BeforeEach
    void setUp() {
        template = new RequestTemplate();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("adds Authorization Bearer header with a fresh service token")
    void apply_SetsAuthorizationHeader() {
        when(jwtService.generateServiceToken()).thenReturn("service-token");

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization"))
                .containsExactly("Bearer service-token");
        verify(jwtService).generateServiceToken();
    }

    @Test
    @DisplayName("forwards X-Transaction-Id when present in the MDC")
    void apply_ForwardsTransactionId_WhenPresentInMdc() {
        when(jwtService.generateServiceToken()).thenReturn("service-token");
        MDC.put("transactionId", "tx-123");

        interceptor.apply(template);

        assertThat(template.headers().get("X-Transaction-Id"))
                .containsExactly("tx-123");
    }

    @Test
    @DisplayName("omits X-Transaction-Id when the MDC is empty")
    void apply_OmitsTransactionId_WhenMdcEmpty() {
        when(jwtService.generateServiceToken()).thenReturn("service-token");

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("X-Transaction-Id");
        assertThat(template.headers()).containsKey("Authorization");
    }
}
