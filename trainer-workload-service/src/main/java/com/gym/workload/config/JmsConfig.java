package com.gym.workload.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.workload.dto.WorkloadRequest;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.Map;

@Configuration
@EnableJms
public class JmsConfig {

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setObjectMapper(objectMapper);
        converter.setTypeIdPropertyName("_type");
        converter.setTypeIdMappings(Map.of("workloadRequest", WorkloadRequest.class));
        return converter;
    }

    /**
     * Redeliver a failing message a couple of times, then let the broker move it to
     * ActiveMQ.DLQ. This is the safety net for poison messages our app-level validation
     * can't see (e.g. undeserializable payloads or unexpected processing errors).
     */
    @Bean
    public ActiveMQConnectionFactoryCustomizer redeliveryPolicyCustomizer() {
        return factory -> {
            RedeliveryPolicy policy = new RedeliveryPolicy();
            policy.setMaximumRedeliveries(2);
            policy.setInitialRedeliveryDelay(500);
            policy.setRedeliveryDelay(500);
            factory.setRedeliveryPolicy(policy);
        };
    }

    /**
     * Transacted listener: on an uncaught exception the session rolls back so the broker
     * redelivers (and, once the redelivery limit is hit, dead-letters). Preserves Boot's
     * default configuration (incl. the message converter above).
     */
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setSessionTransacted(true);
        return factory;
    }
}
