package com.gym.client;

import com.gym.security.JwtService;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkloadMessagePostProcessor implements MessagePostProcessor {

    private static final String JWT_PROPERTY = "jwtToken";
    private static final String TRANSACTION_ID_PROPERTY = "transactionId";

    private final JwtService jwtService;

    @Override
    public Message postProcessMessage(Message message) throws JMSException {
        message.setStringProperty(JWT_PROPERTY, jwtService.generateServiceToken());

        String transactionId = MDC.get(TRANSACTION_ID_PROPERTY);
        if (transactionId != null) {
            message.setStringProperty(TRANSACTION_ID_PROPERTY, transactionId);
        }
        return message;
    }
}
