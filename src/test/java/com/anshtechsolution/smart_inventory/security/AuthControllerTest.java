package com.anshtechsolution.smart_inventory.security;

import com.anshtechsolution.smart_inventory.security.dto.LoginResponse;
import com.anshtechsolution.smart_inventory.security.dto.SignupRequest;
import com.anshtechsolution.smart_inventory.security.dto.SignupResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    private static final String BASE_URL = "/api/v1/auth";

    // 1. Successful login
    @Test
    void loginShouldReturn200WithJwtResponse() throws Exception {

        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);

        when(jwtService.generateToken(authentication))
                .thenReturn("test.jwt.token");

        when(jwtService.getAccessTokenExpirationSeconds())
                .thenReturn(1800L);

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "ValidPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("test.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(1800));

        verify(authenticationManager).authenticate(any());
        verify(jwtService).generateToken(authentication);
        verify(jwtService).getAccessTokenExpirationSeconds();
    }

    // 2. Login with blank username
    @Test
    void loginShouldReturn400WhenUsernameIsBlank() throws Exception {

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": "ValidPassword123!"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authenticationManager, jwtService);
    }

    // 3. Login with blank password
    @Test
    void loginShouldReturn400WhenPasswordIsBlank() throws Exception {

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authenticationManager, jwtService);
    }

    // 4. Signup successfully
    @Test
    void signupShouldReturn201() throws Exception {

        SignupResponse response = new SignupResponse(
                1L,
                "ansh",
                "VIEWER",
                "User registered successfully"
        );

        when(authService.signup(any(SignupRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ansh",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("ansh"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.message")
                        .value("User registered successfully"));

        verify(authService).signup(any(SignupRequest.class));
    }

    // 5. Signup with blank username
    @Test
    void signupShouldReturn400WhenUsernameIsBlank() throws Exception {

        mockMvc.perform(post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // 6. Signup with username shorter than 3 characters
    @Test
    void signupShouldReturn400WhenUsernameIsTooShort() throws Exception {

        mockMvc.perform(post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ab",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // 7. Signup with username longer than 50 characters
    @Test
    void signupShouldReturn400WhenUsernameIsTooLong() throws Exception {

        String username = "u".repeat(51);

        mockMvc.perform(post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "StrongPassword123!"
                                }
                                """.formatted(username)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // 8. Signup with blank password
    @Test
    void signupShouldReturn400WhenPasswordIsBlank() throws Exception {

        mockMvc.perform(post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ansh",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // 9. Signup with password shorter than 12 characters
    @Test
    void signupShouldReturn400WhenPasswordIsTooShort() throws Exception {

        mockMvc.perform(post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ansh",
                                  "password": "Short123!"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // 10. Signup with password longer than 72 characters
    @Test
    void signupShouldReturn400WhenPasswordIsTooLong() throws Exception {

        String password = "P".repeat(73);

        mockMvc.perform(post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ansh",
                                  "password": "%s"
                                }
                                """.formatted(password)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // 11. Signup with duplicate username
    @Test
    void signupShouldReturn409WhenUsernameAlreadyExists() throws Exception {

        when(authService.signup(any(SignupRequest.class)))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Username is already taken"
                ));

        mockMvc.perform(post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "existinguser",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isConflict());

        verify(authService).signup(any(SignupRequest.class));
    }
}