package com.gym.utilities;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserUtilsTest {

    private final UserUtils userUtils = new UserUtils();

    @Test
    void generateUsername_ShouldConcatenateFirstAndLastName_WithDot() {
        String username = userUtils.generateUsername("John", "Smith", u -> false);
        assertThat(username).isEqualTo("john.smith");
    }

    @Test
    void generateUsername_ShouldBeLowercase() {
        String username = userUtils.generateUsername("ALICE", "BROWN", u -> false);
        assertThat(username).isEqualTo("alice.brown");
    }

    @Test
    void generateUsername_WhenBaseUsernameTaken_ShouldAddSuffix1() {
        String username = userUtils.generateUsername("John", "Smith", u -> u.equals("john.smith"));
        assertThat(username).isEqualTo("john.smith1");
    }

    @Test
    void generateUsername_WhenBaseAndSuffix1Taken_ShouldAddSuffix2() {
        String username = userUtils.generateUsername("John", "Smith",
                u -> u.equals("john.smith") || u.equals("john.smith1"));
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
