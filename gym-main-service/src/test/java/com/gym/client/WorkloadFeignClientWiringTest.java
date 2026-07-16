package com.gym.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "eureka.client.enabled=false"
})
class WorkloadFeignClientWiringTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void feignProxyBeanIsCreated() {
        WorkloadFeignClient client = context.getBean(WorkloadFeignClient.class);
        assertThat(client).isNotNull();
    }
}
