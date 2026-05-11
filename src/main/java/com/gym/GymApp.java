package com.gym;

import com.gym.config.AppConfig;
import com.gym.facade.GymFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Slf4j
public class GymApp {

    public static void main(String[] args) {
        log.info("Starting Gym CRM application...");

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AppConfig.class)) {

            GymFacade facade = context.getBean(GymFacade.class);

            log.info("All trainees: {}", facade.findAllTrainees().size());
            log.info("All trainers: {}", facade.findAllTrainers().size());
            log.info("All trainings: {}", facade.findAllTrainings().size());
        }

        log.info("Gym CRM application finished.");
    }
}