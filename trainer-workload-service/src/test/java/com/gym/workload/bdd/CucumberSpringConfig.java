package com.gym.workload.bdd;

import com.gym.workload.TrainerWorkloadApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TrainerWorkloadApplication.class)
@ActiveProfiles("cucumber")
public class CucumberSpringConfig {

    static final MongoDBContainer MONGO =
            new MongoDBContainer(DockerImageName.parse("mongo:7"));

    static final GenericContainer<?> ACTIVEMQ =
            new GenericContainer<>(DockerImageName.parse("apache/activemq-classic:latest"))
                    .withExposedPorts(61616)
                    .waitingFor(Wait.forListeningPort());

    static {
        MONGO.start();
        ACTIVEMQ.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> MONGO.getReplicaSetUrl("trainer_workload"));

        registry.add("spring.activemq.broker-url",
                () -> "tcp://" + ACTIVEMQ.getHost() + ":" + ACTIVEMQ.getMappedPort(61616));

        registry.add("jwt.secret", () -> "0".repeat(64));
    }
}
