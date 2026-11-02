package com.anshtechsolution.smart_inventory.security;

import com.anshtechsolution.smart_inventory.security.dto.SignupRequest;
import com.anshtechsolution.smart_inventory.security.dto.SignupResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest(
                "ansh",
                "StrongPassword123!"
        );
    }

    // 1. Successful signup
    @Test
    void signupShouldRegisterUserSuccessfully() {

        when(appUserRepository.existsByUsername("ansh"))
                .thenReturn(false);

        when(passwordEncoder.encode("StrongPassword123!"))
                .thenReturn("encoded-password");

        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> {
                    AppUser user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        SignupResponse response = authService.signup(signupRequest);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("ansh", response.username());
        assertEquals("VIEWER", response.role());
        assertEquals("User registered successfully", response.message());

        verify(appUserRepository).existsByUsername("ansh");
        verify(passwordEncoder).encode("StrongPassword123!");
        verify(appUserRepository).save(any(AppUser.class));
    }

    // 2. Username should be trimmed
    @Test
    void signupShouldTrimUsername() {

        SignupRequest request = new SignupRequest(
                "  ansh  ",
                "StrongPassword123!"
        );

        when(appUserRepository.existsByUsername("ansh"))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-password");

        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SignupResponse response = authService.signup(request);

        assertEquals("ansh", response.username());

        verify(appUserRepository).existsByUsername("ansh");
    }

    // 3. New user should receive VIEWER role
    @Test
    void signupShouldAssignViewerRoleByDefault() {

        when(appUserRepository.existsByUsername("ansh"))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-password");

        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SignupResponse response = authService.signup(signupRequest);

        assertEquals(Role.VIEWER.name(), response.role());
    }

    // 4. Password should be encoded before saving
    @Test
    void signupShouldEncodePasswordBeforeSaving() {

        when(appUserRepository.existsByUsername("ansh"))
                .thenReturn(false);

        when(passwordEncoder.encode("StrongPassword123!"))
                .thenReturn("bcrypt-hash");

        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> {
                    AppUser user = invocation.getArgument(0);

                    assertEquals("bcrypt-hash", user.getPasswordHash());
                    return user;
                });

        authService.signup(signupRequest);

        verify(passwordEncoder).encode("StrongPassword123!");
    }

    // 5. New user should be enabled
    @Test
    void signupShouldEnableNewUser() {

        when(appUserRepository.existsByUsername("ansh"))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-password");

        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SignupResponse response = authService.signup(signupRequest);

        verify(appUserRepository).save(argThat(user -> user.isEnabled()));
    }

    // 6. Duplicate username should be rejected
    @Test
    void signupShouldThrowConflictWhenUsernameAlreadyExists() {

        when(appUserRepository.existsByUsername("ansh"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.signup(signupRequest)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(appUserRepository).existsByUsername("ansh");
        verify(appUserRepository, never()).save(any(AppUser.class));
        verifyNoInteractions(passwordEncoder);
    }

    // 7. Password should be stored as a hash, not plain text
    @Test
    void signupShouldNotStorePlainTextPassword() {

        String plainPassword = "StrongPassword123!";
        String encodedPassword = "encoded-bcrypt-password";

        when(appUserRepository.existsByUsername("ansh"))
                .thenReturn(false);

        when(passwordEncoder.encode(plainPassword))
                .thenReturn(encodedPassword);

        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.signup(signupRequest);

        verify(appUserRepository).save(argThat(user ->
                !plainPassword.equals(user.getPasswordHash())
                        && encodedPassword.equals(user.getPasswordHash())
        ));
    }

    // 8. Verify password encoder uses BCrypt
    @Test
    void bcryptPasswordEncoderShouldVerifyEncodedPassword() {

        PasswordEncoder bcrypt = new BCryptPasswordEncoder();

        String rawPassword = "StrongPassword123!";
        String encodedPassword = bcrypt.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(bcrypt.matches(rawPassword, encodedPassword));
        assertFalse(bcrypt.matches("WrongPassword123!", encodedPassword));
    }
}