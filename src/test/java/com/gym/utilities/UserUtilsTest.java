package com.gym.utilities;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUtilsTest {

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private TraineeDao traineeDao;

    @InjectMocks
    private UserUtils userUtils;

    @Test
    void generateUsername_ShouldConcatenateFirstAndLastName_WithDot() {
        String username = userUtils.generateUsername("John", "Smith");

        assertThat(username).isEqualTo("john.smith");
    }

    @Test
    void generateUsername_ShouldBeLowercase() {
        String username = userUtils.generateUsername("ALICE", "BROWN");

        assertThat(username).isEqualTo("alice.brown");
    }

    @Test
    void generateUsername_WhenDuplicateExistsInTrainers_ShouldAppendSuffix() {
        Trainer existing = new Trainer();
        existing.setUsername("john.smith");
        when(trainerDao.findAll()).thenReturn(List.of(existing));

        String username = userUtils.generateUsername("John", "Smith");

        assertThat(username).isEqualTo("john.smith1");
    }

    @Test
    void generateUsername_WhenDuplicateExistsInTrainees_ShouldAppendSuffix() {
        Trainee existing = new Trainee();
        existing.setUsername("john.smith");
        when(traineeDao.findAll()).thenReturn(List.of(existing));

        String username = userUtils.generateUsername("John", "Smith");

        assertThat(username).isEqualTo("john.smith1");
    }

    @Test
    void generateUsername_WhenMultipleDuplicatesExist_ShouldIncrementSuffix() {
        Trainer t1 = new Trainer();
        t1.setUsername("john.smith");
        Trainer t2 = new Trainer();
        t2.setUsername("john.smith1");
        when(trainerDao.findAll()).thenReturn(List.of(t1, t2));

        String username = userUtils.generateUsername("John", "Smith");

        assertThat(username).isEqualTo("john.smith2");
    }

    @Test
    void generatePassword_ShouldBe10CharactersLong() {
        String password = userUtils.generatePassword();

        assertThat(password).hasSize(10);
    }

    @Test
    void generatePassword_ShouldBeRandom_EachCall() {
        String p1 = userUtils.generatePassword();
        String p2 = userUtils.generatePassword();

        assertThat(p1).isNotEqualTo(p2);
    }

    @Test
    void generatePassword_ShouldContainOnlyHexCharacters() {
        String password = userUtils.generatePassword();

        assertThat(password).matches("[a-f0-9]{10}");
    }
}