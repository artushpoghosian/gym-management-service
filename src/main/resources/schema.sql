CREATE TABLE IF NOT EXISTS users (
                                     id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    username   VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS training_types (
                                              id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              training_type_name VARCHAR(100) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS trainers (
                                        user_id           BIGINT PRIMARY KEY,
                                        specialization_id VARCHAR(50) NOT NULL,
    CONSTRAINT fk_trainer_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS trainees (
                                        user_id       BIGINT PRIMARY KEY,
                                        date_of_birth DATE,
                                        address       VARCHAR(255),
    CONSTRAINT fk_trainee_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS trainee_trainer (
                                               trainee_id BIGINT NOT NULL,
                                               trainer_id BIGINT NOT NULL,
                                               PRIMARY KEY (trainee_id, trainer_id),
    CONSTRAINT fk_tt_trainee FOREIGN KEY (trainee_id) REFERENCES trainees(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_tt_trainer FOREIGN KEY (trainer_id) REFERENCES trainers(user_id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS trainings (
                                         id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         trainer_id        BIGINT       NOT NULL,
                                         trainee_id        BIGINT       NOT NULL,
                                         training_name     VARCHAR(255) NOT NULL,
    training_type     VARCHAR(50)  NOT NULL,
    training_date     DATE         NOT NULL,
    training_duration INT          NOT NULL,
    CONSTRAINT fk_training_trainer FOREIGN KEY (trainer_id) REFERENCES trainers(user_id),
    CONSTRAINT fk_training_trainee FOREIGN KEY (trainee_id) REFERENCES trainees(user_id)
    );