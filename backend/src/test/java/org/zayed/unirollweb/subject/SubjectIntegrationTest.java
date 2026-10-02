package org.zayed.unirollweb.subject;

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
import org.zayed.unirollweb.auth.JwtService;
import org.zayed.unirollweb.user.Role;
import org.zayed.unirollweb.user.User;
import org.zayed.unirollweb.user.UserRepository;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Subject endpoints over HTTP against a real database. Data is not rolled back between tests,
 * so each test uses its own users and subject codes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SubjectIntegrationTest {

    private static final AtomicInteger codeCounter = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void lecturerCreatesSubject() throws Exception {
        String lecturer = tokenFor(Role.LECTURER, "Dr Tan");
        String code = uniqueCode();

        createSubject(lecturer, code.toLowerCase(), "Programming", 3, 30)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/subjects/")))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.enrolledCount").value(0))
                .andExpect(jsonPath("$.lecturer.name").value("Dr Tan"));
    }

    @Test
    void rejectsDuplicateCode() throws Exception {
        String lecturer = tokenFor(Role.LECTURER, "Dr Tan");
        String code = uniqueCode();
        createSubject(lecturer, code, "Programming", 3, 30).andExpect(status().isCreated());

        createSubject(lecturer, code, "Programming Again", 3, 30)
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidSubjectWithFieldErrors() throws Exception {
        String lecturer = tokenFor(Role.LECTURER, "Dr Tan");

        createSubject(lecturer, "CS 101!", "", 7, 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.creditHours").exists())
                .andExpect(jsonPath("$.errors.capacity").exists());
    }

    @Test
    void getsSubjectByIdOr404() throws Exception {
        String lecturer = tokenFor(Role.LECTURER, "Dr Tan");
        String student = tokenFor(Role.STUDENT, "Aisyah");
        long id = idOf(createSubject(lecturer, uniqueCode(), "Programming", 3, 30));

        mockMvc.perform(get("/api/subjects/" + id).header("Authorization", student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Programming"));
        mockMvc.perform(get("/api/subjects/999999").header("Authorization", student))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchesByCodeOrNameWithPaging() throws Exception {
        String lecturer = tokenFor(Role.LECTURER, "Dr Tan");
        String student = tokenFor(Role.STUDENT, "Aisyah");
        // A word no other test uses, so other tests' subjects don't show up in the results
        String word = "Topic" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 3; i++) {
            createSubject(lecturer, uniqueCode(), word + " " + i, 3, 30).andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/subjects")
                        .param("search", word.toLowerCase())
                        .param("size", "2")
                        .header("Authorization", student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void mineReturnsOnlyCallersSubjects() throws Exception {
        String owner = tokenFor(Role.LECTURER, "Dr Tan");
        String otherLecturer = tokenFor(Role.LECTURER, "Dr Lim");
        String word = "Mine" + UUID.randomUUID().toString().substring(0, 8);
        createSubject(owner, uniqueCode(), word + " A", 3, 30).andExpect(status().isCreated());
        createSubject(owner, uniqueCode(), word + " B", 3, 30).andExpect(status().isCreated());
        // Matches the search by name but belongs to someone else: must not appear with mine=true
        createSubject(otherLecturer, uniqueCode(), word + " C", 3, 30).andExpect(status().isCreated());

        mockMvc.perform(get("/api/subjects").param("search", word).param("mine", "true")
                        .header("Authorization", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].lecturer.name", everyItem(is("Dr Tan"))));
        mockMvc.perform(get("/api/subjects").param("search", word).header("Authorization", owner))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void rejectsPageSizeAboveLimit() throws Exception {
        String student = tokenFor(Role.STUDENT, "Aisyah");

        mockMvc.perform(get("/api/subjects").param("size", "51").header("Authorization", student))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerUpdatesSubject() throws Exception {
        String lecturer = tokenFor(Role.LECTURER, "Dr Tan");
        String code = uniqueCode();
        long id = idOf(createSubject(lecturer, code, "Programming", 3, 30));

        mockMvc.perform(put("/api/subjects/" + id)
                        .header("Authorization", lecturer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectJson(code, "Advanced Programming", 4, 40)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Advanced Programming"))
                .andExpect(jsonPath("$.creditHours").value(4))
                .andExpect(jsonPath("$.capacity").value(40));
    }

    @Test
    void otherLecturerCannotUpdateOrDelete() throws Exception {
        String owner = tokenFor(Role.LECTURER, "Dr Tan");
        String otherLecturer = tokenFor(Role.LECTURER, "Dr Lim");
        String code = uniqueCode();
        long id = idOf(createSubject(owner, code, "Programming", 3, 30));

        mockMvc.perform(put("/api/subjects/" + id)
                        .header("Authorization", otherLecturer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectJson(code, "Hijacked", 3, 30)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/subjects/" + id).header("Authorization", otherLecturer))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerDeletesSubject() throws Exception {
        String lecturer = tokenFor(Role.LECTURER, "Dr Tan");
        long id = idOf(createSubject(lecturer, uniqueCode(), "Programming", 3, 30));

        mockMvc.perform(delete("/api/subjects/" + id).header("Authorization", lecturer))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/subjects/" + id).header("Authorization", lecturer))
                .andExpect(status().isNotFound());
    }

    // Creates a user straight in the database and returns a ready-to-use "Bearer <token>" header value
    private String tokenFor(Role role, String name) {
        User user = userRepository.save(new User(name, "user-" + UUID.randomUUID() + "@uni.edu", "unused-hash", role));
        return "Bearer " + jwtService.createToken(user).accessToken();
    }

    private ResultActions createSubject(String token, String code, String name, int creditHours, int capacity)
            throws Exception {
        return mockMvc.perform(post("/api/subjects")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(subjectJson(code, name, creditHours, capacity)));
    }

    private static String subjectJson(String code, String name, int creditHours, int capacity) {
        return """
                {"code": "%s", "name": "%s", "creditHours": %d, "capacity": %d}
                """.formatted(code, name, creditHours, capacity);
    }

    private static long idOf(ResultActions created) throws Exception {
        String body = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String uniqueCode() {
        return "SUB%03d".formatted(codeCounter.incrementAndGet());
    }
}
