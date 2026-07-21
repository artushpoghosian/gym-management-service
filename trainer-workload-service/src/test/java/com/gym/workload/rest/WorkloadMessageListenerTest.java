package com.gym.workload.rest;

import com.gym.workload.dto.ActionType;
import com.gym.workload.dto.WorkloadRequest;
import com.gym.workload.messaging.DeadLetterPublisher;
import com.gym.workload.security.JwtService;
import com.gym.workload.service.WorkloadService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadMessageListenerTest {

    private static final String VALID_TOKEN = "valid-token";

    @Mock
    private WorkloadService workloadService;

    @Mock
    private JwtService jwtService;

    @Mock
    private jakarta.validation.Validator validator;

    @Mock
    private com.gym.workload.messaging.DeadLetterPublisher deadLetterPublisher;

    @InjectMocks
    private WorkloadMessageListener listener;

    private WorkloadRequest request;

    @BeforeEach
    void setUp() {
        request = new WorkloadRequest();
        request.setTrainerUsername("trainer.jane");
        request.setTrainerFirstName("Jane");
        request.setTrainerLastName("Doe");
        request.setActive(true);
        request.setTrainingDate(LocalDate.of(2026, 7, 10));
        request.setTrainingDurationMinutes(60);
        request.setActionType(ActionType.ADD);
        lenient().when(jwtService.isValid(VALID_TOKEN)).thenReturn(true);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("valid message → processed, DLQ not used")
    void onWorkloadMessage_ProcessesRequest_WhenJwtValid() {
        listener.onWorkloadMessage(request, VALID_TOKEN, "tx-123");

        verify(workloadService).process(request);
        verify(deadLetterPublisher, never()).send(any(), anyString());
    }

    @Test
    @DisplayName("missing required field → not processed, routed to the DLQ with a reason")
    void onWorkloadMessage_MissingRequiredField_RoutedToDlq() {
        Validator realValidator = Validation.buildDefaultValidatorFactory().getValidator();
        WorkloadMessageListener listenerWithRealValidator =
                new WorkloadMessageListener(workloadService, jwtService, realValidator, deadLetterPublisher);

        WorkloadRequest invalid = new WorkloadRequest();
        invalid.setActionType(ActionType.ADD);
        invalid.setTrainingDate(LocalDate.of(2026, 7, 10));
        invalid.setTrainingDurationMinutes(60);
        // trainerUsername / firstName / lastName left blank -> @NotBlank violations

        listenerWithRealValidator.onWorkloadMessage(invalid, VALID_TOKEN, "tx-123");

        verify(workloadService, never()).process(invalid);
        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(deadLetterPublisher).send(eq(invalid), reason.capture());
        assertThat(reason.getValue()).contains("trainerUsername");
    }

    @Test
    @DisplayName("rejects the message (no processing, no DLQ) when the JWT is invalid")
    void onWorkloadMessage_Rejects_WhenJwtInvalid() {
        when(jwtService.isValid("bad-token")).thenReturn(false);

        listener.onWorkloadMessage(request, "bad-token", "tx-123");

        verify(workloadService, never()).process(request);
        verify(deadLetterPublisher, never()).send(any(), anyString());
    }

    @Test
    @DisplayName("rejects the message when the JWT header is missing")
    void onWorkloadMessage_Rejects_WhenJwtMissing() {
        listener.onWorkloadMessage(request, null, "tx-123");

        verify(workloadService, never()).process(request);
    }

    @Test
    @DisplayName("sets the forwarded transactionId in the MDC during processing")
    void onWorkloadMessage_SetsForwardedTransactionId() {
        AtomicReference<String> mdcDuringProcessing = new AtomicReference<>();
        doAnswer(inv -> {
            mdcDuringProcessing.set(MDC.get("transactionId"));
            return null;
        }).when(workloadService).process(request);

        listener.onWorkloadMessage(request, VALID_TOKEN, "tx-123");

        assertThat(mdcDuringProcessing.get()).isEqualTo("tx-123");
    }

    @Test
    @DisplayName("generates a transactionId when the header is absent")
    void onWorkloadMessage_GeneratesTransactionId_WhenHeaderAbsent() {
        AtomicReference<String> mdcDuringProcessing = new AtomicReference<>();
        doAnswer(inv -> {
            mdcDuringProcessing.set(MDC.get("transactionId"));
            return null;
        }).when(workloadService).process(request);

        listener.onWorkloadMessage(request, VALID_TOKEN, null);

        assertThat(mdcDuringProcessing.get()).isNotBlank();
    }

    @Test
    @DisplayName("clears the MDC after processing")
    void onWorkloadMessage_ClearsMdcAfterProcessing() {
        listener.onWorkloadMessage(request, VALID_TOKEN, "tx-123");

        assertThat(MDC.get("transactionId")).isNull();
    }
}
