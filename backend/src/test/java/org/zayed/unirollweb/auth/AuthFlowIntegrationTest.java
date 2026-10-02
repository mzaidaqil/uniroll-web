package org.zayed.unirollweb.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.zayed.unirollweb.TestcontainersConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the whole app (security filters, controllers, services, Flyway, Postgres) and calls it over HTTP.
 * Data is not rolled back between tests, so every test registers its own unique email.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registersLogsInAndFetchesCurrentUser() throws Exception {
        String email = uniqueEmail();

        register("Aisyah", email.toUpperCase(), PASSWORD, "STUDENT")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String token = login(email, PASSWORD);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aisyah"))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void rejectsEmailThatIsAlreadyRegistered() throws Exception {
        String email = uniqueEmail();
        register("Aisyah", email, PASSWORD, "STUDENT").andExpect(status().isCreated());

        register("Someone Else", email.toUpperCase(), PASSWORD, "LECTURER")
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidRegistrationWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "email": "not-an-email", "password": "short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.role").exists());
    }

    @Test
    void loginFailsTheSameWayForWrongPasswordAndUnknownEmail() throws Exception {
        String email = uniqueEmail();
        register("Aisyah", email, PASSWORD, "STUDENT").andExpect(status().isCreated());

        attemptLogin(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
        attemptLogin(uniqueEmail(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWhoseRoleWasEdited() throws Exception {
        String email = uniqueEmail();
        register("Aisyah", email, PASSWORD, "STUDENT").andExpect(status().isCreated());
        String token = login(email, PASSWORD);

        // A JWT is header.payload.signature; change STUDENT to LECTURER in the payload but keep the old signature
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("STUDENT", "LECTURER");
        String forged = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotUseLecturerOnlyEndpoint() throws Exception {
        String email = uniqueEmail();
        register("Aisyah", email, PASSWORD, "STUDENT").andExpect(status().isCreated());
        String token = login(email, PASSWORD);

        mockMvc.perform(post("/api/subjects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private ResultActions register(String name, String email, String password, String role) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "email": "%s", "password": "%s", "role": "%s"}
                        """.formatted(name, email, password, role)));
    }

    private ResultActions attemptLogin(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    private String login(String email, String password) throws Exception {
        String body = attemptLogin(email, password)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@uni.edu";
    }
}
