package com.gym.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkloadMessagePublisherTest {

    private static final String QUEUE = "trainer.workload.queue";

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private WorkloadMessagePostProcessor postProcessor;

    @InjectMocks
    private WorkloadMessagePublisher publisher;

    private WorkloadRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "queue", QUEUE);
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
    @DisplayName("publishes the request to the configured queue with the cross-cutting post-processor")
    void publish_SendsToQueueWithPostProcessor() {
        publisher.publish(request);

        verify(jmsTemplate).convertAndSend(eq(QUEUE), eq(request), eq(postProcessor));
    }
}
