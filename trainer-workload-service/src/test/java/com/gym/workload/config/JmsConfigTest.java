package com.gym.workload.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class JmsConfigTest {

    private final JmsConfig config = new JmsConfig();

    @Test
    void jacksonConverter_IsMappingJacksonConverter() {
        MessageConverter converter = config.jacksonJmsMessageConverter(new ObjectMapper());

        assertThat(converter).isInstanceOf(MappingJackson2MessageConverter.class);
    }

    @Test
    void redeliveryPolicy_LimitsRedeliveriesToTwo() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();

        config.redeliveryPolicyCustomizer().customize(factory);

        assertThat(factory.getRedeliveryPolicy().getMaximumRedeliveries()).isEqualTo(2);
    }
}
