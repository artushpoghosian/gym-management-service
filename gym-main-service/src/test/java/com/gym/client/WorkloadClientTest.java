package com.gym.client;

import com.gym.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private JwtService jwtService;

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
    @DisplayName("send() sets the service JWT as Authorization Bearer header")
    void send_setsAuthorizationHeader() {
        when(jwtService.generateServiceToken()).thenReturn("service-token");

        workloadClient.send(request);

        ArgumentCaptor<HttpEntity<WorkloadRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(
                eq("http://trainer-workload-service/api/trainer-workload"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(Void.class));

        HttpHeaders headers = captor.getValue().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer service-token");
        assertThat(captor.getValue().getBody()).isEqualTo(request);
    }

    @Test
    @DisplayName("send() propagates the exception so the circuit breaker can count it")
    void send_whenHttpCallFails_throws() {
        when(jwtService.generateServiceToken()).thenReturn("service-token");
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> workloadClient.send(request))
                .isInstanceOf(ResourceAccessException.class);
    }

    @Test
    @DisplayName("sendFallback() swallows the failure — no exception propagates")
    void sendFallback_doesNotThrow() {
        assertThatCode(() ->
                workloadClient.sendFallback(request, new ResourceAccessException("Connection refused")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a read timeout is handled by the fallback, not surfaced to the caller")
    void readTimeout_isHandledByFallback() {
        ResourceAccessException timeout =
                new ResourceAccessException("Read timed out", new java.net.SocketTimeoutException("Read timed out"));

        assertThatThrownBy(() -> { throw timeout; })
                .isInstanceOf(ResourceAccessException.class)
                .hasRootCauseInstanceOf(java.net.SocketTimeoutException.class);

        assertThatCode(() -> workloadClient.sendFallback(request, timeout))
                .doesNotThrowAnyException();
    }
}
