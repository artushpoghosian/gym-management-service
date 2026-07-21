package com.gym.client;

import com.gym.security.JwtService;
import jakarta.jms.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadMessagePostProcessorTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private WorkloadMessagePostProcessor postProcessor;

    @BeforeEach
    void setUp() {
        MDC.clear();
        when(jwtService.generateServiceToken()).thenReturn("service-token");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("stamps the service JWT as a message property")
    void postProcess_StampsJwt() throws Exception {
        Message message = mock(Message.class);

        postProcessor.postProcessMessage(message);

        verify(message).setStringProperty("jwtToken", "service-token");
        verify(jwtService).generateServiceToken();
    }

    @Test
    @DisplayName("stamps the transactionId when present in the MDC")
    void postProcess_StampsTransactionId_WhenInMdc() throws Exception {
        MDC.put("transactionId", "tx-123");
        Message message = mock(Message.class);

        postProcessor.postProcessMessage(message);

        verify(message).setStringProperty("transactionId", "tx-123");
    }

    @Test
    @DisplayName("omits the transactionId when the MDC is empty")
    void postProcess_OmitsTransactionId_WhenMdcEmpty() throws Exception {
        Message message = mock(Message.class);

        postProcessor.postProcessMessage(message);

        verify(message, never()).setStringProperty(eq("transactionId"), anyString());
    }
}
