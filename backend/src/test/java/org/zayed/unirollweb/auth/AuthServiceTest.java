package org.zayed.unirollweb.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.zayed.unirollweb.user.Role;
import org.zayed.unirollweb.user.User;
import org.zayed.unirollweb.user.UserRepository;
import org.zayed.unirollweb.user.UserResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit test: no Spring, no database. The repository, encoder and JWT service are Mockito mocks,
 * so each test controls exactly what they return and checks how AuthService used them.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // The constructor hashes a dummy password once (used to equalise login timing)
        when(passwordEncoder.encode("dummy-password-for-timing")).thenReturn("dummy-hash");
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerNormalisesEmailAndStoresOnlyTheHash() {
        when(userRepository.existsByEmail("ali@uni.edu")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(
                new RegisterRequest("  Ali  ", " Ali@Uni.EDU ", "password123", Role.STUDENT));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Ali");
        assertThat(saved.getValue().getEmail()).isEqualTo("ali@uni.edu");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(response.email()).isEqualTo("ali@uni.edu");
    }

    @Test
    void registerRejectsTakenEmailWithoutSaving() {
        when(userRepository.existsByEmail("ali@uni.edu")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ali", "ALI@uni.edu", "password123", Role.STUDENT)))
                .isInstanceOf(EmailAlreadyUsedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokenForCorrectPassword() {
        User user = new User("Ali", "ali@uni.edu", "bcrypt-hash", Role.STUDENT);
        TokenResponse token = new TokenResponse("jwt", "Bearer", 3600);
        when(userRepository.findByEmail("ali@uni.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "bcrypt-hash")).thenReturn(true);
        when(jwtService.createToken(user)).thenReturn(token);

        assertThat(authService.login(new LoginRequest("Ali@Uni.edu", "password123"))).isEqualTo(token);
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User("Ali", "ali@uni.edu", "bcrypt-hash", Role.STUDENT);
        when(userRepository.findByEmail("ali@uni.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ali@uni.edu", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void loginWithUnknownEmailStillRunsBcryptCheck() {
        when(userRepository.findByEmail("nobody@uni.edu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@uni.edu", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
        // Proves the timing protection: a hash comparison happens even though the user doesn't exist
        verify(passwordEncoder).matches("password123", "dummy-hash");
    }
}
