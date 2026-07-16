package com.gym.client;

import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkloadClientTest {

    @Mock
    private WorkloadFeignClient workloadFeignClient;

    @InjectMocks
    private WorkloadClient workloadClient;

    private WorkloadRequest request;

    @BeforeEach
    void setUp() {
        request = WorkloadRequest.builder()
                .trainerUsername("trainer.jane")
                .trainerFirstName("Jane")
                .trainerLastName("Doe")
                .isActive(true)
                .trainingDate(LocalDate.of(2026, 7, 10))
                .trainingDurationMinutes(60)
                .actionType(ActionType.ADD)
                .build();
    }

    @Test
    @DisplayName("send() delegates to the Feign client with the same request body")
    void send_DelegatesToFeignClient() {
        workloadClient.send(request);

        verify(workloadFeignClient).sendWorkload(request);
    }

    @Test
    @DisplayName("send() propagates Feign failures so the circuit breaker can count them")
    void send_WhenFeignCallFails_Throws() {
        FeignException serviceDown = mock(FeignException.class);
        doThrow(serviceDown).when(workloadFeignClient).sendWorkload(any(WorkloadRequest.class));

        assertThatThrownBy(() -> workloadClient.send(request))
                .isInstanceOf(FeignException.class);
    }

    @Test
    @DisplayName("send() propagates timeout-shaped failures (RetryableException)")
    void send_WhenCallTimesOut_Throws() {
        RetryableException timeout = mock(RetryableException.class);
        doThrow(timeout).when(workloadFeignClient).sendWorkload(any(WorkloadRequest.class));

        assertThatThrownBy(() -> workloadClient.send(request))
                .isInstanceOf(RetryableException.class);
    }

    @Test
    @DisplayName("sendFallback() swallows the failure — no exception propagates")
    void sendFallback_DoesNotThrow() {
        assertThatCode(() -> workloadClient.sendFallback(request, mock(FeignException.class)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendFallback() also swallows timeout failures")
    void sendFallback_SwallowsTimeout() {
        assertThatCode(() -> workloadClient.sendFallback(request, mock(RetryableException.class)))
                .doesNotThrowAnyException();
    }
}
