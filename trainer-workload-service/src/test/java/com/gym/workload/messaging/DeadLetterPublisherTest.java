package com.gym.workload.messaging;

import com.gym.workload.dto.ActionType;
import com.gym.workload.dto.WorkloadRequest;
import jakarta.jms.Message;
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
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeadLetterPublisherTest {

    private static final String SOURCE_QUEUE = "trainer.workload.queue";
    private static final String DLQ = "trainer.workload.dlq";
    private static final String REASON = "trainerUsername: must not be blank";

    @Mock
    private JmsTemplate jmsTemplate;

    @InjectMocks
    private DeadLetterPublisher deadLetterPublisher;

    private WorkloadRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(deadLetterPublisher, "sourceQueue", SOURCE_QUEUE);
        ReflectionTestUtils.setField(deadLetterPublisher, "dlq", DLQ);
        request = new WorkloadRequest();
        request.setActionType(ActionType.ADD);
        request.setTrainingDate(LocalDate.of(2026, 7, 10));
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    private MessagePostProcessor captureSend() {
        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq(DLQ), eq(request), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("sends the offending request to the dead-letter queue")
    void send_RoutesToDlq() {
        deadLetterPublisher.send(request, REASON);

        captureSend();
    }

    @Test
    @DisplayName("stamps dlqReason and originalQueue diagnostic properties")
    void send_StampsDiagnosticProperties() throws Exception {
        deadLetterPublisher.send(request, REASON);

        Message message = mock(Message.class);
        captureSend().postProcessMessage(message);
        verify(message).setStringProperty("dlqReason", REASON);
        verify(message).setStringProperty("originalQueue", SOURCE_QUEUE);
    }

    @Test
    @DisplayName("propagates the transactionId when present in the MDC")
    void send_StampsTransactionId_WhenInMdc() throws Exception {
        MDC.put("transactionId", "tx-123");

        deadLetterPublisher.send(request, REASON);

        Message message = mock(Message.class);
        captureSend().postProcessMessage(message);
        verify(message).setStringProperty("transactionId", "tx-123");
    }

    @Test
    @DisplayName("omits the transactionId property when the MDC is empty")
    void send_OmitsTransactionId_WhenMdcEmpty() throws Exception {
        deadLetterPublisher.send(request, REASON);

        Message message = mock(Message.class);
        captureSend().postProcessMessage(message);
        verify(message, never()).setStringProperty(eq("transactionId"), org.mockito.ArgumentMatchers.anyString());
    }
}
