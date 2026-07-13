package com.gym.workload.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class WorkloadUserDetailsServiceTest {

    private final WorkloadUserDetailsService service = new WorkloadUserDetailsService();

    @Test
    void loadUserByUsername_ReturnsUserWithGivenNameAndServiceRole() {
        UserDetails details = service.loadUserByUsername("gym-management-service");

        assertThat(details.getUsername()).isEqualTo("gym-management-service");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SERVICE");
    }

    @Test
    void loadUserByUsername_ReturnsEnabledUser() {
        UserDetails details = service.loadUserByUsername("any.user");

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getUsername()).isEqualTo("any.user");
    }
}
