package org.zayed.unirollweb.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.zayed.unirollweb.common.JwtConfig;
import org.zayed.unirollweb.common.SecurityConfig;
import org.zayed.unirollweb.user.Role;
import org.zayed.unirollweb.user.UserResponse;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerIsPublicAndReturns201() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(
                new UserResponse(1L, "Ali", "ali@uni.edu", Role.STUDENT, Instant.parse("2026-10-01T00:00:00Z")));

        // No token on this request
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("password123", "STUDENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ali@uni.edu"));
    }

    @Test
    void registerRejectsUnknownRole() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("password123", "ADMIN")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    void registerRejectsPasswordLongerThanBcryptLimit() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("x".repeat(73), "STUDENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
        verifyNoInteractions(authService);
    }

    @Test
    void takenEmailBecomes409() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenThrow(new EmailAlreadyUsedException("ali@uni.edu"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("password123", "STUDENT")))
                .andExpect(status().isConflict());
    }

    @Test
    void badCredentialsBecome401WithGenericMessage() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"ali@uni.edu\", \"password\": \"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void malformedJsonBecomes400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(authService);
    }

    private static String registerJson(String password, String role) {
        return """
                {"name": "Ali", "email": "ali@uni.edu", "password": "%s", "role": "%s"}
                """.formatted(password, role);
    }
}
