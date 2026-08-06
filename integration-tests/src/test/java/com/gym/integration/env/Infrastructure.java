package com.gym.integration.env;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Infrastructure {

    private static final String JWT_SECRET = "0".repeat(64);
    private static final String QUEUE = "trainer.workload.queue";

    private static boolean started;
    private static String mainBaseUrl;
    private static String workloadBaseUrl;

    private Infrastructure() {
    }

    public static synchronized void start() {
        if (started) {
            return;
        }

        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
        postgres.start();

        MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7"));
        mongo.start();

        GenericContainer<?> activemq = new GenericContainer<>(DockerImageName.parse("apache/activemq-classic:latest"))
                .withExposedPorts(61616)
                .waitingFor(Wait.forListeningPort());
        activemq.start();

        String brokerUrl = "tcp://" + activemq.getHost() + ":" + activemq.getMappedPort(61616);

        ConfigurableApplicationContext mainCtx =
                new SpringApplicationBuilder(MainServiceLauncher.class)
                        .properties(mainProperties(postgres, brokerUrl))
                        .run();

        ConfigurableApplicationContext workloadCtx =
                new SpringApplicationBuilder(WorkloadServiceLauncher.class)
                        .properties(workloadProperties(mongo, brokerUrl))
                        .run();

        mainBaseUrl = "http://localhost:" + port(mainCtx);
        workloadBaseUrl = "http://localhost:" + port(workloadCtx);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mainCtx.close();
            workloadCtx.close();
            activemq.stop();
            mongo.stop();
            postgres.stop();
        }));

        started = true;
    }

    public static String mainBaseUrl() {
        return mainBaseUrl;
    }

    public static String workloadBaseUrl() {
        return workloadBaseUrl;
    }

    private static Map<String, Object> mainProperties(PostgreSQLContainer<?> postgres, String brokerUrl) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("server.port", 0);
        props.put("spring.application.name", "it-gym-main-service");
        // Don't load the bundled application.properties/yml from either service jar.
        props.put("spring.config.name", "it-none");
        props.put("spring.datasource.url", postgres.getJdbcUrl());
        props.put("spring.datasource.username", postgres.getUsername());
        props.put("spring.datasource.password", postgres.getPassword());
        props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        props.put("spring.jpa.hibernate.ddl-auto", "none");
        props.put("spring.sql.init.mode", "always");
        props.put("spring.jpa.defer-datasource-initialization", "true");
        props.put("spring.activemq.broker-url", brokerUrl);
        props.put("workload.queue", QUEUE);
        props.put("jwt.secret", JWT_SECRET);
        props.put("jwt.expiration-ms", "3600000");
        props.put("eureka.client.enabled", "false");
        return props;
    }

    private static Map<String, Object> workloadProperties(MongoDBContainer mongo, String brokerUrl) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("server.port", 0);
        props.put("spring.application.name", "it-trainer-workload-service");
        props.put("spring.config.name", "it-none");
        props.put("spring.data.mongodb.uri", mongo.getReplicaSetUrl("trainer_workload"));
        props.put("spring.data.mongodb.auto-index-creation", "true");
        props.put("spring.activemq.broker-url", brokerUrl);
        props.put("workload.queue", QUEUE);
        props.put("workload.dlq", "trainer.workload.dlq");
        props.put("jwt.secret", JWT_SECRET);
        props.put("eureka.client.enabled", "false");
        return props;
    }

    private static int port(ConfigurableApplicationContext ctx) {
        return Integer.parseInt(ctx.getEnvironment().getProperty("local.server.port", "0"));
    }
}
