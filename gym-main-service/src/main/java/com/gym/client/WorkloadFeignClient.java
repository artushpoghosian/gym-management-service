package com.gym.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "trainer-workload-service")
public interface WorkloadFeignClient {

    @PostMapping("/api/trainer-workload")
    void sendWorkload(@RequestBody WorkloadRequest request);
}
