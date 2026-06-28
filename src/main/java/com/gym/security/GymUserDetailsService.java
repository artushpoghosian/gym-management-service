package com.gym.security;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GymUserDetailsService implements UserDetailsService {

    private final TraineeDao traineeDao;
    private final TrainerDao trainerDao;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = traineeDao.findById(username)
                .<User>map(t -> t)
                .or(() -> trainerDao.findById(username).map(t -> t))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String role = traineeDao.findById(username).isPresent() ? "ROLE_TRAINEE" : "ROLE_TRAINER";

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isActive(),
                true, true, true,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
