package com.gym.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "trainers")
@PrimaryKeyJoinColumn(name = "user_id")
public class Trainer extends User{

    @NotNull(message = "Specialization is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "specialization_id", nullable = false)
    private TrainingType specialization;
}
