package com.parkingSystem.config;

import com.parkingSystem.model.User;
import com.parkingSystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSeederTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void createsAdminWithHashedPasswordWhenAllVariablesAreSet() {
        when(userRepository.existsByUserName("admin")).thenReturn(false);
        when(userRepository.existsByUserEmail("admin@example.com")).thenReturn(false);

        new AdminSeeder(userRepository, passwordEncoder, "admin", "admin@example.com", "s3cret-pass").run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User admin = captor.getValue();
        assertThat(admin.getRole()).isEqualTo("ADMIN");
        assertThat(admin.getPassword()).isNotEqualTo("s3cret-pass");
        assertThat(passwordEncoder.matches("s3cret-pass", admin.getPassword())).isTrue();
    }

    @Test
    void skipsSilentlyWhenAVariableIsMissing() {
        new AdminSeeder(userRepository, passwordEncoder, "admin", "", "s3cret-pass").run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void doesNotOverwriteAnExistingUser() {
        when(userRepository.existsByUserName("admin")).thenReturn(true);

        new AdminSeeder(userRepository, passwordEncoder, "admin", "admin@example.com", "s3cret-pass").run(null);

        verify(userRepository, never()).save(any());
    }
}
